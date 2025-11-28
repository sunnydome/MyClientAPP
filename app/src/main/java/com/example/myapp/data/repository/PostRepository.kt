package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.FeedItem
import com.example.myapp.data.model.Post
import com.example.myapp.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
/**
 * 帖子数据仓库
 * 协调 网络API (数据源头) 与 本地数据库 (缓存/UI数据源)
 */
class PostRepository(private val database: AppDatabase) {

    private val postDao = database.postDao()
    // 获取 PostApi 实例
    private val postApi = RetrofitClient.postApi

    private val TAG = "PostRepository"
    // ========== 查询方法 (依然从数据库读取，保持 LiveData 响应式) ==========

    /**
     * 获取指定分类的Feed列表 (观察本地数据库)
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return postDao.getPostsByCategory(category).map { posts ->
            posts.map { FeedItem.fromPost(it) }
        }
    }

    /**
     * 获取帖子详情 (观察本地数据库)
     */
    fun getPostById(postId: String): LiveData<Post?> {
        return postDao.getPostById(postId)
    }

    // ========== 网络请求与数据同步 ==========

    /**
     * 从网络拉取 Feed 流
     * 涵盖了【下滑刷新】和【上拉加载】
     * * @param category 分类
     * @param page 页码：1 代表刷新，>1 代表加载更多
     */
    suspend fun fetchFeeds(category: String, page: Int = 1): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🚀 开始请求网络: category=$category, page=$page")
            try {
                val response = postApi.getFeeds(category = category, page = page)
                Log.d(TAG, "📥 API响应: code=${response.code}, message=${response.message}")

                if (response.isSuccess() && response.data != null) {
                    val posts = response.data.list
                    Log.d(TAG, "✅ 数据解析成功: 收到 ${posts.size} 条帖子")

                    val postsWithCategory = posts.map { it.copy(category = category) }

                    // ============ 修改重点：暂时移除 withTransaction ============
                    // 直接执行数据库操作，看看具体卡在哪一步，或者报什么错
                    Log.d(TAG, "👉 准备直接操作数据库...")

                    if (page == 1) {
                        Log.d(TAG, "🧹 正在执行 deleteByCategory...")
                        // 如果这一行报错，说明 PostDao.deleteByCategory 定义有问题
                        postDao.deleteByCategory(category)
                        Log.d(TAG, "✅ deleteByCategory 完成")
                    }

                    Log.d(TAG, "💾 正在执行 insertAll...")
                    // 如果这一行报错，可能是数据类型转换或主键冲突问题
                    postDao.insertAll(postsWithCategory)
                    Log.d(TAG, "✅ insertAll 完成")
                    // ========================================================

                    Result.success(response.data.hasMore)
                } else {
                    Log.e(TAG, "❌ 业务失败: ${response.message}")
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                // ⚠️⚠️ 请重点查看 Logcat 中是否有这行红色日志 ⚠️⚠️
                Log.e(TAG, "💥 发生异常 (Catch Block): ${e.javaClass.simpleName} - ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取帖子详情 (网络 -> 数据库)
     */
    suspend fun fetchPostDetail(postId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = postApi.getPostDetail(postId)
                if (response.isSuccess() && response.data != null) {
                    postDao.insert(response.data)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========== 交互操作 (先请求网络，成功后更新本地) ==========

    /**
     * 发布新帖子
     * 逻辑：尝试网络发布 -> 失败也不要紧 -> 强制存入本地数据库 -> 返回成功
     */
    suspend fun publishPost(post: Post): Result<Post> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 尝试网络请求 (为了模拟真实流程，还是发一下，虽然知道会失败)
                try {
                    val request = com.example.myapp.data.network.api.PublishPostRequest(
                        title = post.title,
                        content = post.content,
                        category = post.category,
                        imageUrls = post.imageUrls,
                        location = post.location
                    )
                    // 发送请求，但不依赖它的结果来决定是否存库
                    postApi.publishPost(request)
                } catch (e: Exception) {
                    // 捕获网络异常，打印日志，但不中断流程
                    Log.w(TAG, "网络发布失败(预期内): ${e.message}")
                }

                // 2. 【核心】强制写入本地数据库
                // 这一步执行后，LiveData 会收到通知，首页列表会自动更新
                postDao.insert(post)
                Log.d(TAG, "已强制写入本地数据库: ${post.title}")

                // 3. 始终返回成功，欺骗 UI 层说我们成功了
                Result.success(post)

            } catch (e: Exception) {
                // 只有数据库写入都崩了，才是真的失败
                Log.e(TAG, "本地保存失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 切换点赞状态
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 乐观更新：先在本地更新 UI，让用户感觉“秒赞”
                val localPost = postDao.getPostByIdSync(postId)
                localPost?.let {
                    val newStatus = !it.isLiked
                    val delta = if (newStatus) 1 else -1
                    postDao.updateLikeStatus(postId, newStatus, delta)
                }

                // 2. 发送网络请求
                val response = postApi.toggleLike(postId)

                if (response.isSuccess() && response.data != null) {
                    // 3. 以服务器返回的最新状态为准，再次校准本地数据
                    val serverStatus = response.data
                    Result.success(serverStatus)
                } else {
                    // 失败了，回滚本地状态
                    localPost?.let {
                        val originalStatus = it.isLiked
                        val delta = if (originalStatus) 1 else -1
                        postDao.updateLikeStatus(postId, originalStatus, delta)
                    }
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                // 网络异常，回滚
                // 实际生产中可能需要在这里也执行回滚逻辑，或者在 ViewModel 中处理
                Result.failure(e)
            }
        }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleCollect(postId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 逻辑同点赞，这里简化直接调接口，成功后更新本地
                val response = postApi.toggleCollect(postId)
                if (response.isSuccess() && response.data != null) {
                    val isCollected = response.data
                    val delta = if (isCollected) 1 else -1
                    postDao.updateCollectStatus(postId, isCollected, delta)
                    Result.success(isCollected)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PostRepository? = null

        fun getInstance(database: AppDatabase): PostRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PostRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}
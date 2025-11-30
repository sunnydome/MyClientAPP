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
import androidx.room.withTransaction
import com.example.myapp.data.network.RetrofitClient.userApi

/**
 * 帖子数据仓库
 * 协调 网络API (数据源头) 与 本地数据库 (缓存/UI数据源)
 */
class PostRepository(private val database: AppDatabase) {

    private val postDao = database.postDao()
    // 获取 PostApi 实例
    private val postApi = RetrofitClient.postApi
    private val userDao = database.userDao()
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

                    // 使用事务包裹：删除和插入作为一个整体执行
                    // 这样 LiveData 只会收到最后的结果，不会收到中间“被清空”的状态，彻底解决闪屏问题
                    database.withTransaction {
                        if (page == 1) {
                            Log.d(TAG, "🧹 事务中: 执行 deleteByCategory...")
                            postDao.deleteByCategory(category)
                        }
                        Log.d(TAG, "💾 事务中: 执行 insertAll...")
                        postDao.insertAll(postsWithCategory)
                    }
                    Log.d(TAG, "✅ 数据库事务完成")

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
     * 修改为：仅发送网络请求，不更新本地数据库。
     * UI 的变化交由 ViewModel 在内存中处理。
     */
    suspend fun toggleLike(postId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 直接发送网络请求
                // 由于没有后端，这里肯定会报错，我们捕获它
                postApi.toggleLike(postId)
                Result.success(Unit)
            } catch (e: Exception) {
                // 2. 忽略网络错误，视为“操作已发出”
                Log.w(TAG, "网络请求失败(无后端忽略): ${e.message}")
                Result.success(Unit)
            }
        }
    }

    /**
     * 切换收藏状态
     * 修改为：仅发送网络请求，不更新本地数据库。
     */
    suspend fun toggleCollect(postId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                postApi.toggleCollect(postId)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "网络请求失败(无后端忽略): ${e.message}")
                Result.success(Unit)
            }
        }
    }
    /**
     * 切换关注状态 (逻辑改进版)
     * 1. 查询当前状态
     * 2. 立即更新本地数据库 (UI秒变)
     * 3. 尝试网络请求 (失败则忽略，假装成功)
     */
    suspend fun toggleFollow(postId: String, authorId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 获取当前本地状态 (如果为空默认 false)
                val currentStatus = postDao.getFollowStatus(postId) ?: false
                val newStatus = !currentStatus

                Log.d(TAG, "执行关注操作: authorId=$authorId, 新状态=$newStatus")

                // 2. 【核心】立即更新本地数据库
                // 注意：关注是针对作者的，所以要更新该作者的所有帖子
                postDao.updateFollowStatusByAuthor(authorId, newStatus)
                // 同时也要更新用户表（如果有的话）
                userDao.updateFollowStatus(authorId, newStatus)

                // 3. 尝试网络请求 (模拟)
                try {
                    // 即使没有后端，这里也可以发请求，超时会进入 catch
                    // 真实的 API 通常是 userApi.toggleFollow(authorId)
                    userApi.toggleFollow(authorId)
                } catch (e: Exception) {
                    // 4. 【关键】忽略网络错误
                    // 因为没有后端，这里一定会报错。我们捕获它，不抛出，
                    // 从而让上层认为操作"成功"了，保持 UI 的关注状态。
                    Log.w(TAG, "网络请求失败(预期内，无后端): ${e.message}，保持本地成功状态")
                }

                // 5. 返回成功的新状态
                Result.success(newStatus)

            } catch (e: Exception) {
                // 只有数据库读写崩了才返回失败
                Log.e(TAG, "本地数据库操作失败", e)
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
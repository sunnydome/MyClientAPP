package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.mock.MockDataProvider
import com.example.myapp.data.model.FeedItem
import com.example.myapp.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 帖子数据仓库
 * 负责帖子相关的数据操作，是ViewModel和数据层的桥梁
 */
class PostRepository(private val database: AppDatabase) {

    private val postDao = database.postDao()

    // ========== 查询方法 ==========

    /**
     * 获取指定分类的Feed列表
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return postDao.getPostsByCategory(category).map { posts ->
            posts.map { FeedItem.fromPost(it) }
        }
    }

    /**
     * 获取所有Feed（用于"发现"页面）
     */
    fun getAllFeeds(): LiveData<List<FeedItem>> {
        return postDao.getAllPosts().map { posts ->
            posts.map { FeedItem.fromPost(it) }
        }
    }

    /**
     * 获取帖子详情
     */
    fun getPostById(postId: String): LiveData<Post?> {
        return postDao.getPostById(postId)
    }

    /**
     * 获取帖子详情（同步方法）
     */
    suspend fun getPostByIdSync(postId: String): Post? {
        return withContext(Dispatchers.IO) {
            postDao.getPostByIdSync(postId)
        }
    }

    /**
     * 获取用户的帖子
     */
    fun getPostsByAuthor(authorId: String): LiveData<List<Post>> {
        return postDao.getPostsByAuthor(authorId)
    }

    /**
     * 获取收藏的帖子
     */
    fun getCollectedPosts(): LiveData<List<Post>> {
        return postDao.getCollectedPosts()
    }

    /**
     * 获取点赞的帖子
     */
    fun getLikedPosts(): LiveData<List<Post>> {
        return postDao.getLikedPosts()
    }

    /**
     * 搜索帖子
     */
    fun searchPosts(keyword: String): LiveData<List<Post>> {
        return postDao.searchPosts(keyword)
    }

    // ========== 写入方法 ==========

    /**
     * 发布新帖子
     */
    suspend fun publishPost(post: Post): Result<Post> {
        return withContext(Dispatchers.IO) {
            try {
                postDao.insert(post)
                Result.success(post)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 更新帖子
     */
    suspend fun updatePost(post: Post): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                postDao.update(post)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除帖子
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                postDao.deleteById(postId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ========== 交互方法 ==========

    /**
     * 切换点赞状态
     */
    suspend fun toggleLike(postId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val post = postDao.getPostByIdSync(postId) ?: return@withContext Result.failure(Exception("帖子不存在"))
                val newLikeStatus = !post.isLiked
                val delta = if (newLikeStatus) 1 else -1
                postDao.updateLikeStatus(postId, newLikeStatus, delta)
                Result.success(newLikeStatus)
            } catch (e: Exception) {
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
                val post = postDao.getPostByIdSync(postId) ?: return@withContext Result.failure(Exception("帖子不存在"))
                val newCollectStatus = !post.isCollected
                val delta = if (newCollectStatus) 1 else -1
                postDao.updateCollectStatus(postId, newCollectStatus, delta)
                Result.success(newCollectStatus)
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
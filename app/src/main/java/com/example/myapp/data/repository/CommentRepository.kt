package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.Comment
import com.example.myapp.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepository(private val database: AppDatabase) {

    private val commentDao = database.commentDao()
    private val postDao = database.postDao()
    private val commentApi = RetrofitClient.commentApi // 获取 API

    /**
     * 获取帖子的一级评论 (观察本地)
     */
    fun getTopLevelComments(postId: String): LiveData<List<Comment>> {
        return commentDao.getTopLevelComments(postId)
    }

    /**
     * 刷新评论列表 (网络 -> 数据库)
     */
    suspend fun refreshComments(postId: String, page: Int = 1): Result<List<Comment>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = commentApi.getComments(postId, page)
                if (response.isSuccess() && response.data != null) {
                    val comments = response.data
                    // 插入数据库
                    commentDao.insertAll(comments)

                    // 返回具体的 comments 列表，而不是 Unit
                    Result.success(comments)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 发表评论
     */
    suspend fun addComment(
        postId: String,
        content: String,
        parentId: String? = null,
        replyToName: String? = null
    ): Result<Comment> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.myapp.data.network.api.AddCommentRequest(
                    content = content,
                    parentId = parentId
                    // replyToUserId 根据实际后端需求传递
                )

                val response = commentApi.addComment(postId, request)

                if (response.isSuccess() && response.data != null) {
                    val newComment = response.data

                    // 1. 插入新评论到本地
                    commentDao.insert(newComment)

                    // 2. 更新帖子的评论计数 (本地 +1)
                    postDao.updateCommentCount(postId, 1)

                    // 3. 如果是回复，更新父评论回复数
                    parentId?.let {
                        commentDao.updateReplyCount(it, 1)
                    }

                    Result.success(newComment)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 切换评论点赞状态
     */
    suspend fun toggleLike(commentId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = commentApi.toggleCommentLike(commentId)
                if (response.isSuccess() && response.data != null) {
                    val isLiked = response.data
                    val delta = if (isLiked) 1 else -1
                    commentDao.updateLikeStatus(commentId, isLiked, delta)
                    Result.success(isLiked)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除评论 (网络 -> 数据库)
     */
    suspend fun deleteComment(commentId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 先查询本地评论，为了后续更新计数
                val comment = commentDao.getCommentByIdSync(commentId)
                    ?: return@withContext Result.failure(Exception("本地评论不存在"))

                // 2. 调用网络删除接口
                val response = commentApi.deleteComment(commentId)

                if (response.isSuccess()) {
                    // 3. 网络删除成功后，清理本地数据
                    // 逻辑与旧版本一致：如果是以及评论，要连带删除回复；更新帖子评论数
                    if (comment.isTopLevel()) {
                        val replyCount = commentDao.getReplyCount(commentId)
                        commentDao.deleteWithReplies(commentId)
                        // 减少计数：1条主评论 + N条回复
                        postDao.updateCommentCount(comment.postId, -(1 + replyCount))
                    } else {
                        commentDao.deleteById(commentId)
                        // 减少计数：1条回复
                        postDao.updateCommentCount(comment.postId, -1)
                        // 更新父评论的回复数
                        comment.parentId?.let {
                            commentDao.updateReplyCount(it, -1)
                        }
                    }
                    Result.success(Unit)
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
        private var INSTANCE: CommentRepository? = null

        fun getInstance(database: AppDatabase): CommentRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CommentRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}
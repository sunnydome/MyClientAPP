package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.mock.MockDataProvider
import com.example.myapp.data.model.Comment
import com.example.myapp.data.model.CommentWithReplies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 评论数据仓库
 * 负责评论相关的数据操作
 */
class CommentRepository(private val database: AppDatabase) {

    private val commentDao = database.commentDao()
    private val postDao = database.postDao()

    /**
     * 获取帖子的一级评论
     */
    fun getTopLevelComments(postId: String): LiveData<List<Comment>> {
        return commentDao.getTopLevelComments(postId)
    }

    /**
     * 获取评论的回复
     */
    fun getReplies(commentId: String): LiveData<List<Comment>> {
        return commentDao.getReplies(commentId)
    }

    /**
     * 获取评论及其回复（组合数据）
     */
    fun getCommentsWithReplies(postId: String): LiveData<List<CommentWithReplies>> {
        val result = MediatorLiveData<List<CommentWithReplies>>()

        val topLevelComments = commentDao.getTopLevelComments(postId)

        result.addSource(topLevelComments) { comments ->
            // 这里简化处理，实际应该异步加载回复
            // 可以在ViewModel中使用协程处理
            result.value = comments.map { comment ->
                CommentWithReplies(comment, emptyList())
            }
        }

        return result
    }

    /**
     * 获取帖子的所有评论
     */
    fun getAllComments(postId: String): LiveData<List<Comment>> {
        return commentDao.getAllComments(postId)
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
                // 获取当前用户信息
                val currentUserId = MockDataProvider.getCurrentUserId()
                val currentUser = database.userDao().getUserByIdSync(currentUserId)
                    ?: return@withContext Result.failure(Exception("用户不存在"))

                val comment = Comment(
                    id = MockDataProvider.generateCommentId(),
                    postId = postId,
                    authorId = currentUserId,
                    authorName = currentUser.userName,
                    authorAvatar = currentUser.avatarUrl,
                    content = content,
                    parentId = parentId,
                    replyToName = replyToName
                )

                commentDao.insert(comment)

                // 更新帖子评论数
                postDao.updateCommentCount(postId, 1)

                // 如果是回复，更新父评论的回复数
                parentId?.let {
                    commentDao.updateReplyCount(it, 1)
                }

                Result.success(comment)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除评论
     */
    suspend fun deleteComment(commentId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val comment = commentDao.getCommentByIdSync(commentId)
                    ?: return@withContext Result.failure(Exception("评论不存在"))

                // 如果是一级评论，同时删除所有回复
                if (comment.isTopLevel()) {
                    val replyCount = commentDao.getReplyCount(commentId)
                    commentDao.deleteWithReplies(commentId)
                    // 更新帖子评论数（一级评论 + 回复数）
                    postDao.updateCommentCount(comment.postId, -(1 + replyCount))
                } else {
                    commentDao.deleteById(commentId)
                    // 更新帖子评论数
                    postDao.updateCommentCount(comment.postId, -1)
                    // 更新父评论回复数
                    comment.parentId?.let {
                        commentDao.updateReplyCount(it, -1)
                    }
                }

                Result.success(Unit)
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
                val comment = commentDao.getCommentByIdSync(commentId)
                    ?: return@withContext Result.failure(Exception("评论不存在"))

                val newLikeStatus = !comment.isLiked
                val delta = if (newLikeStatus) 1 else -1
                commentDao.updateLikeStatus(commentId, newLikeStatus, delta)

                Result.success(newLikeStatus)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取评论数量
     */
    suspend fun getCommentCount(postId: String): Int {
        return withContext(Dispatchers.IO) {
            commentDao.getCommentCount(postId)
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
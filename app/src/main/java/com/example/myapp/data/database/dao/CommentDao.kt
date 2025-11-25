package com.example.myapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapp.data.model.Comment

/**
 * 评论数据访问对象
 */
@Dao
interface CommentDao {

    // ========== 查询 ==========

    @Query("SELECT * FROM comments WHERE id = :commentId")
    fun getCommentById(commentId: String): LiveData<Comment?>

    @Query("SELECT * FROM comments WHERE id = :commentId")
    suspend fun getCommentByIdSync(commentId: String): Comment?

    /**
     * 获取帖子的所有一级评论（按时间倒序）
     */
    @Query("""
        SELECT * FROM comments 
        WHERE postId = :postId AND parentId IS NULL 
        ORDER BY createTime DESC
    """)
    fun getTopLevelComments(postId: String): LiveData<List<Comment>>

    /**
     * 获取某条评论的所有回复
     */
    @Query("""
        SELECT * FROM comments 
        WHERE parentId = :parentId 
        ORDER BY createTime ASC
    """)
    fun getReplies(parentId: String): LiveData<List<Comment>>

    /**
     * 获取某条评论的所有回复（同步方法）
     */
    @Query("""
        SELECT * FROM comments 
        WHERE parentId = :parentId 
        ORDER BY createTime ASC
    """)
    suspend fun getRepliesSync(parentId: String): List<Comment>

    /**
     * 获取帖子的所有评论（包括回复）
     */
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createTime DESC")
    fun getAllComments(postId: String): LiveData<List<Comment>>

    /**
     * 获取用户的所有评论
     */
    @Query("SELECT * FROM comments WHERE authorId = :authorId ORDER BY createTime DESC")
    fun getCommentsByAuthor(authorId: String): LiveData<List<Comment>>

    // ========== 插入/更新 ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<Comment>)

    @Update
    suspend fun update(comment: Comment)

    @Delete
    suspend fun delete(comment: Comment)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)

    /**
     * 删除评论及其所有回复
     */
    @Query("DELETE FROM comments WHERE id = :commentId OR parentId = :commentId")
    suspend fun deleteWithReplies(commentId: String)

    // ========== 状态更新 ==========

    @Query("UPDATE comments SET isLiked = :isLiked, likeCount = likeCount + :delta WHERE id = :commentId")
    suspend fun updateLikeStatus(commentId: String, isLiked: Boolean, delta: Int)

    @Query("UPDATE comments SET replyCount = replyCount + :delta WHERE id = :commentId")
    suspend fun updateReplyCount(commentId: String, delta: Int)

    // ========== 统计 ==========

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    suspend fun getCommentCount(postId: String): Int

    @Query("SELECT COUNT(*) FROM comments WHERE parentId = :parentId")
    suspend fun getReplyCount(parentId: String): Int
}
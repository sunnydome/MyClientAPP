package com.example.myapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapp.data.model.Post

/**
 * 帖子数据访问对象
 */
@Dao
interface PostDao {

    // ========== 查询 ==========

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): LiveData<Post?>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostByIdSync(postId: String): Post?

    // 用于刷新列表时清除旧缓存
    @Query("DELETE FROM posts WHERE category = :category")
    suspend fun deleteByCategory(category: String)
    @Query("SELECT * FROM posts WHERE isDraft = 0 ORDER BY publishTime DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE category = :category AND isDraft = 0 ORDER BY publishTime DESC")
    fun getPostsByCategory(category: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE authorId = :authorId AND isDraft = 0 ORDER BY publishTime DESC")
    fun getPostsByAuthor(authorId: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE isCollected = 1 AND isDraft = 0 ORDER BY publishTime DESC")
    fun getCollectedPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE isLiked = 1 AND isDraft = 0 ORDER BY publishTime DESC")
    fun getLikedPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE isDraft = 1 ORDER BY updateTime DESC")
    fun getDrafts(): LiveData<List<Post>>

    @Query("""
        SELECT * FROM posts 
        WHERE isDraft = 0 
        AND (title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%')
        ORDER BY publishTime DESC
    """)
    fun searchPosts(keyword: String): LiveData<List<Post>>

    // [新增] 更新某个作者的所有帖子的关注状态
    @Query("UPDATE posts SET isFollowing = :isFollowing WHERE authorId = :authorId")
    suspend fun updateFollowStatusByAuthor(authorId: String, isFollowing: Boolean)

    // [新增] 单独获取某篇帖子的关注状态（用于Repository逻辑判断）
    @Query("SELECT isFollowing FROM posts WHERE id = :postId")
    suspend fun getFollowStatus(postId: String): Boolean?

    // ========== 插入/更新 ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    // ========== 状态更新 ==========

    @Query("UPDATE posts SET isLiked = :isLiked, likeCount = likeCount + :delta WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, isLiked: Boolean, delta: Int)

    @Query("UPDATE posts SET isCollected = :isCollected, collectCount = collectCount + :delta WHERE id = :postId")
    suspend fun updateCollectStatus(postId: String, isCollected: Boolean, delta: Int)

    @Query("UPDATE posts SET commentCount = commentCount + :delta WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, delta: Int)

    // ========== 统计 ==========

    @Query("SELECT COUNT(*) FROM posts WHERE category = :category AND isDraft = 0")
    suspend fun getPostCountByCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM posts WHERE authorId = :authorId AND isDraft = 0")
    suspend fun getPostCountByAuthor(authorId: String): Int
}
package com.example.myapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 评论数据模型
 * 支持一级评论和回复
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Post::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId"), Index("parentId")]
)
data class Comment(
    @PrimaryKey
    val id: String,

    // 所属帖子ID
    val postId: String,

    // 评论者ID
    val authorId: String,

    // 评论者名称（冗余字段）
    val authorName: String,

    // 评论者头像（冗余字段）
    val authorAvatar: String,

    // 评论内容
    val content: String,

    // 父评论ID（如果是回复的话）
    val parentId: String? = null,

    // 被回复者名称（如果是回复的话）
    val replyToName: String? = null,

    // 点赞数
    val likeCount: Int = 0,

    // 是否已点赞
    val isLiked: Boolean = false,

    // 子评论数量（仅一级评论有效）
    val replyCount: Int = 0,

    // 评论时间
    val createTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取格式化的时间
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - createTime

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 2592000_000 -> "${diff / 86400_000}天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(createTime))
            }
        }
    }

    /**
     * 是否是一级评论
     */
    fun isTopLevel(): Boolean = parentId == null
}

/**
 * 评论及其回复的组合模型
 * 用于显示评论列表
 */
data class CommentWithReplies(
    val comment: Comment,
    val replies: List<Comment> = emptyList()
)
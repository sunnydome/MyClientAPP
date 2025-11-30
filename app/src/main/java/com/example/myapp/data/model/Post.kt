package com.example.myapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapp.data.database.Converters

/**
 * 帖子数据模型
 * 统一的Feed/Post数据结构，用于首页列表和详情页
 */
@Entity(tableName = "posts")
@TypeConverters(Converters::class)
data class Post(
    @PrimaryKey
    val id: String,

    // 作者ID（关联User表）
    val authorId: String,

    // 作者名称（冗余字段，避免频繁联表查询）
    val authorName: String,

    // 作者头像URL（冗余字段）
    val authorAvatar: String,

    // 标题
    val title: String = "",

    // 正文内容
    val content: String = "",

    // 图片URL列表
    val imageUrls: List<String> = emptyList(),

    // 封面图URL（通常是第一张图）
    val coverUrl: String = "",

    // 封面图宽高比（用于瀑布流布局）
    val coverAspectRatio: Float = 1.0f,

    // 分类标签（关注、发现、同城）
    val category: String = "发现",

    // 话题标签
    val tags: List<String> = emptyList(),

    // 位置信息
    val location: String = "",

    // 点赞数
    val likeCount: Int = 0,

    // 评论数
    val commentCount: Int = 0,

    // 收藏数
    val collectCount: Int = 0,

    // 当前用户是否已点赞
    val isLiked: Boolean = false,

    // 当前用户是否已收藏
    val isCollected: Boolean = false,

    //是否关注作者
    val isFollowing: Boolean = false,

    // 是否为草稿
    val isDraft: Boolean = false,

    // 发布时间
    val publishTime: Long = System.currentTimeMillis(),

    // 创建时间
    val createTime: Long = System.currentTimeMillis(),

    // 更新时间
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取显示用的描述文本
     * 优先显示标题，其次显示内容
     */
    fun getDisplayText(): String {
        return when {
            title.isNotBlank() -> title
            content.isNotBlank() -> content.take(100)
            else -> ""
        }
    }

    /**
     * 获取格式化的发布时间
     */
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - publishTime

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 2592000_000 -> "${diff / 86400_000}天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(publishTime))
            }
        }
    }
}

/**
 * 用于首页Feed列表的简化模型
 * 从Post转换而来，减少不必要的字段
 */
data class FeedItem(
    val id: String,
    val coverUrl: String,
    val coverAspectRatio: Float,
    val title: String,
    val authorName: String,
    val authorAvatar: String,
    val likeCount: Int,
    val isLiked: Boolean
) {
    companion object {
        fun fromPost(post: Post): FeedItem {
            return FeedItem(
                id = post.id,
                coverUrl = post.coverUrl.ifBlank { post.imageUrls.firstOrNull() ?: "" },
                coverAspectRatio = post.coverAspectRatio,
                title = post.getDisplayText(),
                authorName = post.authorName,
                authorAvatar = post.authorAvatar,
                likeCount = post.likeCount,
                isLiked = post.isLiked
            )
        }
    }
}
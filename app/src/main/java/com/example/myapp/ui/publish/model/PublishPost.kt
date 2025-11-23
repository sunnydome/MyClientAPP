package com.example.myapp.ui.publish.model

import android.net.Uri

/**
 * 发布内容的数据模型
 * 用于在ViewModel和View之间传递数据
 */
data class PublishPost(
    // 选中的图片URI列表
    val imageUris: List<Uri> = emptyList(),

    // 标题
    val title: String = "",

    // 正文内容
    val content: String = "",

    // 是否为草稿
    val isDraft: Boolean = false,

    // 创建时间（用于草稿排序等）
    val createTime: Long = System.currentTimeMillis()
) {
    /**
     * 验证帖子是否可以发布
     * 至少需要一张图片或者标题/内容不为空
     */
    fun isValid(): Boolean {
        return imageUris.isNotEmpty() || title.isNotBlank() || content.isNotBlank()
    }

    /**
     * 检查是否为空帖子
     */
    fun isEmpty(): Boolean {
        return imageUris.isEmpty() && title.isBlank() && content.isBlank()
    }

    /**
     * 获取预览文本（用于显示草稿列表等）
     */
    fun getPreviewText(): String {
        return when {
            title.isNotBlank() -> title
            content.isNotBlank() -> content.take(50)
            else -> "未命名"
        }
    }
}
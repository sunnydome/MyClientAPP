package com.example.myapp.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapp.data.database.Converters

/**
 * 草稿数据模型
 * 用于保存未发布的帖子
 */
@Entity(tableName = "drafts")
@TypeConverters(Converters::class)
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 标题
    val title: String = "",

    // 正文内容
    val content: String = "",

    // 本地图片URI列表（字符串形式存储）
    val imageUris: List<String> = emptyList(),

    // 创建时间
    val createTime: Long = System.currentTimeMillis(),

    // 更新时间
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取预览文本
     */
    fun getPreviewText(): String {
        return when {
            title.isNotBlank() -> title
            content.isNotBlank() -> content.take(50)
            else -> "未命名草稿"
        }
    }

    /**
     * 是否为空草稿
     */
    fun isEmpty(): Boolean {
        return title.isBlank() && content.isBlank() && imageUris.isEmpty()
    }

    /**
     * 转换为Uri列表
     */
    fun getImageUriList(): List<Uri> {
        return imageUris.mapNotNull {
            try { Uri.parse(it) } catch (e: Exception) { null }
        }
    }
}
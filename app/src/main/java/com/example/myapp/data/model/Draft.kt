package com.example.myapp.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapp.data.database.Converters

@Entity(tableName = "drafts")
@TypeConverters(Converters::class)
data class Draft(
    // [修改] 移除 autoGenerate，并设置默认值为固定的 1L
    // 这样每次插入 ID 为 1 的数据时，都会覆盖旧的，实现“只有一个草稿”
    @PrimaryKey(autoGenerate = false)
    val id: Long = DRAFT_ID,

    val title: String = "",
    val content: String = "",
    val imageUris: List<String> = emptyList(),
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val DRAFT_ID = 1L // 固定草稿ID
    }

    // ... (保留原有的 getPreviewText, isEmpty, getImageUriList 方法) ...
    fun getImageUriList(): List<Uri> {
        return imageUris.mapNotNull {
            try { Uri.parse(it) } catch (e: Exception) { null }
        }
    }
}
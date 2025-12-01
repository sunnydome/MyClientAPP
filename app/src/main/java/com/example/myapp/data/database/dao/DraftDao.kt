package com.example.myapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapp.data.model.Draft

/**
 * 草稿数据访问对象
 */
@Dao
interface DraftDao {
    // 获取唯一的草稿
    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraft(id: Long = com.example.myapp.data.model.Draft.DRAFT_ID): Draft?

    // 插入或替换（因为ID固定，这实现了覆盖旧草稿）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: Draft)

    // 删除指定ID的草稿
    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: Long = com.example.myapp.data.model.Draft.DRAFT_ID)
}
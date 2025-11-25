package com.example.myapp.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapp.data.model.Draft

/**
 * 草稿数据访问对象
 */
@Dao
interface DraftDao {

    @Query("SELECT * FROM drafts ORDER BY updateTime DESC")
    fun getAllDrafts(): LiveData<List<Draft>>

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    fun getDraftById(draftId: Long): LiveData<Draft?>

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getDraftByIdSync(draftId: Long): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(draft: Draft): Long

    @Update
    suspend fun update(draft: Draft)

    @Delete
    suspend fun delete(draft: Draft)

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteById(draftId: Long)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM drafts")
    suspend fun getDraftCount(): Int
}
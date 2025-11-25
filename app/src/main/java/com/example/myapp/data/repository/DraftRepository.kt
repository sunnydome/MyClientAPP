package com.example.myapp.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.Draft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 草稿数据仓库
 * 负责草稿的本地存储
 */
class DraftRepository(private val database: AppDatabase) {

    private val draftDao = database.draftDao()

    /**
     * 获取所有草稿
     */
    fun getAllDrafts(): LiveData<List<Draft>> {
        return draftDao.getAllDrafts()
    }

    /**
     * 获取草稿
     */
    fun getDraftById(draftId: Long): LiveData<Draft?> {
        return draftDao.getDraftById(draftId)
    }

    /**
     * 获取草稿（同步）
     */
    suspend fun getDraftByIdSync(draftId: Long): Draft? {
        return withContext(Dispatchers.IO) {
            draftDao.getDraftByIdSync(draftId)
        }
    }

    /**
     * 保存草稿
     * @return 返回草稿ID
     */
    suspend fun saveDraft(
        title: String,
        content: String,
        imageUris: List<Uri>,
        existingDraftId: Long? = null
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val draft = Draft(
                    id = existingDraftId ?: 0,
                    title = title,
                    content = content,
                    imageUris = imageUris.map { it.toString() },
                    updateTime = System.currentTimeMillis()
                )

                val draftId = if (existingDraftId != null && existingDraftId > 0) {
                    draftDao.update(draft)
                    existingDraftId
                } else {
                    draftDao.insert(draft)
                }

                Result.success(draftId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除草稿
     */
    suspend fun deleteDraft(draftId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                draftDao.deleteById(draftId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 删除所有草稿
     */
    suspend fun deleteAllDrafts(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                draftDao.deleteAll()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取草稿数量
     */
    suspend fun getDraftCount(): Int {
        return withContext(Dispatchers.IO) {
            draftDao.getDraftCount()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DraftRepository? = null

        fun getInstance(database: AppDatabase): DraftRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DraftRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.example.myapp.data.repository

import android.net.Uri
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.Draft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
/**
 * 草稿数据仓库
 * 负责草稿的本地存储
 */
@Singleton
class DraftRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val draftDao = database.draftDao()

    // 获取唯一的草稿 (同步)
    suspend fun getSingleDraft(): Draft? {
        return withContext(Dispatchers.IO) {
            draftDao.getDraft()
        }
    }

    // 保存草稿 (总是覆盖同一个 ID)
    suspend fun saveDraft(
        title: String,
        content: String,
        imageUris: List<Uri>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val draft = Draft(
                    // ID 默认为 Draft.DRAFT_ID (1L)，不需要手动传
                    title = title,
                    content = content,
                    imageUris = imageUris.map { it.toString() },
                    updateTime = System.currentTimeMillis()
                )
                draftDao.insert(draft)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // 删除草稿
    suspend fun deleteDraft(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                draftDao.deleteById()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
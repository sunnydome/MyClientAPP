package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.User
import com.example.myapp.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val userApi = RetrofitClient.userApi

    // 内存缓存当前用户ID，实际项目中建议存放在 SharedPreferences/DataStore 中
    private var currentUserId: String? = null

    /**
     * 获取当前登录用户ID
     * 注意：如果还没登录，这个可能为空或需要处理
     */
    fun getCurrentUserId(): String {
        return currentUserId ?: "user_1" // 临时回退：如果没有ID，默认使用 Mock 的 user_1，防止崩溃
    }

    /**
     * 获取当前用户信息 (观察本地)
     */
    fun getCurrentUser(): LiveData<User?> {
        // 假设我们总是查询 ID 为 "me" 或者根据真实 ID 查询
        // 这里简化逻辑：先用 getCurrentUserId()
        return userDao.getUserById(getCurrentUserId())
    }

    /**
     * 同步获取当前用户信息
     */
    suspend fun getCurrentUserSync(): User? {
        return userDao.getUserByIdSync(getCurrentUserId())
    }

    /**
     * 刷新当前用户信息 (网络 -> 数据库)
     */
    suspend fun refreshCurrentUser(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApi.getMe()
                if (response.isSuccess() && response.data != null) {
                    val user = response.data

                    // 更新内存中的 ID
                    currentUserId = user.id

                    // 存入数据库
                    userDao.insert(user)
                    Result.success(user)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 切换关注状态
     */
    suspend fun toggleFollow(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApi.toggleFollow(userId)
                if (response.isSuccess() && response.data != null) {
                    val isFollowing = response.data
                    // 更新本地用户表中的关注状态
                    userDao.updateFollowStatus(userId, isFollowing)
                    Result.success(isFollowing)
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ... 单例模式代码保持不变 ...
    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(database: AppDatabase): UserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}
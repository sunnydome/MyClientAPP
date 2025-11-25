package com.example.myapp.data.repository

import androidx.lifecycle.LiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.mock.MockDataProvider
import com.example.myapp.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户数据仓库
 * 负责用户相关的数据操作
 */
class UserRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()

    /**
     * 获取当前登录用户ID
     */
    fun getCurrentUserId(): String = MockDataProvider.getCurrentUserId()

    /**
     * 获取当前用户信息
     */
    fun getCurrentUser(): LiveData<User?> {
        return userDao.getUserById(getCurrentUserId())
    }

    /**
     * 获取当前用户信息（同步）
     */
    suspend fun getCurrentUserSync(): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByIdSync(getCurrentUserId())
        }
    }

    /**
     * 获取用户信息
     */
    fun getUserById(userId: String): LiveData<User?> {
        return userDao.getUserById(userId)
    }

    /**
     * 获取用户信息（同步）
     */
    suspend fun getUserByIdSync(userId: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByIdSync(userId)
        }
    }

    /**
     * 获取关注的用户列表
     */
    fun getFollowingUsers(): LiveData<List<User>> {
        return userDao.getFollowingUsers()
    }

    /**
     * 切换关注状态
     */
    suspend fun toggleFollow(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUserByIdSync(userId)
                    ?: return@withContext Result.failure(Exception("用户不存在"))

                val newFollowStatus = !user.isFollowing
                userDao.updateFollowStatus(userId, newFollowStatus)

                Result.success(newFollowStatus)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                userDao.update(user)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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
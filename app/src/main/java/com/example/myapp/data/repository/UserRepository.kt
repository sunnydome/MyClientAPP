package com.example.myapp.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.User
import com.example.myapp.data.network.api.UserApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val database: AppDatabase,
    private val userApi : UserApi
){
    private val userDao = database.userDao()

    // 内存缓存当前用户ID，实际项目中建议存放在 SharedPreferences/DataStore 中
    private var currentUserId: String? = null

    /**
     * 获取当前登录用户ID
     * 注意：如果还没登录，这个可能为空或需要处理
     */
    fun getCurrentUserId(): String {
        return currentUserId ?: "u_8888" // 默认ID，对应 Apifox 里的 ID
    }

    /**
     * 获取当前用户信息 (观察本地)
     */
    fun getCurrentUser(): LiveData<User?> {
        // 先用 getCurrentUserId()
        return userDao.getUserById(getCurrentUserId())
    }

    /**
     * 获取当前用户信息 (同步)
     */
    suspend fun getCurrentUserSync(): User? {
        // 先尝试从内存/配置获取ID，再去数据库查
        val userId = currentUserId
        if (userId != null) {
            return userDao.getUserByIdSync(userId)
        }
        // 如果内存没ID，尝试查数据库里的第一条用户（针对单用户模式）
        // 或者直接返回 null，触发 refreshCurrentUser
        return null
    }

    /**
     * 刷新当前用户信息 (网络 -> 数据库)
     */
    suspend fun refreshCurrentUser(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // 请求 Apifox 的 /users/me 接口

                val response = userApi.getMe()

                if (response.isSuccess() && response.data != null) {
                    val user = response.data
                    // 更新内存ID
                    currentUserId = user.id

                    // 存入数据库 (这样下次 getCurrentUserSync 就能拿到了)
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

}
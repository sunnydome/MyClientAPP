package com.example.myapp.data.network.api

import com.example.myapp.data.model.User
import com.example.myapp.data.network.model.ApiResponse
import retrofit2.http.*

interface UserApi {

    /**
     * 获取当前登录用户信息
     */
    @GET("users/me")
    suspend fun getMe(): ApiResponse<User>

    /**
     * 获取指定用户信息
     */
    @GET("users/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: String
    ): ApiResponse<User>

    /**
     * 更新用户信息
     */
    @PUT("users/me")
    suspend fun updateProfile(
        @Body user: User
    ): ApiResponse<User>

    /**
     * 关注/取消关注用户
     */
    @POST("users/{userId}/follow")
    suspend fun toggleFollow(
        @Path("userId") userId: String
    ): ApiResponse<Boolean>
}
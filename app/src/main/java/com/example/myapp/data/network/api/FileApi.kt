package com.example.myapp.data.network.api

import com.example.myapp.data.network.model.ApiResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FileApi {

    /**
     * 上传单张图片
     */
    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): ApiResponse<String> // 返回图片 URL

    /**
     * 上传多张图片
     */
    @Multipart
    @POST("upload/images")
    suspend fun uploadImages(
        @Part files: List<MultipartBody.Part>
    ): ApiResponse<List<String>>
}
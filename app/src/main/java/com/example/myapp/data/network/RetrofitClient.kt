package com.example.myapp.data.network

import com.example.myapp.data.network.api.CommentApi
import com.example.myapp.data.network.api.FileApi
import com.example.myapp.data.network.api.PostApi
import com.example.myapp.data.network.api.SimpleApi
import com.example.myapp.data.network.api.UserApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 1. object 单例模式 - 整个应用只有一个实例
 * 2. lazy 延迟初始化 - 第一次使用时才创建
 * 3. Retrofit.Builder 构建器模式
 */
object RetrofitClient {

    /**
     * OkHttp客户端
     * 负责实际的网络请求
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 超时设置
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // 日志拦截器（重要！用于调试）
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    /**
     * Retrofit实例
     * 负责将接口转换为实际的网络请求
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            // Gson转换器：JSON <-> Kotlin对象
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val postApi: PostApi by lazy {
        retrofit.create(PostApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val commentApi: CommentApi by lazy {
        retrofit.create(CommentApi::class.java)
    }

    val fileApi: FileApi by lazy {
        retrofit.create(FileApi::class.java)
    }
}
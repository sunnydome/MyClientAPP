package com.example.myapp.data.network.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 1. Retrofit使用接口定义API
 * 2. @GET 表示HTTP GET请求
 * 3. @Query 表示URL查询参数 ?category=xxx
 * 4. @Path 表示路径参数 /posts/{postId}
 * 5. suspend 表示这是协程函数
 */
interface SimpleApi {

    /**
     * 获取Feed列表
     * 实际请求: GET /feeds?category=发现&limit=10
     */
    @GET("feeds")
    suspend fun getFeeds(
        @Query("category") category: String,
        @Query("limit") limit: Int = 10
    ): FeedResponse

    /**
     * 获取帖子详情
     * 实际请求: GET /posts/post_123
     */
    @GET("posts/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: String
    ): PostDetailResponse
}

// ============ 响应数据类 ============

/**
 * 通用响应包装
 * 对应服务端返回的JSON结构
 */
data class ApiResponse<T>(
    val code: Int,        // 0=成功
    val message: String,
    val data: T?
)

/**
 * Feed列表响应
 */
data class FeedResponse(
    val code: Int,
    val message: String,
    val data: FeedData?
)

data class FeedData(
    val list: List<FeedItemDto>,
    val hasMore: Boolean
)

/**
 * Feed项目（网络传输格式）
 */
data class FeedItemDto(
    val id: String,
    val coverUrl: String,
    val coverAspectRatio: Float = 1.0f,
    val title: String,
    val authorName: String,
    val authorAvatar: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

/**
 * 帖子详情响应
 */
data class PostDetailResponse(
    val code: Int,
    val message: String,
    val data: PostDto?
)

/**
 * 帖子详情（网络传输格式）
 */
data class PostDto(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val title: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val coverUrl: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false
)
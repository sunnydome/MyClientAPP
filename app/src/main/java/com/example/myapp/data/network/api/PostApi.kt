package com.example.myapp.data.network.api

import com.example.myapp.data.model.Post
import com.example.myapp.data.network.model.ApiResponse
import com.example.myapp.data.network.model.PageResponse
import retrofit2.http.*

interface PostApi {

    // ============ 读操作 ============

    /**
     * 获取首页 Feed 列表
     * @param category 分类 (发现, 关注, 同城)
     * @param page 页码 (或使用 cursor)
     */
    @GET("feeds")
    suspend fun getFeeds(
        @Query("category") category: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): ApiResponse<PageResponse<Post>>

    /**
     * 获取帖子详情
     */
    @GET("posts/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: String
    ): ApiResponse<Post>

    /**
     * 获取指定用户的帖子列表 (个人主页)
     */
    @GET("users/{userId}/posts")
    suspend fun getUserPosts(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1
    ): ApiResponse<PageResponse<Post>>

    // ============ 写操作 ============

    /**
     * 发布帖子
     * 注意：这里接收的是 DTO，而不是完整的 Post 实体，因为 ID 和时间通常由服务器生成
     */
    @POST("posts/publish")
    suspend fun publishPost(
        @Body request: PublishPostRequest
    ): ApiResponse<Post>

    /**
     * 删除帖子
     */
    @DELETE("posts/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: String
    ): ApiResponse<Unit>

    // ============ 交互操作 ============

    /**
     * 点赞/取消点赞
     */
    @POST("posts/{postId}/like")
    suspend fun toggleLike(
        @Path("postId") postId: String
    ): ApiResponse<Boolean> // 返回最新的点赞状态

    /**
     * 收藏/取消收藏
     */
    @POST("posts/{postId}/collect")
    suspend fun toggleCollect(
        @Path("postId") postId: String
    ): ApiResponse<Boolean>
}

// 发布帖子的请求体
data class PublishPostRequest(
    val title: String,
    val content: String,
    val category: String,
    val imageUrls: List<String>, // 这里是上传后的图片 URL，不是本地 Uri
    val location: String?
)
package com.example.myapp.data.network.api

import com.example.myapp.data.model.Comment
import com.example.myapp.data.network.model.ApiResponse
import retrofit2.http.*

interface CommentApi {

    /**
     * 获取某个帖子的评论列表
     * 通常按热度或时间排序
     */
    @GET("posts/{postId}/comments")
    suspend fun getComments(
        @Path("postId") postId: String,
        @Query("page") page: Int = 1
    ): ApiResponse<List<Comment>>

    /**
     * 发送评论
     */
    @POST("posts/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: String,
        @Body request: AddCommentRequest
    ): ApiResponse<Comment>

    /**
     * 删除评论
     */
    @DELETE("comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String
    ): ApiResponse<Unit>

    /**
     * 评论点赞
     */
    @POST("comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("commentId") commentId: String
    ): ApiResponse<Boolean>
}

// 发送评论请求体
data class AddCommentRequest(
    val content: String,
    val parentId: String? = null, // 如果是回复评论，则带上父ID
    val replyToUserId: String? = null // 回复给谁
)
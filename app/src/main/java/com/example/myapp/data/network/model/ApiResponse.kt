package com.example.myapp.data.network.model

/**
 * 标准的 API 响应封装
 * 对应后端通常返回的 JSON 结构: { "code": 0, "message": "success", "data": {...} }
 */
data class ApiResponse<T>(
    val code: Int,          // 业务状态码，例如 0 代表成功
    val message: String,    // 提示信息
    val data: T?            // 实际数据泛型
) {
    // 辅助方法：判断业务是否成功
    fun isSuccess(): Boolean = code == 0
}

/**
 * 分页列表响应封装 (针对 Feed 流等)
 */
data class PageResponse<T>(
    val list: List<T>,
    val hasMore: Boolean,
    val nextCursor: String? = null // 下一页的游标/页码
)
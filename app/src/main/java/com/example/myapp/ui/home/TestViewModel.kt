package com.example.myapp.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.network.RetrofitClient
import com.example.myapp.data.network.api.FeedItemDto
import kotlinx.coroutines.launch

/**
 * 阶段1：测试网络请求的ViewModel
 *
 * 学习要点：
 * 1. viewModelScope - ViewModel专用协程作用域，自动管理生命周期
 * 2. launch - 启动一个协程
 * 3. try-catch - 网络请求必须处理异常
 * 4. LiveData - 用于通知UI更新
 */
class TestViewModel : ViewModel() {

    // 私有可变，公开只读
    private val _feeds = MutableLiveData<List<FeedItemDto>>()
    val feeds: LiveData<List<FeedItemDto>> = _feeds

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    companion object {
        private const val TAG = "TestViewModel"
    }

    /**
     * 加载Feed数据
     * 这是你的第一个网络请求！
     */
    fun loadFeeds(category: String = "发现") {
        // 启动协程
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 发起网络请求
                Log.d(TAG, "开始请求: category=$category")

                val response = RetrofitClient.api.getFeeds(category)

                // 打印响应（查看LogCat）
                Log.d(TAG, "响应码: ${response.code}")
                Log.d(TAG, "响应消息: ${response.message}")
                Log.d(TAG, "数据条数: ${response.data?.list?.size ?: 0}")

                // 检查响应
                if (response.code == 0 && response.data != null) {
                    _feeds.value = response.data.list
                    Log.d(TAG, "✅ 请求成功！获取到 ${response.data.list.size} 条数据")
                } else {
                    _error.value = response.message
                    Log.e(TAG, "❌ 业务错误: ${response.message}")
                }

            } catch (e: Exception) {
                // 网络异常
                Log.e(TAG, "❌ 网络异常: ${e.message}", e)
                _error.value = "网络请求失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 测试获取帖子详情
     */
    fun testGetPostDetail(postId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "请求帖子详情: $postId")

                val response = RetrofitClient.api.getPostDetail(postId)

                if (response.code == 0 && response.data != null) {
                    val post = response.data
                    Log.d(TAG, """
                        ✅ 帖子详情:
                        - ID: ${post.id}
                        - 标题: ${post.title}
                        - 作者: ${post.authorName}
                        - 点赞数: ${post.likeCount}
                        - 图片数: ${post.imageUrls.size}
                    """.trimIndent())
                } else {
                    Log.e(TAG, "❌ 获取失败: ${response.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 异常: ${e.message}", e)
            }
        }
    }
}

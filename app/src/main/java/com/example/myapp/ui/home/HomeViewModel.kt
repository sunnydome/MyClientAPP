package com.example.myapp.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.FeedItem
import com.example.myapp.data.repository.PostRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)

    private var isLoadingMore = false
    // 当前选中的分类
    private val _currentCategory = MutableLiveData<String>("发现")
    val currentCategory: LiveData<String> = _currentCategory

    // 缓存每个分类的 LiveData (从数据库读取)
    private val feedsCache = mutableMapOf<String, LiveData<List<FeedItem>>>()

    // 缓存每个分类的当前页码
    private val pageCache = mutableMapOf<String, Int>()

    // 加载状态 (网络请求中)
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * 获取指定类别的 LiveData (UI 观察源)
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return feedsCache.getOrPut(category) {
            postRepository.getFeedsByCategory(category)
        }
    }

    /**
     * 切换 Tab 时调用，如果从未加载过则触发网络请求
     */
    fun loadDataForTab(category: String) {
        _currentCategory.value = category

        // 如果该分类从未加载过网络数据 (页码为 null 或 0)，则触发刷新
        if (pageCache[category] == null) {
            refresh(category)
        }
    }

    /**
     * 下拉刷新：重置页码为 1，请求最新数据
     */
    fun refresh(category: String) {
        Log.d("HomeViewModel", "🔄 UI触发刷新: $category")
        if (_isLoading.value == true || isLoadingMore) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 调用 Repository 从网络拉取第一页，并写入数据库
            val result = postRepository.fetchFeeds(category, page = 1)

            _isLoading.value = false

            result.fold(
                onSuccess = { hasMore ->
                    // 刷新成功，重置页码
                    pageCache[category] = 1
                },
                onFailure = { e ->
                    _error.value = e.message ?: "刷新失败"
                }
            )
        }
    }

    /**
     * 上拉加载更多
     */
    fun loadMore(category: String) {
        // 1. 严格的防抖检查：如果正在加载更多，或者全局正在刷新，直接返回
        if (isLoadingMore || _isLoading.value == true) {
            return
        }

        // 2. 立即上锁！(在协程启动前)
        isLoadingMore = true

        // 这里的页码计算逻辑保持不变
        val currentPage = pageCache[category] ?: 1
        val nextPage = currentPage + 1

        viewModelScope.launch {
            // 注意：这里不需要 _isLoading.value = true，否则下拉刷新的圈圈会弹出来

            // 3. 发起请求
            val result = postRepository.fetchFeeds(category, page = nextPage)

            // 4. 请求结束，解锁
            isLoadingMore = false

            result.fold(
                onSuccess = { hasMore ->
                    // 成功后更新页码
                    pageCache[category] = nextPage
                    // TODO: 如果 hasMore 为 false，可以标记该分类已到底，不再触发 loadMore
                },
                onFailure = { e ->
                    // 失败处理：可以通过一个单独的 LiveData 通知 UI 显示 Toast，而不是改变全局 error 状态
                    // _error.value = "加载更多失败: ${e.message}"
                    // 暂时只打印日志，不干扰 UI
                    android.util.Log.e("HomeViewModel", "Load more failed", e)
                }
            )
        }
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            // 调用 Repository，它会负责乐观更新本地 + 发送网络请求
            val result = postRepository.toggleLike(postId)

            if (result.isFailure) {
                _error.value = "点赞失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
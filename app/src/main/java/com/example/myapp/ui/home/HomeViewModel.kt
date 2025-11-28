package com.example.myapp.ui.home

import android.app.Application
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

    // ... (_currentCategory, feedsCache, pageCache 等保持不变) ...
    // 当前选中的分类
    private val _currentCategory = MutableLiveData<String>("发现")
    val currentCategory: LiveData<String> = _currentCategory

    // 缓存每个分类的 LiveData
    private val feedsCache = mutableMapOf<String, LiveData<List<FeedItem>>>()

    // 缓存每个分类的当前页码
    private val pageCache = mutableMapOf<String, Int>()

    // 废弃全局 isLoading，改为 Map 存储每个分类的加载状态
    private val loadingStateCache = mutableMapOf<String, MutableLiveData<Boolean>>()

    // 全局错误信息 (这个可以保留全局，或者也改成 Map，这里暂用全局)
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 获取指定分类的加载状态 LiveData
    fun getLoadingState(category: String): LiveData<Boolean> {
        return loadingStateCache.getOrPut(category) {
            MutableLiveData(false)
        }
    }

    // 辅助方法：设置特定分类的加载状态
    private fun setLoading(category: String, isLoading: Boolean) {
        loadingStateCache.getOrPut(category) { MutableLiveData(false) }.value = isLoading
    }

    // 辅助方法：读取特定分类是否正在加载
    private fun isLoading(category: String): Boolean {
        return loadingStateCache[category]?.value == true
    }

    // 专门用于上拉加载的锁 Map，防止单分类并发
    private val loadingMoreStateMap = mutableMapOf<String, Boolean>()

    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return feedsCache.getOrPut(category) {
            postRepository.getFeedsByCategory(category)
        }
    }

    fun loadDataForTab(category: String) {
        _currentCategory.value = category
        if (pageCache[category] == null) {
            refresh(category)
        }
    }

    /**
     * 下拉刷新
     */
    fun refresh(category: String) {
        // 只检查【当前分类】是否正在加载
        if (isLoading(category)) return

        viewModelScope.launch {
            setLoading(category, true) // 开启当前分类的 Loading
            _error.value = null

            val result = postRepository.fetchFeeds(category, page = 1)

            setLoading(category, false) // 关闭当前分类的 Loading

            result.fold(
                onSuccess = {
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
        // 检查锁：当前分类正在 Loading 或者正在 LoadingMore
        if (isLoading(category) || loadingMoreStateMap[category] == true) {
            return
        }

        loadingMoreStateMap[category] = true // 上锁

        val currentPage = pageCache[category] ?: 1
        val nextPage = currentPage + 1

        viewModelScope.launch {
            val result = postRepository.fetchFeeds(category, page = nextPage)

            loadingMoreStateMap[category] = false // 解锁

            result.fold(
                onSuccess = { hasMore ->
                    pageCache[category] = nextPage
                },
                onFailure = { e ->
                    android.util.Log.e("HomeViewModel", "Load more failed", e)
                }
            )
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
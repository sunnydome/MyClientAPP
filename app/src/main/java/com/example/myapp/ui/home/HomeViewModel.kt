package com.example.myapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.FeedItem
import com.example.myapp.data.repository.PostRepository
import kotlinx.coroutines.launch
import kotlin.math.max

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)

    private val _currentCategory = MutableLiveData<String>("发现")
    val currentCategory: LiveData<String> = _currentCategory

    // 显式声明 Value 类型为 MediatorLiveData<MutableList<FeedItem>>
    // 这样取出来的 value 才是 MutableList，才支持 set 操作
    private val feedsCache = mutableMapOf<String, MediatorLiveData<MutableList<FeedItem>>>()

    private val pageCache = mutableMapOf<String, Int>()
    private val loadingStateCache = mutableMapOf<String, MutableLiveData<Boolean>>()
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val loadingMoreStateMap = mutableMapOf<String, Boolean>()

    fun getLoadingState(category: String): LiveData<Boolean> =
        loadingStateCache.getOrPut(category) { MutableLiveData(false) }

    private fun setLoading(category: String, isLoading: Boolean) {
        loadingStateCache.getOrPut(category) { MutableLiveData(false) }.value = isLoading
    }

    private fun isLoading(category: String): Boolean =
        loadingStateCache[category]?.value == true

    /**
     * 获取 Feed 列表
     * [关键修复 2] 返回类型改为 LiveData<MutableList<FeedItem>>
     * 虽然 UI 只需要 List，但为了类型匹配，这里直接返回 MutableList 也是兼容的
     */
    fun getFeedsByCategory(category: String): LiveData<MutableList<FeedItem>> {
        return feedsCache.getOrPut(category) {
            val mediator = MediatorLiveData<MutableList<FeedItem>>()
            val dbSource = postRepository.getFeedsByCategory(category) // 这里返回的是 List

            mediator.addSource(dbSource) { list ->
                // [关键修复 3] 将只读 List 转换为可变 ArrayList，这样后续才能修改
                mediator.value = ArrayList(list)
            }
            mediator
        }
    }

    fun loadDataForTab(category: String) {
        _currentCategory.value = category
        if (pageCache[category] == null) refresh(category)
    }

    fun refresh(category: String) {
        if (isLoading(category)) return
        viewModelScope.launch {
            setLoading(category, true)
            _error.value = null
            val result = postRepository.fetchFeeds(category, page = 1)
            setLoading(category, false)
            result.fold(
                onSuccess = { pageCache[category] = 1 },
                onFailure = { e -> _error.value = e.message ?: "刷新失败" }
            )
        }
    }

    fun loadMore(category: String) {
        if (isLoading(category) || loadingMoreStateMap[category] == true) return
        loadingMoreStateMap[category] = true
        val currentPage = pageCache[category] ?: 1
        val nextPage = currentPage + 1
        viewModelScope.launch {
            val result = postRepository.fetchFeeds(category, page = nextPage)
            loadingMoreStateMap[category] = false
            result.onSuccess { pageCache[category] = nextPage }
        }
    }

    fun toggleLike(postId: String) {
        // feedsCache.values 里的 mediator.value 现在被识别为 MutableList
        // 所以可以使用 list[index] = item 的语法
        feedsCache.values.forEach { mediator ->
            val list = mediator.value
            if (list != null) {
                val index = list.indexOfFirst { it.id == postId }
                if (index != -1) {
                    val item = list[index]
                    val newStatus = !item.isLiked
                    val newCount = if (newStatus) item.likeCount + 1 else max(0, item.likeCount - 1)

                    // 静默替换元素，不触发 LiveData 通知，避免刷新闪烁
                    list[index] = item.copy(isLiked = newStatus, likeCount = newCount)
                }
            }
        }

        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }

    fun clearError() { _error.value = null }
}
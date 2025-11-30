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

    // ... (_currentCategory, feedsCache, pageCache 等保持不变) ...
    // 当前选中的分类
    private val _currentCategory = MutableLiveData<String>("发现")
    val currentCategory: LiveData<String> = _currentCategory

    // 缓存每个分类的 LiveData
    private val feedsCache = mutableMapOf<String, MediatorLiveData<List<FeedItem>>>()

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

    /**
     * 使用 MediatorLiveData 组合数据源：
     * 1. 监听数据库 (addSource): 当下拉刷新/加载更多写入数据库时，自动更新 UI。
     * 2. 内存修改 (setValue): 当点赞时，直接修改内存列表，立即更新 UI。
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return feedsCache.getOrPut(category) {
            val mediator = MediatorLiveData<List<FeedItem>>()

            // 获取数据库源
            val dbSource = postRepository.getFeedsByCategory(category)

            // 将数据库源接入 Mediator
            mediator.addSource(dbSource) { list ->
                // 只有当数据库真的有数据变动（比如网络刷新回来）时，才覆盖内存数据
                mediator.value = list
            }
            mediator
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

    /**
     * 切换点赞 - 纯内存操作 + 异步网络请求
     * 解决点赞导致列表刷新闪烁、卡顿的问题
     */
    fun toggleLike(postId: String) {
        // 遍历所有缓存的列表（因为同一个帖子可能出现在"发现"和"关注"里）
        // 直接在内存中找到该帖子并修改状态，触发 LiveData 通知 UI 局部刷新
        feedsCache.values.forEach { mediator ->
            val currentList = mediator.value ?: return@forEach

            // 查找列表中是否有这个 ID
            val index = currentList.indexOfFirst { it.id == postId }
            if (index != -1) {
                // 复制列表（必须创建新集合，否则 LiveData 可能不通知）
                val newList = ArrayList(currentList)
                val item = newList[index]

                // 计算新状态
                val newStatus = !item.isLiked
                val newCount = if (newStatus) item.likeCount + 1 else max(0, item.likeCount - 1)

                // 复制 Item 并修改属性
                val newItem = item.copy(
                    isLiked = newStatus,
                    likeCount = newCount
                )

                // 替换列表中的旧 Item
                newList[index] = newItem

                // 立即更新 UI
                mediator.value = newList
            }
        }

        // Fire and Forget (发后即忘)
        // 即使失败也不回滚，保证 UI 顺滑
        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
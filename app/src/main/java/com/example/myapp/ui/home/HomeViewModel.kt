package com.example.myapp.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.FeedItem
import com.example.myapp.data.repository.PostRepository

/**
 * HomeViewModel - 管理首页的数据
 * 使用Repository模式获取数据
 *
 * 更新说明：使用新的数据层架构
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // 通过application参数获取数据库和Repository
    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)

    // 当前选中的分类
    private val _currentCategory = MutableLiveData<String>("发现")
    val currentCategory: LiveData<String> = _currentCategory

    // 为每个类别维护独立的LiveData缓存
    private val feedsCache = mutableMapOf<String, LiveData<List<FeedItem>>>()

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * 获取指定类别的Feed数据
     * @param category 类别名称
     * @return 对应类别的LiveData
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedItem>> {
        return feedsCache.getOrPut(category) {
            postRepository.getFeedsByCategory(category)
        }
    }

    /**
     * 加载指定Tab的数据
     * @param category 类别
     * @param forceRefresh 是否强制刷新
     */
    fun loadDataForTab(category: String, forceRefresh: Boolean = false) {
        _currentCategory.value = category

        // 由于使用Room的LiveData，数据会自动更新
        // 如果需要强制刷新（如下拉刷新），可以在这里触发网络请求
        if (forceRefresh) {
            // TODO: 实现网络刷新逻辑
            // 目前使用本地数据库，数据会自动同步
        }

        // 确保缓存中有该类别的LiveData
        if (!feedsCache.containsKey(category)) {
            feedsCache[category] = postRepository.getFeedsByCategory(category)
        }
    }

    /**
     * 切换点赞状态
     * 启动协程调用 Repository，数据库更新后 LiveData 会自动通知 UI 刷新
     */
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            // 调用仓库层的切换点赞方法
            // Repository 内部会更新数据库中的 isLiked 和 likeCount 字段
            postRepository.toggleLike(postId)
        }
    }
    /**
     * 刷新指定类别的数据
     * @param category 类别名称
     */
    fun refreshCategory(category: String) {
        loadDataForTab(category, forceRefresh = true)
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        feedsCache.clear()
    }
}
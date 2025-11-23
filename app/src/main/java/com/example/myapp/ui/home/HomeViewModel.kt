package com.example.myapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.ui.home.recyclerPostView.FeedRepository

/**
 * HomeViewModel - 管理首页的数据
 * 为每个类别维护独立的LiveData
 */
class HomeViewModel : ViewModel() {
    private val feedRepository = FeedRepository()

    // 为每个类别维护独立的LiveData
    private val _feedsMap = mutableMapOf<String, MutableLiveData<List<FeedModel>>>()

    /**
     * 获取指定类别的Feed数据
     * @param category 类别名称
     * @return 对应类别的LiveData
     */
    fun getFeedsByCategory(category: String): LiveData<List<FeedModel>> {
        // 如果该类别的LiveData不存在，则创建
        if (!_feedsMap.containsKey(category)) {
            _feedsMap[category] = MutableLiveData<List<FeedModel>>()
        }
        return _feedsMap[category]!!
    }

    /**
     * 根据Tab类别加载数据
     * @param tabCategory 类别名称
     */
    fun loadDataForTab(tabCategory: String) {
        // 从Repository获取数据
        feedRepository.fetchFeedsByCategory(tabCategory).observeForever { feedList ->
            // 更新对应类别的LiveData
            if (!_feedsMap.containsKey(tabCategory)) {
                _feedsMap[tabCategory] = MutableLiveData<List<FeedModel>>()
            }
            _feedsMap[tabCategory]?.value = feedList
        }
    }

    /**
     * 刷新指定类别的数据
     * @param category 类别名称
     */
    fun refreshCategory(category: String) {
        loadDataForTab(category)
    }

    /**
     * 清除指定类别的数据
     * @param category 类别名称
     */
    fun clearCategory(category: String) {
        _feedsMap[category]?.value = emptyList()
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        _feedsMap.values.forEach { it.value = emptyList() }
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel销毁时清理资源
        _feedsMap.clear()
    }
}
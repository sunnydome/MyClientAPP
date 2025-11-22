package com.example.myapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val feedRepository = FeedRepository()

    // 声明一个 LiveData 变量
    private val _feeds = MutableLiveData<List<FeedModel>>()

    // 自定义 getFeeds() 方法
    fun getFeeds(): LiveData<List<FeedModel>> {
        return _feeds
    }

    // 获取初始的 feed 数据（默认是关注）
    init {
        loadDataForTab("关注") // 默认加载关注数据
    }

    // 根据 tab 加载不同的 feed 数据
    fun loadDataForTab(tabCategory: String) {
        feedRepository.fetchFeedsByCategory(tabCategory).observeForever {
            _feeds.value = it
        }
    }
}
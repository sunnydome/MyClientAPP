package com.example.myapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FeedRepository {
    fun fetchFeeds(): LiveData<List<FeedModel>> {
        val liveData = MutableLiveData<List<FeedModel>>()
        // 这里你可以请求网络数据或者读取本地数据库
        liveData.value = listOf(
            FeedModel("1", "https://example.com/image1.jpg", "这是一条描述", "用户1", "https://example.com/user1.jpg"),
            FeedModel("2", "https://example.com/image2.jpg", "这是一条描述", "用户2", "https://example.com/user2.jpg")
        )
        return liveData
    }
}
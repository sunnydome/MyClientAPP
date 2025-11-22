package com.example.myapp.ui.home.RecyclerPostView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapp.ui.home.FeedModel

class FeedRepository {
    // 模拟不同类别的 Feed 数据
    private val feeds关注 = listOf(
        FeedModel(
            "1",
            "https://example.com/image1.jpg",
            "关注内容描述1",
            "用户1",
            "https://example.com/user1.jpg"
        ),
        FeedModel(
            "2",
            "https://example.com/image2.jpg",
            "关注内容描述2",
            "用户2",
            "https://example.com/user2.jpg"
        )
    )

    private val feeds发现 = listOf(
        FeedModel(
            "3",
            "https://example.com/image3.jpg",
            "发现内容描述1",
            "用户3",
            "https://example.com/user3.jpg"
        ),
        FeedModel(
            "4",
            "https://example.com/image4.jpg",
            "发现内容描述2",
            "用户4",
            "https://example.com/user4.jpg"
        ),
        FeedModel(
            "4",
            "https://example.com/image4.jpg",
            "发现内容描述2",
            "用户4",
            "https://example.com/user4.jpg"
        ),
        FeedModel(
            "3",
            "https://example.com/image3.jpg",
            "发现内容描述1",
            "用户3",
            "https://example.com/user3.jpg"
        ),
        FeedModel(
            "4",
            "https://example.com/image4.jpg",
            "发现内容描述2",
            "用户4",
            "https://example.com/user4.jpg"
        ),
        FeedModel(
            "3",
            "https://example.com/image3.jpg",
            "发现内容描述1",
            "用户3",
            "https://example.com/user3.jpg"
        ),
        FeedModel(
            "4",
            "https://example.com/image4.jpg",
            "发现内容描述2",
            "用户4",
            "https://example.com/user4.jpg"
        )
    )

    private val feeds同城 = listOf(
        FeedModel(
            "5",
            "https://example.com/image5.jpg",
            "同城内容描述1",
            "用户5",
            "https://example.com/user5.jpg"
        ),
        FeedModel(
            "6",
            "https://example.com/image6.jpg",
            "同城内容描述2",
            "用户6",
            "https://example.com/user6.jpg"
        )
    )

    // 根据 tab 分类返回不同的数据
    fun fetchFeedsByCategory(category: String): LiveData<List<FeedModel>> {
        val liveData = MutableLiveData<List<FeedModel>>()
        liveData.value = when (category) {
            "关注" -> feeds关注
            "发现" -> feeds发现
            "同城" -> feeds同城
            else -> emptyList()
        }
        return liveData
    }

    // 默认获取所有数据的方法
    fun fetchFeeds(): LiveData<List<FeedModel>> {
        val liveData = MutableLiveData<List<FeedModel>>()
        liveData.value = feeds关注 + feeds发现 + feeds同城
        return liveData
    }
}
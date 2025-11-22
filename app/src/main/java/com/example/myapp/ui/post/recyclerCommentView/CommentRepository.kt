package com.example.myapp.ui.post.recyclerCommentView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapp.ui.post.CommentModel

class CommentRepository {
    private val comments = listOf<CommentModel>(
        CommentModel(
            "1",
            "一小时前",
            "评论1，这条评论将会很长，它用来测试是否能够正确的换行和表达",
            "用户1",
            "https://example.com/user1.jpg"
        ),
        CommentModel(
        "1",
        "一小时前",
        "评论1，这条评论将会很长，它用来测试是否能够正确的换行和表达",
        "用户1",
        "https://example.com/user1.jpg"
        ),
        CommentModel(
            "1",
            "一小时前",
            "评论1，这条评论将会很长，它用来测试是否能够正确的换行和表达",
            "用户1",
            "https://example.com/user1.jpg"
        ),
        CommentModel(
                "1",
        "一小时前",
        "评论1，这条评论将会很长，它用来测试是否能够正确的换行和表达",
        "用户1",
        "https://example.com/user1.jpg"
        ),
        CommentModel(
            "1",
        "一小时前",
        "评论1，这条评论将会很长，它用来测试是否能够正确的换行和表达",
        "用户1",
        "https://example.com/user1.jpg"
        )
    )
    fun fetchComments(): LiveData<List<CommentModel>> {
        val liveData = MutableLiveData<List<CommentModel>>()
        liveData.value = comments
        return liveData
    }
}
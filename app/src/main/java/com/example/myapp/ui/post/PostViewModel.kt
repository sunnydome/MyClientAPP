package com.example.myapp.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.ui.post.recyclerCommentView.CommentRepository
import android.util.Log
import kotlin.math.log

class PostViewModel : ViewModel() {
    private val commentRepository = CommentRepository()
    // 声明一个 LiveData 变量
    private val _comments = MutableLiveData<List<CommentModel>>()

    fun getComments(): LiveData<List<CommentModel>> {
        return _comments
    }

    init {
        loadData()
    }
    fun loadData() {
        commentRepository.fetchComments().observeForever {
            _comments.value = it
            Log.d("here", "is")
        }
    }
}
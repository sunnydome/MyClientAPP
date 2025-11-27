package com.example.myapp.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.Comment
import com.example.myapp.data.model.Post
import com.example.myapp.data.repository.CommentRepository
import com.example.myapp.data.repository.PostRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)
    private val commentRepository: CommentRepository = CommentRepository.getInstance(database)

    private var currentPostId: String? = null

    // 帖子详情 (观察本地数据库)
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    // 评论列表 (观察本地数据库)
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _actionEvent = MutableLiveData<ActionEvent?>()
    val actionEvent: LiveData<ActionEvent?> = _actionEvent

    /**
     * 初始化加载
     */
    fun loadPost(postId: String) {
        currentPostId = postId

        // 1. 立即观察本地数据库，保证秒开
        postRepository.getPostById(postId).observeForever { post ->
            _post.value = post
        }
        commentRepository.getTopLevelComments(postId).observeForever { comments ->
            _comments.value = comments
        }

        // 2. 后台请求最新数据 (静默刷新)
        refreshData(postId)
    }

    /**
     * 刷新帖子详情和评论
     */
    fun refreshData(postId: String) {
        viewModelScope.launch {
            // 并行请求详情和评论
            try {
                coroutineScope {
                    val deferredPost = async { postRepository.fetchPostDetail(postId) }
                    val deferredComments = async { commentRepository.refreshComments(postId) }

                    val postResult = deferredPost.await()
                    val commentResult = deferredComments.await()

                    // 这里可以处理错误，比如 Toast 提示“网络连接失败”，但不需要清空 _post 数据
                    if (postResult.isFailure) {
                        // log error
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 切换点赞
     */
    fun toggleLike() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            // UI 已经在 Repository 的乐观更新中变了，这里只需处理失败回滚提示
            val result = postRepository.toggleLike(postId)
            result.onSuccess { isLiked ->
                _actionEvent.value = ActionEvent.LikeChanged(isLiked)
            }.onFailure { e ->
                _actionEvent.value = ActionEvent.Error("点赞失败: ${e.message}")
            }
        }
    }

    /**
     * 切换收藏
     */
    fun toggleCollect() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            val result = postRepository.toggleCollect(postId)
            result.onSuccess { isCollected ->
                _actionEvent.value = ActionEvent.CollectChanged(isCollected)
            }.onFailure { e ->
                _actionEvent.value = ActionEvent.Error("收藏失败: ${e.message}")
            }
        }
    }

    /**
     * 发表评论
     */
    fun addComment(content: String, parentId: String? = null, replyToName: String? = null) {
        val postId = currentPostId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.addComment(postId, content, parentId, replyToName)
            _isLoading.value = false

            result.fold(
                onSuccess = { comment ->
                    _actionEvent.value = ActionEvent.CommentAdded(comment)
                },
                onFailure = { e ->
                    _actionEvent.value = ActionEvent.Error(e.message ?: "评论失败")
                }
            )
        }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            commentRepository.toggleLike(commentId)
            // 结果通过 LiveData 自动更新
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Repository 还没加 deleteComment 的网络实现，暂时只删本地?
            // 假设 Repository 已经更新了 deleteComment
            val result = commentRepository.deleteComment(commentId)
            // 这里需要去 CommentRepository 确保 deleteComment 也实现了 suspend 网络请求
            _isLoading.value = false

            result.onFailure {
                _actionEvent.value = ActionEvent.Error("删除失败")
            }
        }
    }

    fun clearActionEvent() {
        _actionEvent.value = null
    }

    sealed class ActionEvent {
        data class LikeChanged(val isLiked: Boolean) : ActionEvent()
        data class CollectChanged(val isCollected: Boolean) : ActionEvent()
        data class CommentAdded(val comment: Comment) : ActionEvent()
        data class Error(val message: String) : ActionEvent()
    }
}
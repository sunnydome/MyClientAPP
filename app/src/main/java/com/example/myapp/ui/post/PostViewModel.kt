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

/**
 * 帖子详情页的ViewModel
 * 管理帖子内容和评论列表
 *
 * 更新说明：使用新的数据层架构
 */
class PostViewModel(application: Application) : AndroidViewModel(application) {

    // 通过application参数获取数据库和Repository
    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)
    private val commentRepository: CommentRepository = CommentRepository.getInstance(database)

    // 当前帖子ID
    private var currentPostId: String? = null

    // 帖子详情
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    // 评论列表
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 操作结果事件
    private val _actionEvent = MutableLiveData<ActionEvent?>()
    val actionEvent: LiveData<ActionEvent?> = _actionEvent

    /**
     * 加载帖子详情
     * @param postId 帖子ID
     */
    fun loadPost(postId: String) {
        currentPostId = postId

        // 观察帖子数据
        postRepository.getPostById(postId).observeForever { post ->
            _post.value = post
        }

        // 加载评论
        loadComments(postId)
    }

    /**
     * 加载评论列表
     */
    private fun loadComments(postId: String) {
        commentRepository.getTopLevelComments(postId).observeForever { comments ->
            _comments.value = comments
        }
    }


    /**
     * 切换点赞状态
     */
    fun toggleLike() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = postRepository.toggleLike(postId)
            _isLoading.value = false

            result.fold(
                onSuccess = { isLiked ->
                    _actionEvent.value = ActionEvent.LikeChanged(isLiked)
                },
                onFailure = { error ->
                    _actionEvent.value = ActionEvent.Error(error.message ?: "操作失败")
                }
            )
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleCollect() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = postRepository.toggleCollect(postId)
            _isLoading.value = false

            result.fold(
                onSuccess = { isCollected ->
                    _actionEvent.value = ActionEvent.CollectChanged(isCollected)
                },
                onFailure = { error ->
                    _actionEvent.value = ActionEvent.Error(error.message ?: "操作失败")
                }
            )
        }
    }

    /**
     * 发表评论
     * @param content 评论内容
     * @param parentId 父评论ID（如果是回复的话）
     * @param replyToName 被回复者名称
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
                onFailure = { error ->
                    _actionEvent.value = ActionEvent.Error(error.message ?: "评论失败")
                }
            )
        }
    }

    /**
     * 切换评论点赞
     * @param commentId 评论ID
     */
    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            val result = commentRepository.toggleLike(commentId)
            result.fold(
                onSuccess = { isLiked ->
                    _actionEvent.value = ActionEvent.CommentLikeChanged(commentId, isLiked)
                },
                onFailure = { error ->
                    _actionEvent.value = ActionEvent.Error(error.message ?: "操作失败")
                }
            )
        }
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     */
    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = commentRepository.deleteComment(commentId)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _actionEvent.value = ActionEvent.CommentDeleted(commentId)
                },
                onFailure = { error ->
                    _actionEvent.value = ActionEvent.Error(error.message ?: "删除失败")
                }
            )
        }
    }

    /**
     * 清除事件状态
     */
    fun clearActionEvent() {
        _actionEvent.value = null
    }

    /**
     * 兼容旧代码的loadData方法
     */
    fun loadData() {
        // 如果已有postId则加载，否则等待外部调用loadPost
        currentPostId?.let { loadPost(it) }
    }

    /**
     * 操作事件密封类
     */
    sealed class ActionEvent {
        data class LikeChanged(val isLiked: Boolean) : ActionEvent()
        data class CollectChanged(val isCollected: Boolean) : ActionEvent()
        data class CommentAdded(val comment: Comment) : ActionEvent()
        data class CommentLikeChanged(val commentId: String, val isLiked: Boolean) : ActionEvent()
        data class CommentDeleted(val commentId: String) : ActionEvent()
        data class Error(val message: String) : ActionEvent()
    }
}
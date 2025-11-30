package com.example.myapp.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
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


    // 帖子详情 (观察本地数据库)
    private val _post = MutableLiveData<Post?>()

    // 创建一个“触发器” LiveData，用于存储当前的 postId
    private val _postId = MutableLiveData<String>()

    // 用于内部逻辑获取当前 ID (保持与 _postId 同步或直接读取 _postId.value)
    private val currentPostId: String?
        get() = _postId.value

    // 使用 switchMap 转换。每当 _postId 变化时，自动去 Repository 查新的 LiveData
    // 帖子详情 (观察本地数据库)
    val post: LiveData<Post?> = _postId.switchMap { id ->
        postRepository.getPostById(id)
    }
    // 评论列表 (观察本地数据库)
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _postId.switchMap { id ->
        commentRepository.getTopLevelComments(id)
    }

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _actionEvent = MutableLiveData<ActionEvent?>()
    val actionEvent: LiveData<ActionEvent?> = _actionEvent

    // 新增分页相关状态
    private var commentPage = 1
    private var hasMoreComments = true
    private val PAGE_SIZE = 5

    // 标记是否正在加载更多（区别于全局 isLoading）
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    /**
     * 初始化加载
     */
    fun loadPost(postId: String) {
        // 防止重复加载同一帖子
        if (_postId.value == postId) return

        // 只需要更新触发器，上面的 post 和 comments 会自动更新
        _postId.value = postId

        // 触发网络刷新
        refreshData(postId)
    }

    /**
     * 刷新帖子详情和评论（重置为第一页）
     */
    fun refreshData(postId: String) {
        // 重置分页状态
        commentPage = 1
        hasMoreComments = true

        viewModelScope.launch {
            try {
                coroutineScope {
                    val deferredPost = async { postRepository.fetchPostDetail(postId) }
                    // 调用 Repository，获取第一页数据
                    val deferredComments = async { commentRepository.refreshComments(postId, 1) }

                    val postResult = deferredPost.await()
                    val commentResult = deferredComments.await()

                    // 处理第一页结果，判断是否有更多
                    commentResult.onSuccess { list ->
                        hasMoreComments = list.size >= PAGE_SIZE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 加载更多评论
     */
    fun loadMoreComments() {
        val postId = currentPostId ?: return

        // 检查状态：如果正在加载、没有更多数据、或正在刷新，则不执行
        if (_isLoading.value == true || _isLoadingMore.value == true || !hasMoreComments) {
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true // 显示底部 Loading

            val nextPage = commentPage + 1
            val result = commentRepository.refreshComments(postId, nextPage)

            _isLoadingMore.value = false // 隐藏底部 Loading

            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    commentPage = nextPage
                    // 如果返回数量少于分页大小，说明没有更多了
                    hasMoreComments = list.size >= PAGE_SIZE
                } else {
                    hasMoreComments = false
                }
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
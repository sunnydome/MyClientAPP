package com.example.myapp.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
    private val _post = MediatorLiveData<Post?>()
    val post: LiveData<Post?> = _post

    // 创建一个“触发器” LiveData，用于存储当前的 postId
    private val _postId = MutableLiveData<String>()

    // 用于内部逻辑获取当前 ID (保持与 _postId 同步或直接读取 _postId.value)
    private val currentPostId: String?
        get() = _postId.value

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
    // 初始化监听逻辑
    init {
        // 当 postId 变化时，从数据库获取 LiveData 源
        val dbSource = _postId.switchMap { id -> postRepository.getPostById(id) }

        // 将数据库源添加到 Mediator 中
        _post.addSource(dbSource) { dbPost ->
            // 只有当内存中没有数据，或者确实需要从 DB 刷新时才更新
            // 这里简单处理：数据库有变动就更新 (初始加载会走这里)
            _post.value = dbPost
        }
    }

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

    // [核心修改] 切换点赞 - 纯内存操作
    fun toggleLike() {
        // 1. 获取当前内存中的数据
        val currentPost = _post.value ?: return

        // 2. 计算新状态 (Copy On Write)
        val newStatus = !currentPost.isLiked
        val newCount = if (newStatus) currentPost.likeCount + 1 else currentPost.likeCount - 1
        val newPost = currentPost.copy(
            isLiked = newStatus,
            likeCount = maxOf(0, newCount) // 防止减到负数
        )

        // 3. 【立即更新 UI】不经过数据库
        _post.value = newPost

        // 4. 发送网络请求 (Fire and Forget)
        viewModelScope.launch {
            postRepository.toggleLike(currentPost.id)
            // 结果不重要，因为我们已经更新了 UI
        }
    }

    // [核心修改] 切换收藏 - 纯内存操作
    fun toggleCollect() {
        val currentPost = _post.value ?: return

        val newStatus = !currentPost.isCollected
        val newCount = if (newStatus) currentPost.collectCount + 1 else currentPost.collectCount - 1
        val newPost = currentPost.copy(
            isCollected = newStatus,
            collectCount = maxOf(0, newCount)
        )

        // 立即更新 UI
        _post.value = newPost

        viewModelScope.launch {
            postRepository.toggleCollect(currentPost.id)
        }
    }

    // [新增] 切换关注 - 纯内存操作
    fun toggleFollow() {
        val currentPost = _post.value ?: return
        val newStatus = !currentPost.isFollowing
        val newPost = currentPost.copy(isFollowing = newStatus)

        _post.value = newPost

        viewModelScope.launch {
            // 这里假设 Repository 里有一个 toggleFollow 方法，逻辑同上
            // postRepository.toggleFollow(currentPost.authorId)
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
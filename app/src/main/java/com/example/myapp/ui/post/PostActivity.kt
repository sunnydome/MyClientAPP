package com.example.myapp.ui.post

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.data.model.Comment
import com.example.myapp.ui.post.recyclerCommentView.CommentAdapter
import com.example.myapp.ui.post.recyclerCommentView.CommentDividerDecoration
import com.example.myapp.ui.post.recyclerCommentView.FooterAdapter
import com.example.myapp.ui.imageviewer.ImageViewerActivity

/**
 * 帖子详情页 - 重构版
 *
 * 采用小红书/Instagram 的架构：
 * 单一 RecyclerView + ConcatAdapter，包含：
 * 1. PostHeaderAdapter - 帖子头部（导航、图片、内容）
 * 2. CommentAdapter - 评论列表
 * 3. FooterAdapter - 底部加载提示
 *
 * 优点：
 * - 避免 CoordinatorLayout + AppBarLayout 的复杂联动问题
 * - 滚动流畅，无嵌套滚动
 * - 不会有初始位置偏移的问题
 */
class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var postViewModel: PostViewModel

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var ivMineAvatar: ImageView
    private lateinit var etComment: EditText

    // Adapters
    private lateinit var headerAdapter: PostHeaderAdapter
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var footerAdapter: FooterAdapter
    private lateinit var concatAdapter: ConcatAdapter

    private var replyToComment: Comment? = null
    private var imageTransName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId.isNullOrBlank()) {
            Toast.makeText(this, "帖子不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]

        imageTransName = intent.getStringExtra("extra_trans_name_image")

        // 设置共享元素动画回调
        setupSharedElementCallback()

        // 延迟过渡动画，等待图片加载
        supportPostponeEnterTransition()

        initViews()
        setupListeners()
        observeViewModel()

        postViewModel.loadPost(postId)
    }

    private fun setupSharedElementCallback() {
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                // 获取当前显示的 ImageView
                val headerHolder = headerAdapter.getHeaderViewHolder(recyclerView)
                if (headerHolder != null && headerHolder.getCurrentPosition() != 0) {
                    // 如果不是第一张图片，移除共享元素
                    imageTransName?.let { name ->
                        names?.remove(name)
                        sharedElements?.remove(name)
                    }
                }
            }
        })
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerview_post)
        ivMineAvatar = findViewById(R.id.post_mine_avatar)
        etComment = findViewById(R.id.comment_input)

        // 初始化 HeaderAdapter
        headerAdapter = PostHeaderAdapter(
            targetTransitionName = imageTransName,
            onBackClick = { onBackPressedDispatcher.onBackPressed() },
            onFollowClick = { postViewModel.toggleLike() },
            onShareClick = { Toast.makeText(this, "分享功能开发中", Toast.LENGTH_SHORT).show() },
            onAvatarClick = { /* 跳转用户主页 */ },
            onImageClick = { position, imageView -> openImageViewer(position, imageView) },
            onFirstImageLoaded = { supportStartPostponedEnterTransition() },
            onFirstImageSizeReady = { _, _ -> /* 高度调整已在 HeaderAdapter 内部处理 */ }
        )

        // 初始化 CommentAdapter
        commentAdapter = CommentAdapter(
            onReplyClick = { comment -> onReplyComment(comment) },
            onLikeClick = { comment -> postViewModel.toggleCommentLike(comment.id) },
            onAvatarClick = { /* 跳转用户主页 */ }
        )

        // 初始化 FooterAdapter
        footerAdapter = FooterAdapter()

        // 使用 ConcatAdapter 组合所有 Adapter
        concatAdapter = ConcatAdapter(headerAdapter, commentAdapter, footerAdapter)

        // 设置 RecyclerView
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = concatAdapter

        // 添加评论分割线（只对评论项生效）
        recyclerView.addItemDecoration(
            CommentDividerDecoration.createForConcatAdapter(
                context = this,
                headerItemCount = 1  // HeaderAdapter 占 1 个位置
            )
        )

        // 滚动监听 - 加载更多
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0
                    ) {
                        postViewModel.loadMoreComments()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendComment()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        // 观察帖子数据
        postViewModel.post.observe(this) { post ->
            post?.let {
                headerAdapter.setPost(it)
            }
        }

        // 观察评论列表
        postViewModel.comments.observe(this) { comments ->
            commentAdapter.updateData(comments)
            // 有评论时显示 footer
            footerAdapter.isVisible = comments.isNotEmpty()
        }

        // 观察操作事件
        postViewModel.actionEvent.observe(this) { event ->
            when (event) {
                is PostViewModel.ActionEvent.CommentAdded -> {
                    Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
                    etComment.text.clear()
                    etComment.clearFocus()
                    hideKeyboard()
                    clearReplyState()
                }
                is PostViewModel.ActionEvent.Error -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            postViewModel.clearActionEvent()
        }

        // 观察加载更多状态
        postViewModel.isLoadingMore.observe(this) { isLoadingMore ->
            // 可以在这里更新 FooterAdapter 显示 "加载中" 或 "到底了"
            footerAdapter.isVisible = !isLoadingMore && (postViewModel.comments.value?.isNotEmpty() == true)
        }
    }

    private fun onReplyComment(comment: Comment) {
        replyToComment = comment
        etComment.hint = "回复 @${comment.authorName}"
        etComment.requestFocus()
        showKeyboard()
    }

    private fun clearReplyState() {
        replyToComment = null
        etComment.hint = "写评论..."
    }

    private fun sendComment() {
        val content = etComment.text.toString().trim()
        if (content.isBlank()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }

        postViewModel.addComment(
            content = content,
            parentId = replyToComment?.id,
            replyToName = replyToComment?.authorName
        )
    }

    private fun openImageViewer(position: Int, imageView: ImageView) {
        val post = postViewModel.post.value ?: return
        val imageUrls = post.imageUrls.ifEmpty { listOf(post.coverUrl) }

        if (imageUrls.isEmpty()) return

        val existingTransitionName = imageView.transitionName

        ImageViewerActivity.start(
            context = this,
            imageUrls = imageUrls,
            currentPosition = position,
            sharedElement = imageView,
            transitionName = existingTransitionName
        )
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etComment.windowToken, 0)
    }
}
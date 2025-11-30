package com.example.myapp.ui.post

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.data.model.Comment
import com.example.myapp.data.model.Post
import com.example.myapp.ui.post.recyclerCommentView.CommentAdapter
import com.example.myapp.ui.post.recyclerCommentView.CommentDividerDecoration
import com.example.myapp.ui.post.recyclerCommentView.FooterAdapter
import com.example.myapp.ui.imageviewer.ImageViewerActivity
import androidx.core.view.ViewCompat

/**
 * 帖子详情页
 */
class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var postViewModel: PostViewModel

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var etComment: EditText

    // [重要] 这里是新的变量，对应 xml 中的 btn_like, btn_collect, btn_comment
    // 请确保删除了旧的 layoutLike, ivLike 等变量
    private lateinit var btnLike: TextView
    private lateinit var btnCollect: TextView
    private lateinit var btnComment: TextView

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

        setupSharedElementCallback()
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
                val headerHolder = headerAdapter.getHeaderViewHolder(recyclerView)
                if (headerHolder != null && headerHolder.getCurrentPosition() != 0) {
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
        etComment = findViewById(R.id.comment_input)

        // [重要] 绑定新的 ID (对应 activity_post.xml 中的 ID)
        btnLike = findViewById(R.id.btn_like)
        btnCollect = findViewById(R.id.btn_collect)
        btnComment = findViewById(R.id.btn_comment)

        // 初始化 HeaderAdapter
        headerAdapter = PostHeaderAdapter(
            targetTransitionName = imageTransName,
            onBackClick = { onBackPressedDispatcher.onBackPressed() },
            // [重要] 使用 toggleFollow
            onFollowClick = { postViewModel.toggleFollow() },
            onShareClick = { Toast.makeText(this, "分享功能开发中", Toast.LENGTH_SHORT).show() },
            onAvatarClick = { /* 跳转用户主页 */ },
            onImageClick = { position, imageView -> openImageViewer(position, imageView) },
            onFirstImageLoaded = { supportStartPostponedEnterTransition() },
            onFirstImageSizeReady = { _, _ -> }
        )

        commentAdapter = CommentAdapter(
            onReplyClick = { comment -> onReplyComment(comment) },
            onLikeClick = { comment -> postViewModel.toggleCommentLike(comment.id) },
            onAvatarClick = { /* 跳转用户主页 */ }
        )

        footerAdapter = FooterAdapter()
        concatAdapter = ConcatAdapter(headerAdapter, commentAdapter, footerAdapter)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = concatAdapter

        recyclerView.addItemDecoration(
            CommentDividerDecoration.createForConcatAdapter(this, headerItemCount = 1)
        )

        // 滚动监听
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

        // [重要] 监听新的 TextView 按钮
        btnLike.setOnClickListener {
            postViewModel.toggleLike()
        }

        btnCollect.setOnClickListener {
            postViewModel.toggleCollect()
        }

        btnComment.setOnClickListener {
            etComment.requestFocus()
            showKeyboard()
        }
    }

    private fun observeViewModel() {
        postViewModel.post.observe(this) { post ->
            post?.let {
                headerAdapter.setPost(it)
                // [重要] 更新底部状态
                updateBottomBar(it)
            }
        }

        postViewModel.comments.observe(this) { comments ->
            commentAdapter.updateData(comments)
            footerAdapter.isVisible = comments.isNotEmpty()
        }

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

        postViewModel.isLoadingMore.observe(this) { isLoadingMore ->
            footerAdapter.isVisible = !isLoadingMore && (postViewModel.comments.value?.isNotEmpty() == true)
        }
    }

    /**
     * [重要] 更新底部栏 UI
     * TextView 的 isSelected 会自动触发图标变色
     */
    private fun updateBottomBar(post: Post) {
        // 更新点赞
        btnLike.isSelected = post.isLiked
        btnLike.text = if (post.likeCount > 0) post.likeCount.toString() else "赞"

        // 更新收藏
        btnCollect.isSelected = post.isCollected
        btnCollect.text = if (post.collectCount > 0) post.collectCount.toString() else "收藏"

        // 更新评论数
        btnComment.text = if (post.commentCount > 0) post.commentCount.toString() else "评论"
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

        val existingTransitionName = ViewCompat.getTransitionName(imageView)

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
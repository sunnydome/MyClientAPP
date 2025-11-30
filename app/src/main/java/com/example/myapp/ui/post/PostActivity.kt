package com.example.myapp.ui.post

import android.os.Bundle
import android.util.Log
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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.data.model.Comment
import com.example.myapp.ui.post.pagerView.PagerViewAdapter
import com.example.myapp.ui.post.recyclerCommentView.CommentAdapter
import com.example.myapp.ui.post.recyclerCommentView.FooterAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var postViewModel: PostViewModel

    // ========== Views (对应 activity_post.xml) ==========
    private lateinit var ivBack: ImageView
    private lateinit var ivAuthorAvatar: ImageView
    private lateinit var tvAuthorName: TextView
    private lateinit var btnFollow: TextView
    private lateinit var ivShare: ImageView

    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: PagerViewAdapter
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var tvTimeLocation: TextView
    private lateinit var tvCommentCount: TextView

    private lateinit var ivMineAvatar: ImageView
    private lateinit var etComment: EditText
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var footerAdapter: FooterAdapter

    private var replyToComment: Comment? = null

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

        val imageTransName = intent.getStringExtra("extra_trans_name_image")

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {
                if (::viewPager2.isInitialized && viewPager2.currentItem != 0) {
                    if (imageTransName != null) {
                        names?.remove(imageTransName)
                        sharedElements?.remove(imageTransName)
                    }
                }
            }
        })
        supportPostponeEnterTransition()
        initViews(imageTransName)
        setupListeners()
        observeViewModel()

        postViewModel.loadPost(postId)
    }

    private fun initViews(imageTransName: String?) {
        ivBack = findViewById(R.id.home_return)
        ivAuthorAvatar = findViewById(R.id.post_user_avatar)
        tvAuthorName = findViewById(R.id.post_user_name)
        btnFollow = findViewById(R.id.followButton)
        ivShare = findViewById(R.id.share_icon)

        val rootView = findViewById<android.view.View>(R.id.main)

        viewPager2 = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.view_pager_indicator)

        pagerAdapter = PagerViewAdapter(imageTransName) {
            supportStartPostponedEnterTransition()
        }
        viewPager2.adapter = pagerAdapter

        // [关键修改] 使用 setIcon 而不是 tabBackground
        TabLayoutMediator(tabLayout, viewPager2) { tab, _ ->
            tab.setIcon(R.drawable.indicator_selector)
        }.attach()

        tvTitle = findViewById(R.id.post_description)
        tvContent = findViewById(R.id.post_detail)
        tvTimeLocation = findViewById(R.id.post_time_position)
        tvCommentCount = findViewById(R.id.post_comment_counts)

        ivMineAvatar = findViewById(R.id.post_mine_avatar)
        etComment = findViewById(R.id.comment_input)

        commentRecyclerView = findViewById(R.id.recyclerview_comments)

        val layoutManager = LinearLayoutManager(this) // 获取 LayoutManager 引用
        commentRecyclerView.layoutManager = layoutManager

        commentAdapter = CommentAdapter(
            onReplyClick = { comment -> onReplyComment(comment) },
            onLikeClick = { comment -> postViewModel.toggleCommentLike(comment.id) },
            onAvatarClick = { /* 跳转用户主页 */ }
        )
        // 【新增】滚动监听实现分页加载
        commentRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 只有向下滑动才检查
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // 预加载阈值：倒数第 3 个 item 可见时触发加载
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0) {
                        postViewModel.loadMoreComments()
                    }
                }
            }
        })
        footerAdapter = FooterAdapter()
        val concatAdapter = ConcatAdapter(commentAdapter, footerAdapter)
        commentRecyclerView.adapter = concatAdapter
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnFollow.setOnClickListener {
            it.isSelected = !it.isSelected
            btnFollow.text = if (it.isSelected) "已关注" else "关注"
            postViewModel.toggleLike()
        }

        ivShare.setOnClickListener {
            Toast.makeText(this, "分享功能开发中", Toast.LENGTH_SHORT).show()
        }

        etComment.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendComment()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        postViewModel.post.observe(this) { post ->
            post?.let {
                val urls = post.imageUrls.ifEmpty { listOf(post.coverUrl) }
                pagerAdapter.setList(urls)

                if (urls.size > 1) {
                    tabLayout.visibility = View.VISIBLE
                } else {
                    tabLayout.visibility = View.GONE
                }

                tvTitle.text = post.title
                tvContent.text = post.content
                tvAuthorName.text = post.authorName
                tvTimeLocation.text = "编辑于 ${post.getFormattedTime()} · ${post.location.ifBlank { "未知地点" }}"
                tvCommentCount.text = "共${post.commentCount}条评论"

                Glide.with(this)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(ivAuthorAvatar)
            }
        }

        postViewModel.comments.observe(this) { comments ->
            commentAdapter.updateData(comments)
            footerAdapter.isVisible = true
        }

        postViewModel.actionEvent.observe(this) { event ->
            when (event) {
                is PostViewModel.ActionEvent.CommentAdded -> {
                    Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
                    etComment.text.clear()
                    etComment.clearFocus()
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etComment.windowToken, 0)
                    clearReplyState()
                }
                is PostViewModel.ActionEvent.Error -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            postViewModel.clearActionEvent()
            // 【新增】观察“加载更多”的状态，控制 Footer 显示
            postViewModel.isLoadingMore.observe(this) { isLoadingMore ->
                // 这里可以更新 FooterAdapter 的状态
                // 简单实现：如果正在加载，显示 Footer；否则隐藏（或者显示“到底了”）
                // 你可能需要修改 FooterAdapter 来支持“加载中”文本
                footerAdapter.isVisible = isLoadingMore
            }
        }
    }

    private fun onReplyComment(comment: Comment) {
        replyToComment = comment
        etComment.hint = "回复 @${comment.authorName}"
        etComment.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
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
}
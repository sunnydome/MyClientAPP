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

class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var postViewModel: PostViewModel

    // ========== Views (对应 activity_post.xml) ==========
    // 顶部导航
    private lateinit var ivBack: ImageView           // @+id/home_return
    private lateinit var ivAuthorAvatar: ImageView   // @+id/post_user_avatar
    private lateinit var tvAuthorName: TextView      // @+id/post_user_name
    private lateinit var btnFollow: TextView           // @+id/followButton
    private lateinit var ivShare: ImageView          // @+id/share_icon

    // 内容区域
    private lateinit var viewPager2: ViewPager2      // @+id/view_pager
    private lateinit var pagerAdapter: PagerViewAdapter
    private lateinit var tvTitle: TextView           // @+id/post_description (对应之前的 Title)
    private lateinit var tvContent: TextView         // @+id/post_detail (对应之前的 Content)
    private lateinit var tvTimeLocation: TextView    // @+id/post_time_position
    private lateinit var tvCommentCount: TextView    // @+id/post_comment_counts

    // 底部评论区
    private lateinit var ivMineAvatar: ImageView     // @+id/post_mine_avatar
    private lateinit var etComment: EditText         // @+id/comment_input
    private lateinit var commentRecyclerView: RecyclerView // @+id/recyclerview_comments
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var footerAdapter: FooterAdapter

    // 当前回复的评论
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

        // [核心修改] 设置进入/返回的共享元素回调
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {
                // 检查 ViewPager 是否已经初始化，且当前选中的不是第一张图
                if (::viewPager2.isInitialized && viewPager2.currentItem != 0) {
                    // 如果不是第一张图，说明图片对不上了
                    // 我们从共享元素名单中，把“图片”移除，只保留“卡片”
                    // 这样图片就会淡出，而不是变形缩回
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
        // 1. 顶部栏
        ivBack = findViewById(R.id.home_return)
        ivAuthorAvatar = findViewById(R.id.post_user_avatar)
        tvAuthorName = findViewById(R.id.post_user_name)
        btnFollow = findViewById(R.id.followButton)
        ivShare = findViewById(R.id.share_icon)

        // 2. 中间内容
        viewPager2 = findViewById(R.id.view_pager)
        // 确保你的 PagerViewAdapter 已经改为支持 List<String>
        // 初始化 Adapter，传入 transitionName 和 回调
        pagerAdapter = PagerViewAdapter(imageTransName) {
            // [关键步骤 7] 图片加载好了，开始动画！
            supportStartPostponedEnterTransition()
        }
        viewPager2.adapter = pagerAdapter

        tvTitle = findViewById(R.id.post_description)
        tvContent = findViewById(R.id.post_detail)
        tvTimeLocation = findViewById(R.id.post_time_position)
        tvCommentCount = findViewById(R.id.post_comment_counts)

        // 3. 底部及评论列表
        ivMineAvatar = findViewById(R.id.post_mine_avatar)
        etComment = findViewById(R.id.comment_input)

        commentRecyclerView = findViewById(R.id.recyclerview_comments)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)

        // 初始化评论适配器
        commentAdapter = CommentAdapter(
            onReplyClick = { comment -> onReplyComment(comment) },
            onLikeClick = { comment -> postViewModel.toggleCommentLike(comment.id) },
            onAvatarClick = { /* 跳转用户主页 */ }
        )
        footerAdapter = FooterAdapter()
        // 3. 使用 ConcatAdapter 连接它们
        // 顺序很重要：先显示评论，再显示Footer
        val concatAdapter = ConcatAdapter(commentAdapter, footerAdapter)
        // 4. 设置给 RecyclerView
        commentRecyclerView.adapter = concatAdapter
    }

    private fun setupListeners() {
        // 返回按钮
        // 1. 修改返回按钮的点击事件
        ivBack.setOnClickListener {
            // [关键修改] 不要调用 finish()
            // finish() 会直接杀掉页面，没有动画
            // 这相当于告诉系统：“用户触发了返回”，系统会自动处理转场动画
            onBackPressedDispatcher.onBackPressed()
        }

        // 关注按钮 (示例逻辑)
        btnFollow.setOnClickListener {
            it.isSelected = !it.isSelected
            btnFollow.text = if (it.isSelected) "已关注" else "关注"
            // TODO: 调用 ViewModel 的关注接口
        }

        // 分享按钮
        ivShare.setOnClickListener {
            Toast.makeText(this, "分享功能开发中", Toast.LENGTH_SHORT).show()
        }

        // 评论输入框：监听软键盘的“发送”动作
        etComment.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendComment()
                true // 消费事件
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        // 观察帖子数据
        postViewModel.post.observe(this) { post ->
            post?.let {
                // 图片轮播
                val urls = if (post.imageUrls.isNotEmpty()) post.imageUrls else listOf(post.coverUrl)
                pagerAdapter.setList(urls)
                pagerAdapter.notifyDataSetChanged()

                // 文本内容
                tvTitle.text = post.title
                tvContent.text = post.content
                tvAuthorName.text = post.authorName
                tvTimeLocation.text = "编辑于 ${post.getFormattedTime()} · ${post.location.ifBlank { "未知地点" }}"
                tvCommentCount.text = "共${post.commentCount}条评论"

                // 作者头像
                Glide.with(this)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(ivAuthorAvatar)

                // 观察评论列表
                postViewModel.comments.observe(this) { comments ->
                    // 更新评论列表
                    commentAdapter.updateData(comments)

                    // 更新 Footer 状态
                    // 逻辑：只要数据加载回来（哪怕是空列表），就显示“到底了”
                    // 或者你可以改为：if (comments.isNotEmpty()) 才显示
                    footerAdapter.isVisible = true
                }
                // 我的头像 (这里暂时用作者头像代替，实际应从 UserRepository 获取当前用户头像)
                // Glide.with(this).load(currentUserAvatar)...
            }
        }

        // 观察评论列表
        postViewModel.comments.observe(this) { comments ->
            commentAdapter.updateData(comments)
        }

        // 观察事件回调
        postViewModel.actionEvent.observe(this) { event ->
            when (event) {
                is PostViewModel.ActionEvent.CommentAdded -> {
                    Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
                    etComment.text.clear()
                    etComment.clearFocus()
                    // 收起键盘
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
        }
    }

    private fun onReplyComment(comment: Comment) {
        replyToComment = comment
        etComment.hint = "回复 @${comment.authorName}"
        etComment.requestFocus()
        // 弹出键盘
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
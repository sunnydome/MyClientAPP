package com.example.myapp.ui.post

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.data.model.Comment
import com.example.myapp.ui.post.pagerView.PagerViewAdapter
import com.example.myapp.ui.post.recyclerCommentView.CommentAdapter

/**
 * 帖子详情页Activity
 *
 * 更新说明：使用新的数据层架构
 */
class PostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }

    private lateinit var postViewModel: PostViewModel

    // Views
    private lateinit var viewPager2: ViewPager2
    private lateinit var pagerAdapter: PagerViewAdapter
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter

    // 可选的帖子信息Views（根据你的布局）
    private var tvTitle: TextView? = null
    private var tvContent: TextView? = null
    private var tvAuthorName: TextView? = null
    private var ivAuthorAvatar: ImageView? = null
    private var tvLikeCount: TextView? = null
    private var tvCommentCount: TextView? = null
    private var btnLike: ImageView? = null
    private var btnCollect: ImageView? = null
    private var etComment: EditText? = null
    private var btnSendComment: TextView? = null

    // 当前回复的评论（如果有的话）
    private var replyToComment: Comment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        // 获取帖子ID
        val postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId.isNullOrBlank()) {
            Toast.makeText(this, "帖子不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化ViewModel
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]

        // 初始化Views
        initViews()

        // 设置监听器
        setupListeners()

        // 观察数据
        observeViewModel()

        // 加载数据
        postViewModel.loadPost(postId)
    }

    /**
     * 初始化Views
     */
    private fun initViews() {
        // 图片轮播
        viewPager2 = findViewById(R.id.view_pager)
        pagerAdapter = PagerViewAdapter()
        viewPager2.adapter = pagerAdapter

        // 评论列表
        commentRecyclerView = findViewById(R.id.recyclerview_comments)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(
            onReplyClick = { comment ->
                onReplyComment(comment)
            },
            onLikeClick = { comment ->
                postViewModel.toggleCommentLike(comment.id)
            },
            onAvatarClick = { comment ->
                // TODO: 跳转到用户主页
            }
        )
        commentRecyclerView.adapter = commentAdapter

        // 尝试获取可选的Views（根据实际布局）
        //tvTitle = findViewById(R.id.tv_post_title)
        //tvContent = findViewById(R.id.tv_post_content)
        //tvAuthorName = findViewById(R.id.tv_author_name)
        //ivAuthorAvatar = findViewById(R.id.iv_author_avatar)
        //tvLikeCount = findViewById(R.id.tv_like_count)
        //tvCommentCount = findViewById(R.id.tv_comment_count)
        //btnLike = findViewById(R.id.btn_like)
        //btnCollect = findViewById(R.id.btn_collect)
        //etComment = findViewById(R.id.et_comment)
        //btnSendComment = findViewById(R.id.btn_send_comment)
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 点赞按钮
        btnLike?.setOnClickListener {
            postViewModel.toggleLike()
        }

        // 收藏按钮
        btnCollect?.setOnClickListener {
            postViewModel.toggleCollect()
        }

        // 发送评论按钮
        btnSendComment?.setOnClickListener {
            sendComment()
        }
    }

    /**
     * 观察ViewModel数据
     */
    private fun observeViewModel() {
        // 观察帖子详情
        postViewModel.post.observe(this) { post ->
            post?.let {
                // 更新图片轮播
                // 这里需要修改PagerViewAdapter以支持图片URL
                // 暂时使用占位数据
                val imageCount = post.imageUrls.size.coerceAtLeast(1)
                pagerAdapter.setList((0 until imageCount).toList())

                // 更新帖子信息
                tvTitle?.text = post.title
                tvContent?.text = post.content
                tvAuthorName?.text = post.authorName
                tvLikeCount?.text = formatCount(post.likeCount)
                tvCommentCount?.text = formatCount(post.commentCount)
                btnLike?.isSelected = post.isLiked
                btnCollect?.isSelected = post.isCollected

                // 加载作者头像
                ivAuthorAvatar?.let { avatar ->
                    Glide.with(this)
                        .load(post.authorAvatar)
                        .placeholder(R.drawable.avatar_placeholder)
                        .circleCrop()
                        .into(avatar)
                }
            }
        }

        // 观察评论列表
        postViewModel.comments.observe(this) { comments ->
            commentAdapter.updateData(comments)
        }

        // 观察操作事件
        postViewModel.actionEvent.observe(this) { event ->
            when (event) {
                is PostViewModel.ActionEvent.LikeChanged -> {
                    btnLike?.isSelected = event.isLiked
                    val message = if (event.isLiked) "已点赞" else "已取消点赞"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                is PostViewModel.ActionEvent.CollectChanged -> {
                    btnCollect?.isSelected = event.isCollected
                    val message = if (event.isCollected) "已收藏" else "已取消收藏"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                is PostViewModel.ActionEvent.CommentAdded -> {
                    Toast.makeText(this, "评论成功", Toast.LENGTH_SHORT).show()
                    etComment?.text?.clear()
                    clearReplyState()
                }
                is PostViewModel.ActionEvent.CommentLikeChanged -> {
                    // 评论点赞状态已通过LiveData自动更新
                }
                is PostViewModel.ActionEvent.CommentDeleted -> {
                    Toast.makeText(this, "评论已删除", Toast.LENGTH_SHORT).show()
                }
                is PostViewModel.ActionEvent.Error -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                }
                null -> { /* 忽略 */ }
            }
            postViewModel.clearActionEvent()
        }
    }

    /**
     * 回复评论
     */
    private fun onReplyComment(comment: Comment) {
        replyToComment = comment
        etComment?.hint = "回复 @${comment.authorName}"
        etComment?.requestFocus()
    }

    /**
     * 清除回复状态
     */
    private fun clearReplyState() {
        replyToComment = null
        etComment?.hint = "写评论..."
    }

    /**
     * 发送评论
     */
    private fun sendComment() {
        val content = etComment?.text?.toString()?.trim()
        if (content.isNullOrBlank()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }

        postViewModel.addComment(
            content = content,
            parentId = replyToComment?.id,
            replyToName = replyToComment?.authorName
        )
    }

    /**
     * 格式化数量显示
     */
    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> String.format("%.1fw", count / 10000.0)
            count >= 1000 -> String.format("%.1fk", count / 1000.0)
            else -> count.toString()
        }
    }
}
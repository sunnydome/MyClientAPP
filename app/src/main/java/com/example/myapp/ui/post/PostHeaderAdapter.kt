package com.example.myapp.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.data.model.Post
import com.example.myapp.ui.post.pagerView.PagerViewAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 帖子头部适配器
 *
 * 作为 RecyclerView 的第一个 item，包含：
 * - 顶部导航栏（返回、用户信息、关注、分享）
 * - 图片轮播
 * - 帖子标题、内容、时间
 * - 评论数标题
 */
class PostHeaderAdapter(
    private val targetTransitionName: String?,
    private val onBackClick: () -> Unit,
    private val onFollowClick: () -> Unit,
    private val onShareClick: () -> Unit,
    private val onAvatarClick: () -> Unit,
    private val onImageClick: ((position: Int, imageView: ImageView) -> Unit)? = null,
    private val onFirstImageLoaded: () -> Unit,
    private val onFirstImageSizeReady: ((width: Int, height: Int) -> Unit)? = null
) : RecyclerView.Adapter<PostHeaderAdapter.HeaderViewHolder>() {

    private var post: Post? = null
    private var pagerAdapter: PagerViewAdapter? = null
    private var tabLayoutMediator: TabLayoutMediator? = null

    fun setPost(post: Post) {
        this.post = post
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(post)
    }

    override fun getItemCount(): Int = 1

    override fun onViewRecycled(holder: HeaderViewHolder) {
        super.onViewRecycled(holder)
        // 清理 TabLayoutMediator 避免内存泄漏
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // 顶部导航
        private val ivBack: ImageView = itemView.findViewById(R.id.home_return)
        private val ivAuthorAvatar: ImageView = itemView.findViewById(R.id.post_user_avatar)
        private val tvAuthorName: TextView = itemView.findViewById(R.id.post_user_name)
        private val btnFollow: TextView = itemView.findViewById(R.id.followButton)
        private val ivShare: ImageView = itemView.findViewById(R.id.share_icon)

        // 图片轮播
        private val viewPager2: ViewPager2 = itemView.findViewById(R.id.view_pager)
        private val tabLayout: TabLayout = itemView.findViewById(R.id.view_pager_indicator)

        // 帖子内容
        private val tvTitle: TextView = itemView.findViewById(R.id.post_description)
        private val tvContent: TextView = itemView.findViewById(R.id.post_detail)
        private val tvTimeLocation: TextView = itemView.findViewById(R.id.post_time_position)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.post_comment_counts)

        init {
            // 设置点击事件
            ivBack.setOnClickListener { onBackClick() }
            ivShare.setOnClickListener { onShareClick() }
            ivAuthorAvatar.setOnClickListener { onAvatarClick() }

            btnFollow.setOnClickListener {
                it.isSelected = !it.isSelected
                btnFollow.text = if (it.isSelected) "已关注" else "关注"
                onFollowClick()
            }

            // 初始化 PagerAdapter
            pagerAdapter = PagerViewAdapter(
                targetTransitionName = targetTransitionName,
                onFirstImageLoaded = onFirstImageLoaded,
                onFirstImageSizeReady = { width, height ->
                    // 动态调整 ViewPager2 高度
                    adjustViewPagerHeight(width, height)
                    onFirstImageSizeReady?.invoke(width, height)
                },
                onImageClick = { position, imageView ->
                    onImageClick?.invoke(position, imageView)
                }
            )
            viewPager2.adapter = pagerAdapter
            viewPager2.offscreenPageLimit = 3
        }

        fun bind(post: Post?) {
            post ?: return

            // 设置图片列表
            val urls = post.imageUrls.ifEmpty { listOf(post.coverUrl) }
            pagerAdapter?.setList(urls)

            // 设置指示器
            setupIndicator(urls.size)

            if (viewPager2.adapter?.itemCount != urls.size) {
                setupIndicator(urls.size)
            }
            // 绑定帖子内容
            tvTitle.text = post.title
            tvContent.text = post.content
            tvAuthorName.text = post.authorName
            // 根据 post.isFollowing 的真实状态来显示 UI
            if (post.isFollowing) {
                btnFollow.text = "已关注"
                btnFollow.isSelected = true
            } else {
                btnFollow.text = "关注"
                btnFollow.isSelected = false
            }

            btnFollow.setOnClickListener {
                // 立即给用户视觉反馈 (防止网络慢的时候感觉没点上)
                // 但真实状态最终会由 LiveData 回调刷新
                val nextState = !btnFollow.isSelected
                btnFollow.isSelected = nextState
                btnFollow.text = if (nextState) "已关注" else "关注"

                // 触发回调
                onFollowClick()
            }
            tvTimeLocation.text = "编辑于 ${post.getFormattedTime()} · ${post.location.ifBlank { "未知地点" }}"
            tvCommentCount.text = "共${post.commentCount}条评论"

            // 加载作者头像
            Glide.with(itemView.context)
                .load(post.authorAvatar)
                .placeholder(R.drawable.avatar_placeholder)
                .circleCrop()
                .into(ivAuthorAvatar)
        }

        private fun setupIndicator(imageCount: Int) {
            // 先清理旧的 mediator
            tabLayoutMediator?.detach()

            if (imageCount > 1) {
                tabLayout.visibility = View.VISIBLE
                tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager2) { tab, _ ->
                    tab.setIcon(R.drawable.indicator_selector)
                }.also { it.attach() }
            } else {
                tabLayout.visibility = View.GONE
            }
        }

        /**
         * 根据图片尺寸动态调整 ViewPager2 高度
         */
        private fun adjustViewPagerHeight(imageWidth: Int, imageHeight: Int) {
            if (imageWidth <= 0 || imageHeight <= 0) return

            viewPager2.post {
                val screenWidth = viewPager2.width
                if (screenWidth <= 0) return@post

                val screenHeight = itemView.context.resources.displayMetrics.heightPixels

                // 按图片比例计算高度
                val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
                var targetHeight = (screenWidth / aspectRatio).toInt()

                // 限制最大高度（屏幕高度的 75%）
                val maxHeight = (screenHeight * 0.75f).toInt()
                // 限制最小高度（屏幕高度的 30%）
                val minHeight = (screenHeight * 0.3f).toInt()

                targetHeight = targetHeight.coerceIn(minHeight, maxHeight)

                val layoutParams = viewPager2.layoutParams
                if (layoutParams.height != targetHeight) {
                    layoutParams.height = targetHeight
                    viewPager2.layoutParams = layoutParams
                }
            }
        }

        /**
         * 获取当前显示的 ImageView（用于共享元素动画）
         */
        fun getCurrentImageView(): ImageView? {
            return pagerAdapter?.getImageViewAt(viewPager2.currentItem)
        }

        /**
         * 获取当前 ViewPager2 的位置
         */
        fun getCurrentPosition(): Int = viewPager2.currentItem
    }

    /**
     * 获取 HeaderViewHolder（用于获取 ImageView）
     */
    fun getHeaderViewHolder(recyclerView: RecyclerView): HeaderViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(0) as? HeaderViewHolder
    }
}
package com.example.myapp.ui.home.recyclerPostView

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R
import com.example.myapp.data.model.FeedItem
import com.example.myapp.ui.post.PostActivity

/**
 * Feed列表适配器
 * 使用ListAdapter + DiffUtil实现高效更新
 *
 * 更新说明：使用新的FeedItem数据模型
 */
class FeedAdapter(
    private val onItemClick: ((FeedItem, View) -> Unit)? = null,
    private val onLikeClick: ((FeedItem) -> Unit)? = null
) : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    /**
     * 更新数据（兼容旧接口）
     */
    fun updateData(newFeeds: List<FeedItem>) {
        submitList(newFeeds)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return FeedViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.item_image)
        private val titleText: TextView = itemView.findViewById(R.id.item_title)
        private val userNameText: TextView = itemView.findViewById(R.id.item_user_name)
        private val userAvatar: ImageView = itemView.findViewById(R.id.item_user_avatar)
        private val likeButton: ImageView? = itemView.findViewById(R.id.item_like_button)
        private val likeCount: TextView? = itemView.findViewById(R.id.item_like_count)

        fun bind(feed: FeedItem) {
            // 动态设置图片的高度比例 (实现瀑布流的核心)
            val layoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams

            // 设置宽高比。DimensionRatio 格式通常为 "width:height" 或者直接是一个浮点数代表 w/h
            // 假设 feed.coverAspectRatio 是 width/height (例如 0.75)
            layoutParams.dimensionRatio = String.format("%f:1", feed.coverAspectRatio)
            imageView.layoutParams = layoutParams

            // 加载封面图
            Glide.with(itemView.context)
                .load(feed.coverUrl)
                .placeholder(R.drawable.placeholder_image) // 建议使用纯色背景作为占位图，体验更好
                .error(R.drawable.error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)

            // 绑定标题/描述
            titleText.text = feed.title

            // 绑定用户名
            userNameText.text = feed.authorName

            // 加载用户头像
            Glide.with(itemView.context)
                .load(feed.authorAvatar)
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .circleCrop()
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(userAvatar)

            // 点赞按钮状态
            likeButton?.let { button ->
                button.isSelected = feed.isLiked
                button.setOnClickListener {
                    // 立即更新 UI 状态
                    button.isSelected = !button.isSelected

                    // 立即更新数字 (假装成功)
                    likeCount?.let { tv ->
                        val currentStr = tv.text.toString()
                        // 这里做个简单处理：如果是纯数字才 +/- 1
                        val count = currentStr.toIntOrNull()
                        if (count != null) {
                            val newCount = if (button.isSelected) count + 1 else maxOf(0, count - 1)
                            tv.text = newCount.toString()
                        }
                    }

                    // 触发回调 (去 ViewModel 发请求 & 改内存数据)
                    onLikeClick?.invoke(feed)
                }
            }

            // 点赞数量（如果有的话）
            likeCount?.text = formatCount(feed.likeCount)

            // 必须保证这个名字在当前界面是唯一的，通常用 "image_" + id
            val transitionName = "trans_image_${feed.id}"
            ViewCompat.setTransitionName(imageView, transitionName)

            // 点击事件 - 跳转到详情页
            itemView.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick.invoke(feed, imageView)
                } else {
                    // 默认行为：跳转到帖子详情页
                    val context = itemView.context
                    val intent = Intent(context, PostActivity::class.java).apply {
                        putExtra(PostActivity.EXTRA_POST_ID, feed.id)
                    }
                    context.startActivity(intent)
                }
            }
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
}

/**
 * DiffUtil回调
 */
// 告诉适配器旧列表和新列表之间到底发生了什么变化
class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}
package com.example.myapp.ui.post.recyclerCommentView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R
import com.example.myapp.data.model.Comment

/**
 * 评论列表适配器
 * 使用ListAdapter + DiffUtil实现高效更新
 *
 * 更新说明：使用新的Comment数据模型
 */
class CommentAdapter(
    private val onReplyClick: ((Comment) -> Unit)? = null,
    private val onLikeClick: ((Comment) -> Unit)? = null,
    private val onAvatarClick: ((Comment) -> Unit)? = null
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    /**
     * 更新数据（兼容旧接口）
     */
    fun updateData(newComments: List<Comment>) {
        submitList(newComments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val userAvatar: ImageView = itemView.findViewById(R.id.comment_user_avatar)
        private val userName: TextView = itemView.findViewById(R.id.comment_user_name)
        private val content: TextView = itemView.findViewById(R.id.comment_content)
        private val commentTime: TextView = itemView.findViewById(R.id.comment_time)
        // 可选的点赞和回复按钮（如果布局中有的话）
        //private val likeButton: ImageView? = itemView.findViewById(R.id.comment_like_button)
        //private val likeCount: TextView? = itemView.findViewById(R.id.comment_like_count)
        //private val replyButton: TextView? = itemView.findViewById(R.id.comment_reply_button)
        //private val replyCount: TextView? = itemView.findViewById(R.id.comment_reply_count)

        fun bind(comment: Comment) {
            // 加载用户头像
            Glide.with(itemView.context)
                .load(comment.authorAvatar)
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .circleCrop()
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(userAvatar)

            // 绑定用户名
            userName.text = comment.authorName

            // 绑定评论内容
            // 如果是回复，显示@被回复者
            content.text = if (comment.replyToName != null) {
                "回复 @${comment.replyToName}：${comment.content}"
            } else {
                comment.content
            }

            // 绑定时间
            commentTime.text = comment.getFormattedTime()
            /*
            // 点赞按钮状态
            likeButton?.let { button ->
                button.isSelected = comment.isLiked
                button.setOnClickListener {
                    onLikeClick?.invoke(comment)
                }
            }

            // 点赞数量
            likeCount?.text = if (comment.likeCount > 0) {
                formatCount(comment.likeCount)
            } else {
                ""
            }

            // 回复按钮
            replyButton?.setOnClickListener {
                onReplyClick?.invoke(comment)
            }

            // 回复数量（仅一级评论显示）
            replyCount?.let { tv ->
                if (comment.isTopLevel() && comment.replyCount > 0) {
                    tv.visibility = View.VISIBLE
                    tv.text = "共${comment.replyCount}条回复"
                } else {
                    tv.visibility = View.GONE
                }
            }*/

            // 头像点击
            userAvatar.setOnClickListener {
                onAvatarClick?.invoke(comment)
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
class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem == newItem
    }
}
package com.example.myapp.ui.post.recyclerCommentView

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R
import com.example.myapp.ui.post.CommentModel

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)  {
    // 声明视图组件
    private val userAvatar: ImageView = itemView.findViewById(R.id.comment_user_avatar)
    private val userName: TextView = itemView.findViewById(R.id.comment_user_name)
    private val content: TextView = itemView.findViewById(R.id.comment_content)
    private val commentTime: TextView = itemView.findViewById(R.id.comment_time)


    // 绑定数据到视图
    fun bind(comment : CommentModel) {

        // 绑定用户名
        userName.text = comment.userName

        // 绑定时间
        commentTime.text = comment.time

        // 绑定评论内容
        content.text = comment.content

        // 加载用户头像（圆形头像）
        Glide.with(itemView.context)
            .load(comment.userAvatar)
            .placeholder(R.drawable.avatar_placeholder)  // 占位头像
            .error(R.drawable.avatar_placeholder)       // 错误时占位头像
            .circleCrop()                               // 圆形裁剪头像
            .dontAnimate()                               // 禁用动画
            .diskCacheStrategy(DiskCacheStrategy.ALL)    // 缓存策略
            .into(userAvatar)
    }
}
package com.example.myapp.ui.home.RecyclerPostView

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R
import com.example.myapp.ui.home.FeedModel

class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    // 声明视图组件
    private val imageView: ImageView = itemView.findViewById(R.id.item_image)
    private val descriptionText: TextView = itemView.findViewById(R.id.item_title)
    private val userNameText: TextView = itemView.findViewById(R.id.item_user_name)
    private val userAvatar: ImageView = itemView.findViewById(R.id.item_user_avatar)

    // 绑定数据到视图
    fun bind(feed: FeedModel) {
        // 加载封面图（大图）
        Glide.with(itemView.context)
            .load(feed.imageUrl)
            .placeholder(R.drawable.placeholder_image)  // 加载中占位图
            .error(R.drawable.error_image)              // 加载失败占位图
            .dontAnimate()                              // 防止图片加载时的动画效果
            .diskCacheStrategy(DiskCacheStrategy.ALL)   // 缓存策略：缓存所有版本
            .into(imageView)

        // 绑定描述文本
        descriptionText.text = feed.description

        // 绑定用户名
        userNameText.text = feed.userName

        // 加载用户头像（圆形头像）
        Glide.with(itemView.context)
            .load(feed.userAvatar)
            .placeholder(R.drawable.avatar_placeholder)  // 占位头像
            .error(R.drawable.avatar_placeholder)       // 错误时占位头像
            .circleCrop()                               // 圆形裁剪头像
            .dontAnimate()                               // 禁用动画
            .diskCacheStrategy(DiskCacheStrategy.ALL)    // 缓存策略
            .into(userAvatar)
    }
}
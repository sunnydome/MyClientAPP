package com.example.myapp.ui.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R
class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageView: ImageView = itemView.findViewById(R.id.item_image)
    private val descriptionText: TextView = itemView.findViewById(R.id.item_title)
    private val userNameText: TextView = itemView.findViewById(R.id.item_user_name)
    private val userAvatar: ImageView = itemView.findViewById(R.id.item_user_avatar)

    fun bind(feed: FeedModel) {
        // 封面图加载
        Glide.with(itemView.context)
            .load(feed.imageUrl)
            .placeholder(R.drawable.placeholder_image)   // 加载中显示
            .error(R.drawable.error_image)               // 加载失败显示
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)


        descriptionText.text = feed.description
        userNameText.text = feed.userName

        // 头像加载
        Glide.with(itemView.context)
            .load(feed.userAvatar)
            .placeholder(R.drawable.avatar_placeholder)  // 灰色头像
            .error(R.drawable.avatar_placeholder)
            .circleCrop()                                // 头像自动圆形（可选）
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(userAvatar)
    }
}
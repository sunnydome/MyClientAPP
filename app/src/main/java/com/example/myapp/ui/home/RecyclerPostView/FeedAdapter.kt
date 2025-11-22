package com.example.myapp.ui.home.RecyclerPostView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.ui.home.FeedModel

class FeedAdapter(private var feeds: List<FeedModel>) : RecyclerView.Adapter<FeedViewHolder>() {

    // 更新数据
    fun updateData(newFeeds: List<FeedModel>) {
        feeds = newFeeds
        notifyDataSetChanged() // 数据更新后刷新 RecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return FeedViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(feeds[position])
    }

    override fun getItemCount(): Int {
        return feeds.size
    }
}
package com.example.myapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

class FeedAdapter(private val feeds: List<FeedModel>) : RecyclerView.Adapter<FeedViewHolder>() {

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
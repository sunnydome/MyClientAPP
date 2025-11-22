package com.example.myapp.ui.post.recyclerCommentView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.ui.post.CommentModel

class CommentAdapter(private var comments: List<CommentModel>) : RecyclerView.Adapter<CommentViewHolder>() {

    // 更新数据
    fun updateData(newComments: List<CommentModel>) {
        comments = newComments
        notifyDataSetChanged() // 数据更新后刷新 RecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}
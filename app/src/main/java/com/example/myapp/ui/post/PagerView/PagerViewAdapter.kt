package com.example.myapp.ui.post.PagerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.ui.post.PagerView.PagerViewHolder

class PagerViewAdapter : RecyclerView.Adapter<PagerViewHolder>() {
    private var mList: List<Int> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_picture, parent, false)
        return PagerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bindData(mList[position])
    }

    fun setList(list: List<Int>) {
        mList = list
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}

class CommentModel {

}
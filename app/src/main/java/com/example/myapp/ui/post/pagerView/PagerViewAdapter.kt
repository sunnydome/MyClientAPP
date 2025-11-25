package com.example.myapp.ui.post.pagerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

class PagerViewAdapter(
    private val targetTransitionName: String?, // 接收传进来的 TransitionName
    private val onFirstImageLoaded: () -> Unit // 回调 Activity
) : RecyclerView.Adapter<PagerViewHolder>() {
    // 修改泛型为 String
    private var mList: List<String> = ArrayList()
    private var hasNotifyLoaded = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_picture, parent, false)
        return PagerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        // 只有第一张图（position 0）才作为共享元素
        val transName = if (position == 0) targetTransitionName else null

        holder.bindData(mList[position], transName) {
            // 只有第一张图加载完才通知
            if (position == 0 && !hasNotifyLoaded) {
                hasNotifyLoaded = true
                onFirstImageLoaded()
            }
        }
    }

    // 接收 String 列表
    fun setList(list: List<String>) {
        mList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}

class CommentModel {

}
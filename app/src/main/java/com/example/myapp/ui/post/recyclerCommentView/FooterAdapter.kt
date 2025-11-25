package com.example.myapp.ui.post.recyclerCommentView

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 简单的页脚适配器，用于展示“到底了”
 */
class FooterAdapter : RecyclerView.Adapter<FooterAdapter.FooterViewHolder>() {

    // 控制是否显示Footer（例如没有评论时不显示）
    var isVisible = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        // 直接创建一个 TextView，也可以 inflate 一个 layout xml
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "- 已经到底了 -"
            textSize = 12f
            setTextColor(0xFF999999.toInt()) // 灰色
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40) // 上下留点间距
        }
        return FooterViewHolder(textView)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        // 不需要绑定数据
    }

    override fun getItemCount(): Int = if (isVisible) 1 else 0

    class FooterViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)
}
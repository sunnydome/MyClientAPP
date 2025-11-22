package com.example.myapp.ui.post.pagerView

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val mTextView: TextView = itemView.findViewById(R.id.tv_text)
    private var colors = arrayOf("#CCFF99","#41F1E5","#8D41F1","#FF99CC")

    fun bindData(i: Int) {
        mTextView.text = i.toString()
        mTextView.setBackgroundColor(Color.parseColor(colors[i]))
    }
}
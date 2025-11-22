package com.example.myapp.ui.post

import android.os.Bundle
import com.example.myapp.R
import androidx.activity.ComponentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.ui.post.PagerView.PagerViewAdapter

class PostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        val viewPager2 = findViewById<ViewPager2>(R.id.view_pager)
        val myAdapter = PagerViewAdapter()
        val data: List<Int> = listOf(0, 1, 2, 3)
        myAdapter.setList(data)
        viewPager2.adapter = myAdapter
    }
}
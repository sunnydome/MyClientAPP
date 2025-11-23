package com.example.myapp.ui.home

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapp.R
import com.example.myapp.ui.home.recyclerPostView.FeedAdapter

class HomeActivity : ComponentActivity() {
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var homeViewModel: HomeViewModel

    private lateinit var homeConcerned: TextView

    private lateinit var homeRecommend: TextView

    private lateinit var homeCity: TextView

    private lateinit var underline : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 初始化 RecyclerView
        feedRecyclerView = findViewById(R.id.recyclerview_feed)
        feedRecyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        // 初始化 ViewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 初始化TextView
        homeConcerned = findViewById(R.id.home_concerned)
        homeRecommend = findViewById(R.id.home_recommend)
        homeCity = findViewById(R.id.home_city)

        underline = findViewById<View>(R.id.underline_top)
        // 设置点击事件
        homeConcerned.setOnClickListener { onTabSelected("关注") }
        homeRecommend.setOnClickListener { onTabSelected("发现") }
        homeCity.setOnClickListener { onTabSelected("同城") }

        // 观察 LiveData 数据
        homeViewModel.getFeeds().observe(this, Observer { feedList ->
            feedAdapter = FeedAdapter(feedList)
            feedRecyclerView.adapter = feedAdapter
        })
        onTabSelected("发现")
    }

    private fun onTabSelected(category : String) {
        resetTabs()
        underline.visibility = View.VISIBLE
        val layoutParams = underline.layoutParams as ConstraintLayout.LayoutParams
        when (category) {
            "关注" -> {
                homeConcerned.setTextColor(ContextCompat.getColor(this,R.color.selected_color))
                layoutParams.startToStart = homeConcerned.id
            }
            "发现" -> {
                homeRecommend.setTextColor(ContextCompat.getColor(this,R.color.selected_color))
                layoutParams.startToStart = homeRecommend.id
            }
            "同城" -> {
                homeCity.setTextColor(ContextCompat.getColor(this,R.color.selected_color))
                layoutParams.startToStart = homeCity.id
            }
        }
        underline.layoutParams = layoutParams
        // 根据选中的 tab 加载不同的数据
        homeViewModel.loadDataForTab(category)
    }

    private fun resetTabs() {

        underline.visibility = View.GONE
        // 重置Tab样式
        homeConcerned.setTextColor(ContextCompat.getColor(this, R.color.default_color))

        homeRecommend.setTextColor(ContextCompat.getColor(this, R.color.default_color))

        homeCity.setTextColor(ContextCompat.getColor(this, R.color.default_color))
    }
}
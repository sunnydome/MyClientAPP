package com.example.myapp.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapp.R

class HomeActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recyclerview_id)
        recyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        // 初始化 ViewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 观察 LiveData 数据
        homeViewModel.getFeeds().observe(this, Observer { feedList ->
            feedAdapter = FeedAdapter(feedList)
            recyclerView.adapter = feedAdapter
        })
    }
}
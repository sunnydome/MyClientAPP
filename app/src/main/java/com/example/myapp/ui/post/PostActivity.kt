package com.example.myapp.ui.post

import android.os.Bundle
import android.util.Log
import com.example.myapp.R
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.ui.post.pagerView.PagerViewAdapter
import com.example.myapp.ui.post.recyclerCommentView.CommentAdapter

class PostActivity : ComponentActivity() {

    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var postViewModel: PostViewModel
    private lateinit var viewPager2 : ViewPager2
    private lateinit var pagerAdapter: PagerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        viewPager2 = findViewById<ViewPager2>(R.id.view_pager)
        pagerAdapter = PagerViewAdapter()
        val data: List<Int> = listOf(0, 1, 2, 3)
        pagerAdapter.setList(data)
        viewPager2.adapter = pagerAdapter

        commentRecyclerView = findViewById<RecyclerView>(R.id.recyclerview_comments)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)

        // 初始化 ViewModel
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]

        commentAdapter = CommentAdapter(emptyList())
        commentRecyclerView.adapter = commentAdapter
        // 观察 LiveData 数据
        postViewModel.getComments().observe(this, Observer { comments ->
            Log.d("PostActivity", "Comments: $comments")
            commentAdapter.updateData(comments)
        })
        postViewModel.loadData()
    }
}
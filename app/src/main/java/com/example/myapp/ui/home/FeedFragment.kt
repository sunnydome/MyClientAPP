package com.example.myapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapp.R
import com.example.myapp.ui.home.recyclerPostView.FeedAdapter

/**
 * Feed Fragment - 展示特定类别的Feed列表
 * @param category 类别名称（关注、发现、同城）
 */
class FeedFragment : Fragment() {

    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var homeViewModel: HomeViewModel
    private var category: String = ""

    companion object {
        private const val ARG_CATEGORY = "category"

        /**
         * 创建Fragment实例的工厂方法
         * @param category 类别名称
         */
        fun newInstance(category: String): FeedFragment {
            val fragment = FeedFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY, category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从arguments中获取类别
        category = arguments?.getString(ARG_CATEGORY) ?: "发现"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化RecyclerView
        feedRecyclerView = view.findViewById(R.id.recyclerview_feed)
        feedRecyclerView.layoutManager = StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL
        )

        // 获取Activity级别的ViewModel（多个Fragment共享）
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        // 初始化Adapter
        feedAdapter = FeedAdapter(emptyList())
        feedRecyclerView.adapter = feedAdapter

        // 观察对应类别的数据
        observeData()
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeData() {
        homeViewModel.getFeedsByCategory(category).observe(viewLifecycleOwner) { feedList ->
            feedAdapter.updateData(feedList)
        }
    }

    /**
     * Fragment可见时加载数据
     */
    override fun onResume() {
        super.onResume()
        // 加载该类别的数据
        homeViewModel.loadDataForTab(category)
    }
}
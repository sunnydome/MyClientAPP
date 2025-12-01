package com.example.myapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapp.R
import com.example.myapp.ui.home.recyclerPostView.FeedAdapter
import com.example.myapp.ui.post.PostActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var category: String = ""

    companion object {
        private const val ARG_CATEGORY = "category"

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
        category = arguments?.getString(ARG_CATEGORY) ?: "发现"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 ViewModel
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        // 初始化 SwipeRefreshLayout (下拉刷新)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        // 设置刷新圆圈的颜色
        swipeRefreshLayout.setColorSchemeResources(com.google.android.material.R.color.design_default_color_primary)

        swipeRefreshLayout.setOnRefreshListener {
            // 触发下拉刷新逻辑
            homeViewModel.refresh(category)
        }

        // 初始化 RecyclerView
        feedRecyclerView = view.findViewById(R.id.recyclerview_feed)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        feedRecyclerView.layoutManager = layoutManager

        feedAdapter = FeedAdapter(
            onItemClick = { feedItem, sharedImageView ->
                // 把数据存入 Intent 内部的 Bundle
                val intent = Intent(requireContext(), PostActivity::class.java).apply {
                    putExtra(PostActivity.EXTRA_POST_ID, feedItem.id)
                    putExtra("extra_trans_name_image", ViewCompat.getTransitionName(sharedImageView))
                }
                // 实现共享资源转场
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair.create(sharedImageView, ViewCompat.getTransitionName(sharedImageView))
                )
                startActivity(intent, options.toBundle())
            },
            onLikeClick = { feedItem ->
                homeViewModel.toggleLike(feedItem.id)
            }
        )
        feedRecyclerView.adapter = feedAdapter

        // 监听数据插入，自动滚动到顶部
        feedAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // 如果是在第一个位置插入了数据（发布新帖或下拉刷新）
                if (positionStart == 0) {
                    // 强制滚动到顶部，让新帖子立即可见
                    feedRecyclerView.scrollToPosition(0)
                }
            }
        })

        // 监听滚动实现“自动加载更多”
        feedRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 只有向下滑动时才检查 (dy > 0)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    // StaggeredGrid 需要获取一个数组，因为瀑布流底部可能不平齐
                    val firstVisibleItems = layoutManager.findFirstVisibleItemPositions(null)

                    if (firstVisibleItems != null && firstVisibleItems.isNotEmpty()) {
                        val firstVisibleItemPosition = firstVisibleItems[0]

                        // 简单的触发阈值：如果 (可见数量 + 第一个可见的位置) >= 总数量，说明到底了
                        // 通常预留几个 item (例如 4 个) 提前加载，体验更好
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                            // 检查 ViewModel 是否正在加载，防止重复触发
                            if (homeViewModel.getLoadingState(category).value == false) {
                                homeViewModel.loadMore(category)
                            }
                        }
                    }
                }
            }
        })

        // 观察数据
        observeData()

        // 初始加载
        homeViewModel.loadDataForTab(category)
    }

    private fun observeData() {
        // 观察 Feed 列表
        homeViewModel.getFeedsByCategory(category).observe(viewLifecycleOwner) { feedList ->
            feedAdapter.updateData(feedList)

            // 数据回来后，如果列表为空显示空视图，否则显示列表
            val emptyView = view?.findViewById<View>(R.id.empty_view)
            if (feedList.isEmpty()) {
                emptyView?.visibility = View.VISIBLE
                feedRecyclerView.visibility = View.GONE
            } else {
                emptyView?.visibility = View.GONE
                feedRecyclerView.visibility = View.VISIBLE
            }
        }

        // 观察加载状态 -> 控制刷新动画的显示与隐藏
        homeViewModel.getLoadingState(category).observe(viewLifecycleOwner) { isLoading ->
            // 不仅处理结束，也要处理开始
            if (isLoading) {
                // 如果正在后台自动刷新，且用户没有在手势拖拽，则显示刷新球
                if (!swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = true
                }
            } else {
                // loading 结束，收起下拉刷新的圆圈
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // 观察错误信息
        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // 出错也要停止动画
                homeViewModel.clearError()
            }
        }
    }
}
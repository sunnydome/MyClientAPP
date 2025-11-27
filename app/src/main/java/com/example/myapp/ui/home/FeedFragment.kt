package com.example.myapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.myapp.R
import com.example.myapp.ui.home.recyclerPostView.FeedAdapter
import com.example.myapp.ui.post.PostActivity
import androidx.core.util.Pair
import androidx.fragment.app.viewModels

/**
 * Feed Fragment - 展示特定类别的Feed列表
 * @param category 类别名称（关注、发现、同城）
 *
 * 更新说明：适配新的FeedAdapter和数据层
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

        // 初始化Adapter（新版本不需要传入初始数据）
        feedAdapter = FeedAdapter(
            onItemClick = { feedItem, sharedCardView, sharedImageView ->
                val intent = Intent(requireContext(), PostActivity::class.java).apply {
                    putExtra(com.example.myapp.ui.post.PostActivity.EXTRA_POST_ID, feedItem.id)

                    // 传递两个 TransitionName 到详情页
                    putExtra("extra_trans_name_image", ViewCompat.getTransitionName(sharedImageView))
                    putExtra("extra_trans_name_card", ViewCompat.getTransitionName(sharedCardView))
                }

                // [关键修改] 创建包含两个共享元素的动画选项
                // Pair.create(View, TransitionName)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair.create(sharedCardView, ViewCompat.getTransitionName(sharedCardView)),
                    Pair.create(sharedImageView, ViewCompat.getTransitionName(sharedImageView))
                )

                startActivity(intent, options.toBundle())
            },
            onLikeClick = { feedItem ->
                // --- 修改开始 ---
                // 调用 ViewModel 的方法，传入帖子 ID
                homeViewModel.toggleLike(feedItem.id)
                // --- 修改结束 ---
            }
        )
        feedRecyclerView.adapter = feedAdapter

        // 观察对应类别的数据
        observeData()

        // 触发加载数据
        homeViewModel.loadDataForTab(category)
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeData() {
        homeViewModel.getFeedsByCategory(category).observe(viewLifecycleOwner) { feedList ->
            feedAdapter.updateData(feedList)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
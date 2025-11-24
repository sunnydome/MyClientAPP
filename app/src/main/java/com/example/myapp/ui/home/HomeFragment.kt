package com.example.myapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 首页Fragment - 包含Tab切换（关注、发现、同城）
 */
class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: HomePagerAdapter
    private lateinit var homeViewModel: HomeViewModel

    // Tab类别列表
    private val categories = listOf("关注", "发现", "同城")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 初始化ViewPager2
        initViewPager(view)

        // 初始化TabLayout
        initTabLayout(view)

        // 设置默认选中"发现"Tab
        setDefaultTab()
    }

    /**
     * 初始化ViewPager2
     */
    private fun initViewPager(view: View) {
        viewPager = view.findViewById(R.id.view_pager)

        // 注意：这里使用 childFragmentManager，而不是 requireActivity().supportFragmentManager
        pagerAdapter = HomePagerAdapter(this, categories)
        viewPager.adapter = pagerAdapter

        // 修改点：设置为 Tab 总数 - 1，或者直接设为 2
        // 这样“关注”、“发现”、“同城”三个页面都会一直保存在内存中，不会销毁 View
        viewPager.offscreenPageLimit = categories.size

        // 监听页面切换
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val category = categories[position]
                onTabSelected(category, position)
            }
        })
    }

    /**
     * 初始化TabLayout并与ViewPager2关联
     */
    private fun initTabLayout(view: View) {
        tabLayout = view.findViewById(R.id.tab_layout)

        // 使用TabLayoutMediator将TabLayout与ViewPager2关联
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = categories[position]
        }.attach()
    }

    /**
     * 设置默认选中的Tab
     */
    private fun setDefaultTab() {
        val defaultPosition = categories.indexOf("发现")
        if (defaultPosition != -1) {
            viewPager.setCurrentItem(defaultPosition, false)
        }
    }

    /**
     * Tab选中时的回调
     */
    private fun onTabSelected(category: String, position: Int) {
        // 可以在这里添加统计、日志等
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
    }
}
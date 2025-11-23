package com.example.myapp.ui.home

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.appcompat.app.AppCompatActivity
/**
 * 首页Activity - 使用ViewPager2 + TabLayout实现Tab切换
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: HomePagerAdapter
    private lateinit var homeViewModel: HomeViewModel

    // Tab类别列表
    private val categories = listOf("关注", "发现", "同城")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 初始化ViewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 初始化ViewPager2
        initViewPager()

        // 初始化TabLayout
        initTabLayout()

        // 设置默认选中"发现"Tab
        setDefaultTab()
    }

    /**
     * 初始化ViewPager2
     */
    private fun initViewPager() {
        viewPager = findViewById(R.id.view_pager)
        pagerAdapter = HomePagerAdapter(this, categories)
        viewPager.adapter = pagerAdapter

        // 设置ViewPager预加载页面数量（可选）
        viewPager.offscreenPageLimit = 1

        // 监听页面切换
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 页面切换时可以做一些操作，比如统计、预加载等
                val category = categories[position]
                onTabSelected(category, position)
            }
        })
    }

    /**
     * 初始化TabLayout并与ViewPager2关联
     */
    private fun initTabLayout() {
        tabLayout = findViewById(R.id.tab_layout)

        // 使用TabLayoutMediator将TabLayout与ViewPager2关联
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // 设置Tab的文字
            tab.text = categories[position]
        }.attach()
    }

    /**
     * 设置默认选中的Tab
     */
    private fun setDefaultTab() {
        // 默认选中"发现"（索引1）
        val defaultPosition = categories.indexOf("发现")
        if (defaultPosition != -1) {
            viewPager.setCurrentItem(defaultPosition, false) // false表示不平滑滚动
        }
    }

    /**
     * Tab选中时的回调
     * @param category 类别名称
     * @param position Tab位置
     */
    private fun onTabSelected(category: String, position: Int) {
        // 可以在这里添加统计、日志等
        // 例如：Analytics.trackTabSelected(category)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        viewPager.adapter = null
    }
}
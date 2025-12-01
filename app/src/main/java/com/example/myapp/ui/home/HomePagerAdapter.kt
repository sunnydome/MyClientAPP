package com.example.myapp.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 的 Adapter
 * 用于管理不同Tab对应的Fragment
 * 接收 Fragment 作为参数
 * 因为 HomeFragment 内部需要嵌套 ViewPager2
 */
class HomePagerAdapter(
    fragment: Fragment,  // 接收 Fragment
    private val categories: List<String>
) : FragmentStateAdapter(fragment) {  // ← 传入 Fragment

    /**
     * 返回Fragment的数量
     */
    override fun getItemCount(): Int = categories.size

    /**
     * 根据位置创建对应的Fragment
     * @param position 位置索引
     * @return 对应类别的FeedFragment
     */
    override fun createFragment(position: Int): Fragment {
        return FeedFragment.newInstance(categories[position])
    }

    /**
     * 获取指定位置的类别名称
     * @param position 位置索引
     * @return 类别名称
     */
    fun getCategoryAtPosition(position: Int): String {
        return categories[position]
    }
}
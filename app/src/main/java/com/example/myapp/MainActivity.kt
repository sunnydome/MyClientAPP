package com.example.myapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import com.example.myapp.ui.home.HomeFragment
import com.example.myapp.ui.market.MarketFragment
import com.example.myapp.ui.message.MessageFragment
import com.example.myapp.ui.profile.ProfileFragment
import com.example.myapp.ui.publish.PublishActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 主Activity - 应用容器
 * 管理底部导航和顶级Fragment
 */
class MainActivity : FragmentActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var fab : FloatingActionButton

    // 缓存Fragment实例，避免重复创建
    private val fragmentMap = mutableMapOf<Int, Fragment>()

    // 当前显示的Fragment的ID
    private var currentFragmentId = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 恢复状态
        savedInstanceState?.let {
            currentFragmentId = it.getInt(KEY_CURRENT_FRAGMENT, R.id.nav_home)
        }

        // 初始化底部导航
        initBottomNavigation()

        // 设置返回键处理
        setupBackPressedHandler()

        // 如果是首次创建，显示首页
        if (savedInstanceState == null) {
            showFragment(R.id.nav_home)
        } else {
            // 恢复之前显示的Fragment（此时Fragment已经通过FragmentManager自动恢复）
            // 只需要确保正确的Fragment是可见的
            showFragment(currentFragmentId)
        }
    }

    /**
     * 初始化底部导航栏
     */
    private fun initBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fab = findViewById(R.id.fab_publish)
        // 设置导航项选中监听
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(R.id.nav_home)
                    true
                }
                R.id.nav_market -> {
                    showFragment(R.id.nav_market)
                    true
                }
                R.id.nav_message -> {
                    showFragment(R.id.nav_message)
                    true
                }
                R.id.nav_profile -> {
                    showFragment(R.id.nav_profile)
                    true
                }
                else -> false
            }
        }
        fab.setOnClickListener{
            startPublishActivity()
        }
        // 设置默认选中项
        bottomNavigationView.selectedItemId = currentFragmentId
    }

    /**
     * 设置返回键处理（使用新的OnBackPressedDispatcher）
     */
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 如果不在首页，返回首页
                if (currentFragmentId != R.id.nav_home) {
                    bottomNavigationView.selectedItemId = R.id.nav_home
                } else {
                    // 在首页，退出应用
                    isEnabled = false // 禁用此回调
                    onBackPressedDispatcher.onBackPressed() // 调用系统默认行为
                }
            }
        })
    }

    /**
     * 显示指定的Fragment（使用 show/hide 方式）
     * @param itemId 底部导航项ID
     */
    private fun showFragment(itemId: Int) {
        val targetFragment = getOrCreateFragment(itemId)
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // 隐藏当前所有Fragment
        fragmentMap.values.forEach { fragment ->
            if (fragment.isAdded) {
                transaction.hide(fragment)
            }
        }

        // 显示或添加目标Fragment
        if (targetFragment.isAdded) {
            // Fragment已添加，直接显示
            transaction.show(targetFragment)
        } else {
            // Fragment未添加，先添加再显示
            transaction.add(R.id.fragment_container, targetFragment)
        }

        // 使用commitNow确保立即执行，避免状态不一致
        transaction.commitNowAllowingStateLoss()

        currentFragmentId = itemId
    }

    /**
     * 获取或创建Fragment
     * @param itemId 底部导航项ID
     * @return 对应的Fragment实例
     */
    private fun getOrCreateFragment(itemId: Int): Fragment {
        return fragmentMap.getOrPut(itemId) {
            when (itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_market -> MarketFragment()
                R.id.nav_message -> MessageFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }

    /**
     * 启动发布Activity
     */
    private fun startPublishActivity() {
        val intent = Intent(this, PublishActivity::class.java)

        // 使用 ActivityOptions 设置转场动画
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up_in, R.anim.no_animation)

        // 启动 Activity 并应用转场动画
        startActivity(intent, options.toBundle())
    }

    /**
     * 保存状态
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_FRAGMENT, currentFragmentId)
    }

    companion object {
        private const val KEY_CURRENT_FRAGMENT = "current_fragment"
    }
}
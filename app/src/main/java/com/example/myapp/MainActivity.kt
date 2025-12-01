package com.example.myapp

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.myapp.ui.home.HomeFragment
import com.example.myapp.ui.market.MarketFragment
import com.example.myapp.ui.message.MessageFragment
import com.example.myapp.ui.profile.ProfileFragment
import com.example.myapp.ui.publish.PublishActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // 底部fragment
    // 让代码更简洁，避免了无休止的判空操作（null checks）。
    private lateinit var navHome: TextView
    private lateinit var navMarket: TextView
    private lateinit var navMessage: TextView
    private lateinit var navProfile: TextView

    private lateinit var navItems: List<TextView>
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private var currentNavId = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedInstanceState?.let {
            currentNavId = it.getInt(KEY_CURRENT_NAV, R.id.nav_home)
        }

        initViews()

        setupBackPressed()

        if (savedInstanceState == null) {
            selectNav(R.id.nav_home)
        } else {
            selectNav(currentNavId)
        }
    }

    // 初始化主页的几个Fragments
    private fun initViews() {
        navHome = findViewById(R.id.nav_home)
        navMarket = findViewById(R.id.nav_market)
        navMessage = findViewById(R.id.nav_message)
        navProfile = findViewById(R.id.nav_profile)

        navItems = listOf(navHome, navMarket, navMessage, navProfile)

        // 设置点击监听
        navHome.setOnClickListener { selectNav(R.id.nav_home) }
        navMarket.setOnClickListener { selectNav(R.id.nav_market) }
        navMessage.setOnClickListener { selectNav(R.id.nav_message) }
        navProfile.setOnClickListener { selectNav(R.id.nav_profile) }

        // 发布按钮
        findViewById<android.view.View>(R.id.fab_publish).setOnClickListener {
            startPublishActivity()
        }
    }

    private fun selectNav(navId: Int) {
        // 更新所有导航项样式
        navItems.forEach { textView ->
            val isSelected = textView.id == navId
            textView.isSelected = isSelected

            // 选中时加粗，未选中时正常
            textView.typeface = if (isSelected) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.DEFAULT
            }
        }

        // 切换Fragment
        showFragment(navId)
        currentNavId = navId
    }

    // 以内存换体验
    private fun showFragment(navId: Int) {
        val fragment = getOrCreateFragment(navId)
        val transaction = supportFragmentManager.beginTransaction()

        // 隐藏所有
        fragmentMap.values.forEach {
            if (it.isAdded) transaction.hide(it)
        }

        // 显示目标
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.fragment_container, fragment)
        }
        // 开始执行事务
        transaction.commitNowAllowingStateLoss()
    }

    private fun getOrCreateFragment(navId: Int): Fragment {
        return fragmentMap.getOrPut(navId) {
            when (navId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_market -> MarketFragment()
                R.id.nav_message -> MessageFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }

    // 如果不在首页->返回首页
    // 如果在首页->返回主界面
    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentNavId != R.id.nav_home) {
                    selectNav(R.id.nav_home)
                } else {
                    // 关闭当前对回调事件的拦截
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    //启动发布页， 同时添加动画
    private fun startPublishActivity() {
        val intent = Intent(this, PublishActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(
            this, R.anim.slide_up_in, R.anim.no_animation
        )
        startActivity(intent, options.toBundle())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 这行代码会让 Android 自动帮你保存界面上的一些标准状态
        super.onSaveInstanceState(outState)
        // 把当前的导航栏 ID (currentNavId) 放进去，贴上标签 KEY_CURRENT_NAV。
        outState.putInt(KEY_CURRENT_NAV, currentNavId)
    }

    companion object {
        private const val KEY_CURRENT_NAV = "current_nav"
    }
}
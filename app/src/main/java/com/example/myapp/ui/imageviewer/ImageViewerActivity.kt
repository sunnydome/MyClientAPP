package com.example.myapp.ui.imageviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.databinding.ActivityImageViewerBinding

/**
 * 全屏图片查看器 Activity
 *
 * 功能特性：
 * 1. 支持左右滑动切换图片
 * 2. 支持双指缩放
 * 3. 支持下滑关闭（带动画）
 * 4. 支持共享元素动画
 * 5. 背景透明度随手势变化
 * 6. 支持横竖屏切换和 Chrome OS 大屏设备
 */
class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var imageAdapter: ImageViewerAdapter

    private var imageUrls: List<String> = emptyList()
    private var currentPosition: Int = 0
    private var enterPosition: Int = 0

    companion object {
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val EXTRA_CURRENT_POSITION = "extra_current_position"
        private const val EXTRA_TRANSITION_NAME = "extra_transition_name"

        /**
         * 启动图片查看器
         *
         * @param context 上下文
         * @param imageUrls 图片URL列表
         * @param currentPosition 当前显示的图片位置
         * @param sharedElement 共享元素View（用于转场动画）
         * @param transitionName 共享元素名称
         */
        fun start(
            context: Context,
            imageUrls: List<String>,
            currentPosition: Int = 0,
            sharedElement: View? = null,
            transitionName: String? = null
        ) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URLS, ArrayList(imageUrls))
                putExtra(EXTRA_CURRENT_POSITION, currentPosition)
                putExtra(EXTRA_TRANSITION_NAME, transitionName)
            }

            if (context is Activity && sharedElement != null && transitionName != null) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    context,
                    sharedElement,
                    transitionName
                )
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
                if (context is Activity) {
                    context.overridePendingTransition(android.R.anim.fade_in, 0)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏沉浸式
        setupFullScreen()

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传递的数据
        imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
        currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)
        enterPosition = currentPosition
        val transitionName = intent.getStringExtra(EXTRA_TRANSITION_NAME)

        if (imageUrls.isEmpty()) {
            finish()
            return
        }

        // 设置共享元素回调
        setupSharedElementCallback(transitionName)

        // 延迟共享元素动画
        supportPostponeEnterTransition()

        initViews(transitionName)
        setupListeners()
    }

    private fun setupFullScreen() {
        // 设置透明状态栏和导航栏
        window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        // 沉浸式布局
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 隐藏系统栏
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupSharedElementCallback(transitionName: String?) {
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                // 如果用户滑动到了其他图片，取消共享元素动画
                if (currentPosition != enterPosition) {
                    names?.clear()
                    sharedElements?.clear()
                    return
                }

                // 获取当前显示的 ImageView
                val currentView = imageAdapter.getCurrentImageView(currentPosition)
                if (currentView != null && transitionName != null) {
                    sharedElements?.clear()
                    sharedElements?.put(transitionName, currentView)
                }
            }
        })
    }

    private fun initViews(transitionName: String?) {
        // 设置适配器
        imageAdapter = ImageViewerAdapter(
            imageUrls = imageUrls,
            enterPosition = enterPosition,
            transitionName = transitionName,
            onImageReady = {
                // 第一张图片加载完成后启动共享元素动画
                supportStartPostponedEnterTransition()
            },
            onDismiss = {
                // 下滑关闭
                finishWithAnimation()
            },
            onDrag = { translationY, alpha ->
                // 拖拽时更新背景透明度
                binding.root.setBackgroundColor(
                    Color.argb((alpha * 255).toInt(), 0, 0, 0)
                )
            },
            onSingleTap = {
                // 单击关闭
                finishWithAnimation()
            }
        )

        binding.viewPager.apply {
            adapter = imageAdapter
            setCurrentItem(currentPosition, false)
            offscreenPageLimit = 1
        }

        // 更新指示器
        updateIndicator()
    }

    private fun setupListeners() {
        // 监听页面切换
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                updateIndicator()
            }
        })

        // 点击背景关闭
        binding.root.setOnClickListener {
            finishWithAnimation()
        }
    }

    private fun updateIndicator() {
        if (imageUrls.size > 1) {
            binding.tvIndicator.visibility = View.VISIBLE
            binding.tvIndicator.text = "${currentPosition + 1}/${imageUrls.size}"
        } else {
            binding.tvIndicator.visibility = View.GONE
        }
    }

    private fun finishWithAnimation() {
        if (currentPosition == enterPosition) {
            // 使用共享元素动画返回
            finishAfterTransition()
        } else {
            // 使用淡出动画
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishWithAnimation()
    }

    /**
     * 处理配置变更（如屏幕旋转）
     * 由于在 Manifest 中设置了 configChanges，旋转时不会重建 Activity
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // 重新初始化图片矩阵以适应新的屏幕尺寸
        binding.viewPager.post {
            imageAdapter.notifyDataSetChanged()
        }
    }
}
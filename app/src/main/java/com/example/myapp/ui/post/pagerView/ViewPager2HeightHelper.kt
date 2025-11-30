package com.example.myapp.ui.post.pagerView

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * ViewPager2 动态高度辅助类
 * 用于实现图片自适应高度，去除灰色填充区域
 */
object ViewPager2HeightHelper {

    /**
     * 根据屏幕宽度和图片比例设置 ViewPager2 的高度
     *
     * @param viewPager2 目标 ViewPager2
     * @param aspectRatio 宽高比，例如 3f/4f 表示 3:4 竖图，4f/3f 表示 4:3 横图
     * @param maxHeightRatio 最大高度占屏幕高度的比例，防止图片过高
     */
    fun setHeightByAspectRatio(
        viewPager2: ViewPager2,
        aspectRatio: Float,
        maxHeightRatio: Float = 0.7f
    ) {
        viewPager2.post {
            val screenWidth = viewPager2.width
            val screenHeight = viewPager2.context.resources.displayMetrics.heightPixels

            // 计算目标高度
            var targetHeight = (screenWidth / aspectRatio).toInt()

            // 限制最大高度
            val maxHeight = (screenHeight * maxHeightRatio).toInt()
            if (targetHeight > maxHeight) {
                targetHeight = maxHeight
            }

            val layoutParams = viewPager2.layoutParams
            layoutParams.height = targetHeight
            viewPager2.layoutParams = layoutParams
        }
    }

    /**
     * 根据第一张图片的实际尺寸动态调整 ViewPager2 高度
     *
     * @param viewPager2 目标 ViewPager2
     * @param imageWidth 图片原始宽度
     * @param imageHeight 图片原始高度
     * @param maxHeightRatio 最大高度占屏幕高度的比例
     */
    fun setHeightByImageSize(
        viewPager2: ViewPager2,
        imageWidth: Int,
        imageHeight: Int,
        maxHeightRatio: Float = 0.75f
    ) {
        if (imageWidth <= 0 || imageHeight <= 0) return

        viewPager2.post {
            val screenWidth = viewPager2.width
            val screenHeight = viewPager2.context.resources.displayMetrics.heightPixels

            // 按图片比例计算高度
            val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
            var targetHeight = (screenWidth / aspectRatio).toInt()

            // 限制最大高度
            val maxHeight = (screenHeight * maxHeightRatio).toInt()
            // 限制最小高度（至少占屏幕高度的 30%）
            val minHeight = (screenHeight * 0.3f).toInt()

            targetHeight = targetHeight.coerceIn(minHeight, maxHeight)

            val layoutParams = viewPager2.layoutParams
            if (layoutParams.height != targetHeight) {
                layoutParams.height = targetHeight
                viewPager2.layoutParams = layoutParams
            }
        }
    }

    /**
     * 为 ViewPager2 设置预设高度（常用比例）
     */
    enum class ImageRatio(val widthRatio: Float, val heightRatio: Float) {
        RATIO_1_1(1f, 1f),      // 正方形 1:1
        RATIO_4_3(4f, 3f),      // 横图 4:3
        RATIO_3_4(3f, 4f),      // 竖图 3:4
        RATIO_16_9(16f, 9f),    // 宽屏 16:9
        RATIO_9_16(9f, 16f);    // 竖屏 9:16

        val aspectRatio: Float
            get() = widthRatio / heightRatio
    }

    /**
     * 使用预设比例设置高度
     */
    fun setHeightByRatio(viewPager2: ViewPager2, ratio: ImageRatio, maxHeightRatio: Float = 0.7f) {
        setHeightByAspectRatio(viewPager2, ratio.aspectRatio, maxHeightRatio)
    }
}

/**
 * ViewPager2 扩展函数
 */
fun ViewPager2.setDynamicHeight(aspectRatio: Float, maxHeightRatio: Float = 0.7f) {
    ViewPager2HeightHelper.setHeightByAspectRatio(this, aspectRatio, maxHeightRatio)
}

fun ViewPager2.setDynamicHeightByImage(imageWidth: Int, imageHeight: Int, maxHeightRatio: Float = 0.75f) {
    ViewPager2HeightHelper.setHeightByImageSize(this, imageWidth, imageHeight, maxHeightRatio)
}
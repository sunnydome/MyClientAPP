package com.example.myapp.ui.post.pagerView

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.myapp.R

/**
 * 图片ViewHolder - 优化版
 *
 * 优化点：
 * 1. 新增图片尺寸回调，用于动态调整 ViewPager2 高度
 * 2. 使用透明背景，去除灰色填充
 * 3. 优化 Glide 加载配置
 */
class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val mImageView: ImageView = itemView.findViewById(R.id.iv_image)

    /**
     * 绑定数据
     *
     * @param url 图片URL
     * @param transitionName 共享元素动画名称
     * @param onImageLoaded 图片加载完成回调
     * @param onImageSizeReady 图片尺寸获取回调（用于动态调整容器高度）
     */
    fun bindData(
        url: String,
        transitionName: String?,
        onImageLoaded: () -> Unit,
        onImageSizeReady: ((width: Int, height: Int) -> Unit)? = null
    ) {
        // 设置共享元素动画名称
        if (transitionName != null) {
            ViewCompat.setTransitionName(mImageView, transitionName)
        } else {
            ViewCompat.setTransitionName(mImageView, null)
        }

        // 重置 ImageView 状态
        mImageView.setImageDrawable(null)
        mImageView.scaleType = ImageView.ScaleType.FIT_CENTER

        Glide.with(itemView.context)
            .load(url)
            .placeholder(R.drawable.placeholder_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // 即使失败也要触发回调，否则页面会卡在空白状态
                    onImageLoaded()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // 触发加载完成回调
                    onImageLoaded()

                    // 获取图片原始尺寸
                    val imageWidth = resource.intrinsicWidth
                    val imageHeight = resource.intrinsicHeight

                    // 回调图片尺寸（用于动态调整容器高度）
                    onImageSizeReady?.invoke(imageWidth, imageHeight)

                    // 动态调整 scaleType
                    adjustScaleType(resource)

                    return false
                }
            })
            .into(mImageView)
    }

    /**
     * 原有的绑定方法（保持向后兼容）
     */
    fun bindData(url: String, transitionName: String?, onImageLoaded: () -> Unit) {
        bindData(url, transitionName, onImageLoaded, null)
    }

    /**
     * 根据图片和容器比例动态调整 scaleType
     *
     * 策略：
     * - 图片比容器更"瘦高"：使用 FIT_CENTER（保持完整，上下可能有空白）
     * - 图片比容器更"矮胖"：使用 FIT_CENTER（保持完整，左右可能有空白）
     * - 如果希望填满容器：可以使用 CENTER_CROP（但会裁剪部分内容）
     */
    private fun adjustScaleType(drawable: Drawable) {
        mImageView.post {
            val viewWidth = mImageView.width
            val viewHeight = mImageView.height

            if (viewWidth <= 0 || viewHeight <= 0) return@post

            val imageAspect = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
            val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()

            // 选择 scaleType 策略
            // 方案1：始终使用 FIT_CENTER，保证图片完整显示（推荐，配合动态高度使用）
            mImageView.scaleType = ImageView.ScaleType.FIT_CENTER

            // 方案2：根据比例选择（如果不使用动态高度，可以用这个）
            // mImageView.scaleType = if (imageAspect > viewAspect) {
            //     // 图片更宽，宽度撑满，高度会有空白
            //     ImageView.ScaleType.FIT_CENTER
            // } else {
            //     // 图片更高，高度撑满，宽度会有空白
            //     ImageView.ScaleType.FIT_CENTER
            // }
        }
    }
}
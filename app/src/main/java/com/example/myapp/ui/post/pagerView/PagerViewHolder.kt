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
 * 图片ViewHolder - 支持点击全屏查看
 */
class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val mImageView: ImageView = itemView.findViewById(R.id.iv_image)

    /**
     * 获取 ImageView（用于共享元素动画）
     */
    fun getImageView(): ImageView = mImageView

    /**
     * 绑定数据
     *
     * @param url 图片URL
     * @param transitionName 共享元素动画名称
     * @param onImageLoaded 图片加载完成回调
     * @param onImageSizeReady 图片尺寸获取回调
     * @param onImageClick 图片点击回调
     */
    fun bindData(
        url: String,
        transitionName: String?,
        onImageLoaded: () -> Unit,
        onImageSizeReady: ((width: Int, height: Int) -> Unit)? = null,
        onImageClick: ((imageView: ImageView) -> Unit)? = null
    ) {
        // 设置共享元素动画名称
        if (transitionName != null) {
            ViewCompat.setTransitionName(mImageView, transitionName)
        } else {
            ViewCompat.setTransitionName(mImageView, null)
        }

        // 设置点击事件
        mImageView.setOnClickListener {
            onImageClick?.invoke(mImageView)
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
                    onImageLoaded()

                    val imageWidth = resource.intrinsicWidth
                    val imageHeight = resource.intrinsicHeight
                    onImageSizeReady?.invoke(imageWidth, imageHeight)

                    adjustScaleType(resource)
                    return false
                }
            })
            .into(mImageView)
    }

    /**
     * 兼容旧接口
     */
    fun bindData(url: String, transitionName: String?, onImageLoaded: () -> Unit) {
        bindData(url, transitionName, onImageLoaded, null, null)
    }

    private fun adjustScaleType(drawable: Drawable) {
        mImageView.post {
            val viewWidth = mImageView.width
            val viewHeight = mImageView.height

            if (viewWidth <= 0 || viewHeight <= 0) return@post

            mImageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }
}
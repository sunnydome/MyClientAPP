package com.example.myapp.ui.post.pagerView

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.myapp.R

class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val mImageView: ImageView = itemView.findViewById(R.id.iv_image)

    fun bindData(url: String, transitionName: String?, onImageLoaded: () -> Unit) {

        if (transitionName != null) {
            ViewCompat.setTransitionName(mImageView, transitionName)
        }

        Glide.with(itemView.context)
            .load(url)
            .placeholder(R.drawable.placeholder_image)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    // 即使失败也要开始动画，否则页面会卡死在空白状态
                    onImageLoaded()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    onImageLoaded()
                    resource?.let { drawable ->
                        val viewWidth = mImageView.width
                        val viewHeight = mImageView.height

                        // 防止除以0
                        if (viewWidth > 0 && viewHeight > 0) {
                            val imageAspect = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
                            val viewAspect = viewHeight.toFloat() / viewWidth.toFloat()

                            // 核心逻辑：
                            // 如果图片比 View 更"瘦高" (imageAspect > viewAspect)，说明宽度填满时高度会溢出 -> 使用 CenterCrop (裁剪上下)
                            // 如果图片比 View 更"矮胖" (imageAspect <= viewAspect)，说明宽度填满时高度不够 -> 使用 FitCenter (留背景色)
                            if (imageAspect > viewAspect) {
                                mImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                            } else {
                                mImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        }
                    }
                    return false
                }
            })
            .into(mImageView)
    }
}
package com.example.myapp.ui.imageviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapp.R

/**
 * 图片查看器适配器
 */
class ImageViewerAdapter(
    private val imageUrls: List<String>,
    private val enterPosition: Int,
    private val transitionName: String?,
    private val onImageReady: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onDrag: (translationY: Float, alpha: Float) -> Unit,
    private val onSingleTap: () -> Unit
) : RecyclerView.Adapter<ImageViewerAdapter.ImageViewerViewHolder>() {

    private var hasTriggeredReady = false
    private val viewHolderMap = mutableMapOf<Int, ImageViewerViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_viewer, parent, false)
        return ImageViewerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewerViewHolder, position: Int) {
        viewHolderMap[position] = holder

        val isEnterImage = position == enterPosition
        holder.bind(
            url = imageUrls[position],
            transitionName = if (isEnterImage) transitionName else null,
            onImageLoaded = {
                if (isEnterImage && !hasTriggeredReady) {
                    hasTriggeredReady = true
                    onImageReady()
                }
            },
            onDismiss = onDismiss,
            onDrag = onDrag,
            onSingleTap = onSingleTap
        )
    }

    override fun onViewRecycled(holder: ImageViewerViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            viewHolderMap.remove(position)
        }
        holder.recycle()
    }

    override fun getItemCount(): Int = imageUrls.size

    /**
     * 获取当前位置的 ImageView（用于共享元素动画）
     */
    fun getCurrentImageView(position: Int): ImageView? {
        return viewHolderMap[position]?.getImageView()
    }

    class ImageViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val photoView: DragPhotoView = itemView.findViewById(R.id.photo_view)
        fun bind(
            url: String,
            transitionName: String?,
            onImageLoaded: () -> Unit,
            onDismiss: () -> Unit,
            onDrag: (Float, Float) -> Unit,
            onSingleTap: () -> Unit
        ) {
            // 设置共享元素名称
            if (transitionName != null) {
                photoView.transitionName = transitionName
            }

            // 设置回调
            photoView.setOnDismissListener(onDismiss)
            photoView.setOnDragListener(onDrag)
            photoView.setOnSingleTapListener(onSingleTap)

            // 加载图片
            Glide.with(itemView.context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(1080, 1920) // 限制最大尺寸，避免OOM
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                    ) {
                        photoView.setImageDrawable(resource)
                        onImageLoaded()
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        photoView.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        photoView.setImageDrawable(errorDrawable)
                        onImageLoaded()
                    }
                })
        }

        fun getImageView(): ImageView = photoView

        fun recycle() {
            Glide.with(itemView.context).clear(photoView)
            photoView.reset()
        }
    }
}
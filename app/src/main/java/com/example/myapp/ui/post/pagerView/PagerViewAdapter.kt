package com.example.myapp.ui.post.pagerView

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

/**
 * 图片轮播适配器 - 支持点击全屏查看
 */
class PagerViewAdapter(
    private val targetTransitionName: String?,
    private val onFirstImageLoaded: () -> Unit,
    private val onFirstImageSizeReady: ((width: Int, height: Int) -> Unit)? = null,
    private val onImageClick: ((position: Int, imageView: ImageView) -> Unit)? = null
) : RecyclerView.Adapter<PagerViewHolder>() {

    private var mList: List<String> = emptyList()
    private var hasNotifyLoaded = false
    private var hasNotifySizeReady = false

    // 保存 ViewHolder 引用，用于获取 ImageView
    private val viewHolderMap = mutableMapOf<Int, PagerViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picture, parent, false)
        return PagerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        viewHolderMap[position] = holder

        // 只有第一张图才作为共享元素
        val transName = if (position == 0) targetTransitionName else null

        holder.bindData(
            url = mList[position],
            transitionName = transName,
            onImageLoaded = {
                if (position == 0 && !hasNotifyLoaded) {
                    hasNotifyLoaded = true
                    onFirstImageLoaded()
                }
            },
            onImageSizeReady = { width, height ->
                if (position == 0 && !hasNotifySizeReady) {
                    hasNotifySizeReady = true
                    onFirstImageSizeReady?.invoke(width, height)
                }
            },
            onImageClick = { imageView ->
                onImageClick?.invoke(position, imageView)
            }
        )
    }

    override fun onViewRecycled(holder: PagerViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            viewHolderMap.remove(position)
        }
    }

    override fun getItemCount(): Int = mList.size

    /**
     * 设置图片列表
     */
    fun setList(list: List<String>) {
        if (mList != list) {
            hasNotifyLoaded = false
            hasNotifySizeReady = false
        }
        mList = list
        notifyDataSetChanged()
    }

    /**
     * 获取图片列表
     */
    fun getList(): List<String> = mList

    /**
     * 获取指定位置的 ImageView
     */
    fun getImageViewAt(position: Int): ImageView? {
        return viewHolderMap[position]?.getImageView()
    }
}
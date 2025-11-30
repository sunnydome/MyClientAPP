package com.example.myapp.ui.post.pagerView

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

/**
 * 图片轮播适配器 - 优化版
 *
 * 优化点：
 * 1. 新增图片尺寸回调，用于动态调整 ViewPager2 高度
 * 2. 使用 DiffUtil 优化数据更新（可选）
 * 3. 优化内存使用
 */
class PagerViewAdapter(
    private val targetTransitionName: String?,
    private val onFirstImageLoaded: () -> Unit,
    private val onFirstImageSizeReady: ((width: Int, height: Int) -> Unit)? = null
) : RecyclerView.Adapter<PagerViewHolder>() {

    private var mList: List<String> = emptyList()
    private var hasNotifyLoaded = false
    private var hasNotifySizeReady = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_picture, parent, false)
        return PagerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        // 只有第一张图（position 0）才作为共享元素
        val transName = if (position == 0) targetTransitionName else null

        holder.bindData(
            url = mList[position],
            transitionName = transName,
            onImageLoaded = {
                // 只有第一张图加载完才通知
                if (position == 0 && !hasNotifyLoaded) {
                    hasNotifyLoaded = true
                    onFirstImageLoaded()
                }
            },
            onImageSizeReady = { width, height ->
                // 只有第一张图的尺寸才用于调整容器高度
                if (position == 0 && !hasNotifySizeReady) {
                    hasNotifySizeReady = true
                    onFirstImageSizeReady?.invoke(width, height)
                }
            }
        )
    }

    override fun getItemCount(): Int = mList.size

    /**
     * 设置图片列表
     */
    fun setList(list: List<String>) {
        // 重置状态
        if (mList != list) {
            hasNotifyLoaded = false
            hasNotifySizeReady = false
        }
        mList = list
        notifyDataSetChanged()
    }

    /**
     * 获取当前图片列表（用于外部获取）
     */
    fun getList(): List<String> = mList

    override fun onViewRecycled(holder: PagerViewHolder) {
        super.onViewRecycled(holder)
        // 可以在这里清理 Glide 请求，避免内存泄漏
        // Glide.with(holder.itemView.context).clear(holder.itemView.findViewById(R.id.iv_image))
    }
}
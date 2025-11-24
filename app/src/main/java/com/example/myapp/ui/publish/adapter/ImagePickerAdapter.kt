package com.example.myapp.ui.publish.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R

/**
 * 图片选择器适配器
 * 显示已选择的图片和添加按钮
 */
class ImagePickerAdapter(
    private val onAddClick: () -> Unit,
    private val onImageClick: (Int, Uri) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : ListAdapter<ImagePickerItem, RecyclerView.ViewHolder>(ImageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_ADD = 1
        const val MAX_IMAGES = 9
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ImagePickerItem.Image -> VIEW_TYPE_IMAGE
            is ImagePickerItem.AddButton -> VIEW_TYPE_ADD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_picker, parent, false)
                ImageViewHolder(view)
            }
            VIEW_TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_picker_add, parent, false)
                AddButtonViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ImagePickerItem.Image -> (holder as ImageViewHolder).bind(item.uri, position)
            is ImagePickerItem.AddButton -> (holder as AddButtonViewHolder).bind()
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_image)
        private val removeButton: ImageView = itemView.findViewById(R.id.iv_remove)

        /**
         * 图片ViewHolder
         */
        fun bind(uri: Uri, position: Int) { // 注意：这里的 position 参数仅用于初次绑定，不要在点击事件中使用
            // 使用Glide加载图片
            Glide.with(itemView.context)
                .load(uri)
                .into(imageView)

            // 点击图片预览
            imageView.setOnClickListener {
                // 修改处 1：获取实时位置
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onImageClick(currentPos, uri)
                }
            }

            // 点击删除按钮
            removeButton.setOnClickListener {
                // 修改处 2：获取实时位置，代替原来的 position 参数
                val currentPos = bindingAdapterPosition
                // 确保位置有效（防止在动画移除过程中点击导致的越界崩溃）
                if (currentPos != RecyclerView.NO_POSITION) {
                    onRemoveClick(currentPos)
                }
            }
        }
    }

    /**
     * 添加按钮ViewHolder
     */
    inner class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.setOnClickListener {
                onAddClick()
            }
        }
    }

    /**
     * 更新图片列表
     * 自动添加"添加"按钮
     */
    fun updateImages(images: List<Uri>) {
        val items = mutableListOf<ImagePickerItem>()

        // 添加图片项
        images.forEach { uri ->
            items.add(ImagePickerItem.Image(uri))
        }

        // 如果图片数量未达上限，添加"添加"按钮
        if (images.size < MAX_IMAGES) {
            items.add(ImagePickerItem.AddButton)
        }

        submitList(items)
    }
}

/**
 * 图片选择器的Item类型
 */
sealed class ImagePickerItem {
    data class Image(val uri: Uri) : ImagePickerItem()
    object AddButton : ImagePickerItem()
}

/**
 * DiffUtil回调
 */
class ImageDiffCallback : DiffUtil.ItemCallback<ImagePickerItem>() {
    override fun areItemsTheSame(oldItem: ImagePickerItem, newItem: ImagePickerItem): Boolean {
        return when {
            oldItem is ImagePickerItem.Image && newItem is ImagePickerItem.Image ->
                oldItem.uri == newItem.uri
            oldItem is ImagePickerItem.AddButton && newItem is ImagePickerItem.AddButton -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ImagePickerItem, newItem: ImagePickerItem): Boolean {
        return oldItem == newItem
    }
}
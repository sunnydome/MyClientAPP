package com.example.myapp.ui.publish

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.ui.publish.model.PublishPost

/**
 * 发布页面的ViewModel
 * 管理发布内容的状态和业务逻辑
 */
class PublishViewModel : ViewModel() {

    companion object {
        const val MAX_IMAGE_COUNT = 9 // 最多选择9张图片
    }

    // 当前发布内容的状态
    private val _publishPost = MutableLiveData<PublishPost>(PublishPost())
    val publishPost: LiveData<PublishPost> = _publishPost

    // 选中的图片列表
    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    // 标题文本
    private val _title = MutableLiveData<String>("")
    val title: LiveData<String> = _title

    // 正文文本
    private val _content = MutableLiveData<String>("")
    val content: LiveData<String> = _content

    // 是否可以发布（至少有图片或文字）
    private val _canPublish = MutableLiveData<Boolean>(false)
    val canPublish: LiveData<Boolean> = _canPublish

    // 发布事件（用于通知Activity执行发布操作）
    private val _publishEvent = MutableLiveData<PublishPost?>()
    val publishEvent: LiveData<PublishPost?> = _publishEvent

    // 保存草稿事件
    private val _saveDraftEvent = MutableLiveData<PublishPost?>()
    val saveDraftEvent: LiveData<PublishPost?> = _saveDraftEvent

    /**
     * 添加图片
     */
    fun addImages(uris: List<Uri>) {
        val currentImages = _selectedImages.value ?: emptyList()
        val newImages = (currentImages + uris).take(MAX_IMAGE_COUNT)
        _selectedImages.value = newImages
        updatePublishPost()
    }

    /**
     * 移除图片
     */
    fun removeImage(position: Int) {
        val currentImages = _selectedImages.value?.toMutableList() ?: return
        if (position in currentImages.indices) {
            currentImages.removeAt(position)
            _selectedImages.value = currentImages
            updatePublishPost()
        }
    }

    /**
     * 移动图片位置（拖拽排序）
     */
    fun moveImage(fromPosition: Int, toPosition: Int) {
        val currentImages = _selectedImages.value?.toMutableList() ?: return
        if (fromPosition in currentImages.indices && toPosition in currentImages.indices) {
            val item = currentImages.removeAt(fromPosition)
            currentImages.add(toPosition, item)
            _selectedImages.value = currentImages
            updatePublishPost()
        }
    }

    /**
     * 检查是否还能添加更多图片
     */
    fun canAddMoreImages(): Boolean {
        val currentCount = _selectedImages.value?.size ?: 0
        return currentCount < MAX_IMAGE_COUNT
    }

    /**
     * 获取剩余可添加图片数量
     */
    fun getRemainingImageSlots(): Int {
        val currentCount = _selectedImages.value?.size ?: 0
        return MAX_IMAGE_COUNT - currentCount
    }

    /**
     * 更新标题
     */
    fun updateTitle(text: String) {
        _title.value = text
        updatePublishPost()
    }

    /**
     * 更新正文
     */
    fun updateContent(text: String) {
        _content.value = text
        updatePublishPost()
    }

    /**
     * 更新发布内容并检查是否可以发布
     */
    private fun updatePublishPost() {
        val post = PublishPost(
            imageUris = _selectedImages.value ?: emptyList(),
            title = _title.value ?: "",
            content = _content.value ?: ""
        )
        _publishPost.value = post
        _canPublish.value = post.isValid()
    }

    /**
     * 请求发布
     * 触发发布事件，由Activity处理实际的发布逻辑
     */
    fun requestPublish() {
        val post = _publishPost.value ?: return
        if (post.isValid()) {
            _publishEvent.value = post
        }
    }

    /**
     * 发布事件已处理（重置事件状态）
     */
    fun publishEventHandled() {
        _publishEvent.value = null
    }

    /**
     * 保存为草稿
     */
    fun saveDraft() {
        val post = _publishPost.value ?: return
        if (!post.isEmpty()) {
            _saveDraftEvent.value = post.copy(isDraft = true)
        }
    }

    /**
     * 草稿保存事件已处理
     */
    fun draftEventHandled() {
        _saveDraftEvent.value = null
    }

    /**
     * 检查是否有未保存的内容
     */
    fun hasUnsavedContent(): Boolean {
        val post = _publishPost.value ?: return false
        return !post.isEmpty()
    }

    /**
     * 清空所有内容
     */
    fun clear() {
        _selectedImages.value = emptyList()
        _title.value = ""
        _content.value = ""
        updatePublishPost()
    }

    /**
     * 从草稿恢复内容
     */
    fun loadFromDraft(draft: PublishPost) {
        _selectedImages.value = draft.imageUris
        _title.value = draft.title
        _content.value = draft.content
        updatePublishPost()
    }
}
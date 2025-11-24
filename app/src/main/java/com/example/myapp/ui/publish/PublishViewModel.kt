package com.example.myapp.ui.publish

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.mock.MockDataProvider
import com.example.myapp.data.model.Draft
import com.example.myapp.data.model.Post
import com.example.myapp.data.repository.DraftRepository
import com.example.myapp.data.repository.PostRepository
import com.example.myapp.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * 发布页面的ViewModel
 * 管理发布内容的状态和业务逻辑
 *
 * 更新说明：集成新的数据层架构，支持发布到数据库和草稿保存
 */
class PublishViewModel(application: Application) : AndroidViewModel(application) {

    // 通过application参数获取数据库和Repository
    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)
    private val draftRepository: DraftRepository = DraftRepository.getInstance(database)
    private val userRepository: UserRepository = UserRepository.getInstance(database)

    companion object {
        const val MAX_IMAGE_COUNT = 9 // 最多选择9张图片
        const val MAX_TITLE_LENGTH = 20 // 标题长度最多为20
        const val MAX_CONTENT_LENGTH = 1000 // 正文长度最多为1000
    }

    // 当前编辑的草稿ID（如果是从草稿恢复的话）
    private var currentDraftId: Long? = null

    // 选中的图片列表
    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    // 标题文本
    private val _title = MutableLiveData<String>("")
    val title: LiveData<String> = _title

    // 正文文本
    private val _content = MutableLiveData<String>("")
    val content: LiveData<String> = _content

    // 分类（默认发现）
    private val _category = MutableLiveData<String>("发现")
    val category: LiveData<String> = _category

    // 位置信息
    private val _location = MutableLiveData<String>("")
    val location: LiveData<String> = _location

    // 是否可以发布（至少有图片或文字）
    private val _canPublish = MutableLiveData<Boolean>(false)
    val canPublish: LiveData<Boolean> = _canPublish

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 发布事件
    private val _publishEvent = MutableLiveData<PublishEvent?>()
    val publishEvent: LiveData<PublishEvent?> = _publishEvent

    // 保存草稿事件（兼容旧代码）
    private val _saveDraftEvent = MutableLiveData<Any?>()
    val saveDraftEvent: LiveData<Any?> = _saveDraftEvent

    // ========== 图片操作 ==========

    /**
     * 添加图片
     */
    fun addImages(uris: List<Uri>) {
        val currentImages = _selectedImages.value ?: emptyList()
        val newImages = (currentImages + uris).take(MAX_IMAGE_COUNT)
        _selectedImages.value = newImages
        updateCanPublish()
    }

    /**
     * 移除图片
     */
    fun removeImage(position: Int) {
        val currentImages = _selectedImages.value?.toMutableList() ?: return
        if (position in currentImages.indices) {
            currentImages.removeAt(position)
            _selectedImages.value = currentImages
            updateCanPublish()
        }
    }

    /**
     * 移动图片位置（拖拽排序）
     */
    // TODO: 修复这个逻辑
    fun moveImage(fromPosition: Int, toPosition: Int) {
        val currentImages = _selectedImages.value?.toMutableList() ?: return
        if (fromPosition in currentImages.indices && toPosition in currentImages.indices) {
            val item = currentImages.removeAt(fromPosition)
            currentImages.add(toPosition, item)
            _selectedImages.value = currentImages
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

    // ========== 内容操作 ==========

    /**
     * 更新标题
     */
    fun updateTitle(text: String) {
        _title.value = text.take(MAX_TITLE_LENGTH)
        updateCanPublish()
    }

    /**
     * 更新正文
     */
    fun updateContent(text: String) {
        _content.value = text.take(MAX_CONTENT_LENGTH)
        updateCanPublish()
    }

    /**
     * 更新分类
     */
    fun updateCategory(category: String) {
        _category.value = category
    }

    /**
     * 更新位置
     */
    fun updateLocation(location: String) {
        _location.value = location
    }

    /**
     * 更新是否可以发布的状态
     */
    private fun updateCanPublish() {
        val hasImages = !_selectedImages.value.isNullOrEmpty()
        val hasTitle = !_title.value.isNullOrBlank()
        val hasContent = !_content.value.isNullOrBlank()
        _canPublish.value = hasImages || hasTitle || hasContent
    }

    // ========== 发布操作 ==========

    /**
     * 请求发布
     */
    fun requestPublish() {
        if (_canPublish.value != true) return

        viewModelScope.launch {
            _isLoading.value = true

            // 获取当前用户信息
            val currentUser = userRepository.getCurrentUserSync()
            if (currentUser == null) {
                _isLoading.value = false
                _publishEvent.value = PublishEvent.Error("获取用户信息失败")
                return@launch
            }

            // 创建帖子
            val post = Post(
                id = MockDataProvider.generatePostId(),
                authorId = currentUser.id,
                authorName = currentUser.userName,
                authorAvatar = currentUser.avatarUrl,
                title = _title.value ?: "",
                content = _content.value ?: "",
                // 注意：实际项目中，这里应该先上传图片到服务器获取URL
                // 这里暂时将本地URI转为字符串存储，仅用于测试
                imageUrls = _selectedImages.value?.map { it.toString() } ?: emptyList(),
                coverUrl = _selectedImages.value?.firstOrNull()?.toString() ?: "",
                category = _category.value ?: "发现",
                location = _location.value ?: ""
            )

            val result = postRepository.publishPost(post)
            _isLoading.value = false

            result.fold(
                onSuccess = { publishedPost ->
                    // 如果是从草稿发布的，删除草稿
                    currentDraftId?.let { draftId ->
                        draftRepository.deleteDraft(draftId)
                    }
                    _publishEvent.value = PublishEvent.Success(publishedPost)
                },
                onFailure = { error ->
                    _publishEvent.value = PublishEvent.Error(error.message ?: "发布失败")
                }
            )
        }
    }

    /**
     * 发布事件已处理
     */
    fun publishEventHandled() {
        _publishEvent.value = null
    }

    // ========== 草稿操作 ==========

    /**
     * 保存为草稿
     */
    fun saveDraft() {
        if (!hasUnsavedContent()) return

        viewModelScope.launch {
            _isLoading.value = true

            val result = draftRepository.saveDraft(
                title = _title.value ?: "",
                content = _content.value ?: "",
                imageUris = _selectedImages.value ?: emptyList(),
                existingDraftId = currentDraftId
            )

            _isLoading.value = false

            result.fold(
                onSuccess = { draftId ->
                    currentDraftId = draftId
                    _saveDraftEvent.value = Unit // 触发事件
                },
                onFailure = { error ->
                    _publishEvent.value = PublishEvent.Error(error.message ?: "保存失败")
                }
            )
        }
    }

    /**
     * 草稿保存事件已处理
     */
    fun draftEventHandled() {
        _saveDraftEvent.value = null
    }

    /**
     * 从草稿恢复内容
     */
    fun loadFromDraft(draftId: Long) {
        viewModelScope.launch {
            val draft = draftRepository.getDraftByIdSync(draftId) ?: return@launch
            currentDraftId = draftId
            _selectedImages.value = draft.getImageUriList()
            _title.value = draft.title
            _content.value = draft.content
            updateCanPublish()
        }
    }

    /**
     * 从草稿恢复内容（兼容旧代码）
     */
    fun loadFromDraft(draft: com.example.myapp.ui.publish.model.PublishPost) {
        _selectedImages.value = draft.imageUris
        _title.value = draft.title
        _content.value = draft.content
        updateCanPublish()
    }

    // ========== 工具方法 ==========

    /**
     * 检查是否有未保存的内容
     */
    fun hasUnsavedContent(): Boolean {
        val hasImages = !_selectedImages.value.isNullOrEmpty()
        val hasTitle = !_title.value.isNullOrBlank()
        val hasContent = !_content.value.isNullOrBlank()
        return hasImages || hasTitle || hasContent
    }

    /**
     * 清空所有内容
     */
    fun clear() {
        currentDraftId = null
        _selectedImages.value = emptyList()
        _title.value = ""
        _content.value = ""
        _category.value = "发现"
        _location.value = ""
        updateCanPublish()
    }

    /**
     * 发布事件密封类
     */
    sealed class PublishEvent {
        data class Success(val post: Post) : PublishEvent()
        data class Error(val message: String) : PublishEvent()
    }
}
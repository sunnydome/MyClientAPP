package com.example.myapp.ui.publish

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.model.Post
import com.example.myapp.data.repository.DraftRepository
import com.example.myapp.data.repository.PostRepository
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.data.repository.FileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val draftRepository: DraftRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository
): ViewModel(){


    companion object {
        const val MAX_IMAGE_COUNT = 9
        const val MAX_TITLE_LENGTH = 20
        const val MAX_CONTENT_LENGTH = 1000
        private const val TAG = "PublishViewModel"
    }

    private var currentDraftId: Long? = null

    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    private val _title = MutableLiveData<String>("")
    val title: LiveData<String> = _title

    private val _content = MutableLiveData<String>("")
    val content: LiveData<String> = _content

    private val _category = MutableLiveData<String>("发现")
    val category: LiveData<String> = _category

    private val _location = MutableLiveData<String>("")
    val location: LiveData<String> = _location

    private val _canPublish = MutableLiveData<Boolean>(false)
    val canPublish: LiveData<Boolean> = _canPublish

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _publishEvent = MutableLiveData<PublishEvent?>()
    val publishEvent: LiveData<PublishEvent?> = _publishEvent

    private val _saveDraftEvent = MutableLiveData<Any?>()
    val saveDraftEvent: LiveData<Any?> = _saveDraftEvent

    fun addImages(uris: List<Uri>) {
        val currentImages = _selectedImages.value ?: emptyList()
        val newImages = (currentImages + uris).take(MAX_IMAGE_COUNT)
        _selectedImages.value = newImages
        updateCanPublish()
    }

    fun removeImage(position: Int) {
        val currentImages = _selectedImages.value?.toMutableList() ?: return
        if (position in currentImages.indices) {
            currentImages.removeAt(position)
            _selectedImages.value = currentImages
            updateCanPublish()
        }
    }

    fun canAddMoreImages(): Boolean {
        val currentCount = _selectedImages.value?.size ?: 0
        return currentCount < MAX_IMAGE_COUNT
    }

    fun getRemainingImageSlots(): Int {
        val currentCount = _selectedImages.value?.size ?: 0
        return MAX_IMAGE_COUNT - currentCount
    }

    fun updateTitle(text: String) {
        _title.value = text.take(MAX_TITLE_LENGTH)
        updateCanPublish()
    }

    fun updateContent(text: String) {
        _content.value = text.take(MAX_CONTENT_LENGTH)
        updateCanPublish()
    }

    fun updateCategory(category: String) {
        _category.value = category
    }

    fun updateLocation(location: String) {
        _location.value = location
    }

    private fun updateCanPublish() {
        val hasImages = !_selectedImages.value.isNullOrEmpty()
        val hasTitle = !_title.value.isNullOrBlank()
        val hasContent = !_content.value.isNullOrBlank()
        _canPublish.value = hasImages || hasTitle || hasContent
    }

    // ========== 核心修改区域：发布逻辑 ==========

    fun requestPublish() {
        if (_canPublish.value != true) return

        viewModelScope.launch {
            _isLoading.value = true

            // 获取用户信息
            val currentUser = userRepository.getCurrentUserSync() ?: run {
                userRepository.refreshCurrentUser().getOrNull()
            }
            // 这里的 Mock 容错：如果还是获取不到用户，创建一个临时的本地用户，防止无法发布
            val finalUser = currentUser ?: com.example.myapp.data.model.User(
                id = "local_user", userName = "我", avatarUrl = "", bio = ""
            )

            // 处理图片 (复制到私有目录 + 计算宽高比)
            val localUris = _selectedImages.value ?: emptyList()
            var savedImagePaths: List<String> = emptyList()
            var coverRatio = 1.0f // 默认 1:1

            if (localUris.isNotEmpty()) {
                // 将图片复制到 APP 私有目录，生成 file:// 路径
                savedImagePaths = fileRepository.copyImagesToAppStorage(localUris)

                // 计算第一张图片的宽高比，用于瀑布流布局
                // 解决布局重叠的关键！
                coverRatio = fileRepository.calculateImageAspectRatio(localUris.first())
            }

            // 3. 构建 Post 对象
            val localId = "local_${System.currentTimeMillis()}"

            val post = Post(
                id = localId,
                authorId = finalUser.id,
                authorName = finalUser.userName,
                authorAvatar = finalUser.avatarUrl,
                title = _title.value ?: "",
                content = _content.value ?: "",
                imageUrls = savedImagePaths, // 使用 file:// 路径
                coverUrl = savedImagePaths.firstOrNull() ?: "",
                coverAspectRatio = coverRatio, // 使用计算出的真实比例
                category = _category.value ?: "发现",
                location = _location.value ?: "",
                publishTime = System.currentTimeMillis()
            )

            // 4. 调用 Repository (逻辑已改为强制存库)
            // 尝试一下虚假的网络上传（不影响流程）
            try {
                // 这里可以保留你之前的 uploadImages 逻辑去跑一下网络，或者直接忽略
                // TODO: 开发完后端或者测试接口时进行测试或上传
            } catch (e: Exception) {}

            val result = postRepository.publishPost(post)
            _isLoading.value = false

            result.fold(
                onSuccess = { publishedPost ->
                    currentDraftId?.let { draftRepository.deleteDraft(it) }
                    _publishEvent.value = PublishEvent.Success(publishedPost)
                },
                onFailure = { error ->
                    _publishEvent.value = PublishEvent.Error(error.message ?: "发布失败")
                }
            )
        }
    }

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
                    _publishEvent.value = PublishEvent.Error(error.message ?: "保存草稿失败")
                }
            )
        }
    }

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
     * 从草稿恢复内容（兼容传递对象的方式）
     */
    fun loadFromDraft(draft: com.example.myapp.ui.publish.model.PublishPost) {
        // 为了兼容旧代码的数据类，如果已经全面转用 Draft 实体，可以移除此方法
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
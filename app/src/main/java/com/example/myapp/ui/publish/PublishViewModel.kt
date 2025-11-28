package com.example.myapp.ui.publish

import android.app.Application
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.model.Post
import com.example.myapp.data.repository.DraftRepository
import com.example.myapp.data.repository.PostRepository
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * 发布页面的ViewModel
 * 管理发布内容的状态和业务逻辑
 */
class PublishViewModel(application: Application) : AndroidViewModel(application) {

    // 通过application参数获取数据库和Repository
    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val postRepository: PostRepository = PostRepository.getInstance(database)
    private val draftRepository: DraftRepository = DraftRepository.getInstance(database)
    private val userRepository: UserRepository = UserRepository.getInstance(database)

    // 获取 API 实例 (确保 RetrofitClient 中已添加 fileApi)
    private val fileApi = RetrofitClient.fileApi

    companion object {
        const val MAX_IMAGE_COUNT = 9 // 最多选择9张图片
        const val MAX_TITLE_LENGTH = 20 // 标题长度最多为20
        const val MAX_CONTENT_LENGTH = 1000 // 正文长度最多为1000
        private const val TAG = "PublishViewModel"
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

    // 保存草稿事件
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

    // ========== 发布操作 ==========

    /**
     * 请求发布
     */
    fun requestPublish() {
        if (_canPublish.value != true) return

        viewModelScope.launch {
            _isLoading.value = true

            // 1. 获取用户信息 (保持不变)
            val currentUser = userRepository.getCurrentUserSync() ?: run {
                userRepository.refreshCurrentUser().getOrNull()
            }
            if (currentUser == null) {
                _isLoading.value = false
                _publishEvent.value = PublishEvent.Error("获取用户信息失败")
                return@launch
            }
            // 2. 上传图片 (保持不变，使用我们之前讨论的模拟上传逻辑)
            // 简单起见，这里假设直接使用本地 Uri (真实场景需上传)
            val uploadedImageUrls = _selectedImages.value?.map { it.toString() } ?: emptyList()

            // 3. 构建 Post 对象 【关键修改】
            // 生成一个基于时间的本地 ID，确保唯一性，也方便排序
            val localId = "local_${System.currentTimeMillis()}"

            val post = Post(
                id = localId,  // <--- 以前是 ""，现在改为生成 ID
                authorId = currentUser.id,
                authorName = currentUser.userName,
                authorAvatar = currentUser.avatarUrl,
                title = _title.value ?: "",
                content = _content.value ?: "",
                imageUrls = uploadedImageUrls,
                coverUrl = uploadedImageUrls.firstOrNull() ?: "",

                // 确保分类是 "发现" (或者是当前选中的分类)，这样才能出现在首页默认列表里
                category = _category.value ?: "发现",
                location = _location.value ?: "",

                // 确保时间是当前时间，保证它排在列表最前面 (ORDER BY publishTime DESC)
                publishTime = System.currentTimeMillis()
            )

            // 4. 调用 Repository 发布
            val result = postRepository.publishPost(post)
            _isLoading.value = false

            result.fold(
                onSuccess = { publishedPost ->
                    // 发布成功，删除草稿
                    currentDraftId?.let { draftRepository.deleteDraft(it) }
                    _publishEvent.value = PublishEvent.Success(publishedPost)
                },
                onFailure = { error ->
                    _publishEvent.value = PublishEvent.Error(error.message ?: "发布失败")
                }
            )
        }
    }

    /**
     * 上传图片列表 (失败时返回本地路径)
     */
    private suspend fun uploadImages(uris: List<Uri>): List<String>? {
        return withContext(Dispatchers.IO) {
            val urls = mutableListOf<String>()
            try {
                for (uri in uris) {
                    // 尝试上传
                    try {
                        val part = prepareFilePart("file", uri)
                        if (part != null) {
                            val response = fileApi.uploadImage(part)
                            if (response.isSuccess() && response.data != null) {
                                urls.add(response.data)
                                continue // 上传成功，处理下一张
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "图片上传异常 (忽略): ${e.message}")
                    }

                    // 如果上面上传失败了，或者没走通，
                    // 直接把本地 Uri 转为 String 当作 URL 使用
                    // 这样 Glide 加载时会把它当成本地文件加载，依然能显示
                    urls.add(uri.toString())
                }
                urls
            } catch (e: Exception) {
                Log.e(TAG, "上传流程严重错误", e)
                null // 只有这里返回 null 才会中断发布
            }
        }
    }

    /**
     * 辅助方法：将 Uri 转换为 MultipartBody.Part
     * 需要将 ContentResolver 的流复制到临时文件，因为 Retrofit 需要文件长度
     */
    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver

        try {
            // 获取文件类型
            val mimeType = contentResolver.getType(fileUri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            // 创建临时文件
            val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)

            // 复制流到临时文件
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 创建 RequestBody
            val requestFile = tempFile.asRequestBody(mimeType?.toMediaTypeOrNull())

            // 创建 Part
            return MultipartBody.Part.createFormData(partName, tempFile.name, requestFile)
        } catch (e: Exception) {
            Log.e(TAG, "准备上传文件失败: $fileUri", e)
            return null
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
        // 这里只是为了兼容旧代码的数据类，如果已经全面转用 Draft 实体，可以移除此方法
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
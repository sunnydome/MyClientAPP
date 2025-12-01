package com.example.myapp.ui.publish

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.ui.publish.adapter.ImagePickerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * 发布Activity - 独立的发布页面
 * 使用MVVM架构管理发布内容
 */
@AndroidEntryPoint
class PublishActivity : FragmentActivity() {

    private lateinit var viewModel: PublishViewModel
    private lateinit var imageAdapter: ImagePickerAdapter

    // Views
    private lateinit var rvImages: RecyclerView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnPublish: TextView
    private lateinit var btnSaveDraft: TextView
    private lateinit var btnClose: View

    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlots = viewModel.getRemainingImageSlots()
            val urisToAdd = uris.take(remainingSlots)
            viewModel.addImages(urisToAdd)

            // 如果选择的图片超过剩余槽位，提示用户
            if (uris.size > remainingSlots) {
                Toast.makeText(
                    this,
                    "最多只能选择${PublishViewModel.MAX_IMAGE_COUNT}张图片",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置退出动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.no_animation,
                R.anim.slide_down_out
            )
        }
        setContentView(R.layout.activity_publish)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[PublishViewModel::class.java]

        // 初始化Views
        initViews()

        // 设置适配器
        setupImageAdapter()

        // 设置监听器
        setupListeners()

        // 观察ViewModel数据
        observeViewModel()
    }

    /**
     * 初始化Views
     */
    private fun initViews() {
        rvImages = findViewById(R.id.rv_images)
        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        btnPublish = findViewById(R.id.btn_publish)
        btnSaveDraft = findViewById(R.id.btn_save_draft)
        btnClose = findViewById(R.id.btn_close)
    }

    /**
     * 设置图片适配器
     */
    private fun setupImageAdapter() {
        imageAdapter = ImagePickerAdapter(
            onAddClick = {
                // 点击添加按钮，打开图片选择器
                if (viewModel.canAddMoreImages()) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    Toast.makeText(
                        this,
                        "最多只能选择${PublishViewModel.MAX_IMAGE_COUNT}张图片",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onImageClick = { position, _, view ->
                // 获取当前所有图片的 Uri 列表
                val uris = viewModel.selectedImages.value ?: emptyList()

                // 将 Uri 转为 String (Glide 支持 content:// 或 file:// 字符串)
                val imageUrls = uris.map { it.toString() }

                // 设置临时的 TransitionName 以启用动画
                // 必须给 View 设置一个名字，系统才能找到它
                val transitionName = "publish_image_$position"
                androidx.core.view.ViewCompat.setTransitionName(view, transitionName)

                // 启动图片查看器 (复用现有的 ImageViewerActivity)
                com.example.myapp.ui.imageviewer.ImageViewerActivity.start(
                    context = this,
                    imageUrls = imageUrls,       // 传入转换后的列表
                    currentPosition = position,  // 传入当前点击的位置
                    sharedElement = view,        // 传入共享元素
                    transitionName = transitionName // 传入动画名称
                )
            },
            onRemoveClick = { position ->
                // 移除图片
                viewModel.removeImage(position)
            }
        )

        // 设置网格布局，3列
        rvImages.layoutManager = GridLayoutManager(this, 3)
        rvImages.adapter = imageAdapter
    }

    /**
     * 设置监听器
     * 关心与UI交互
     */
    private fun setupListeners() {
        // 返回按钮
        btnClose.setOnClickListener {
            handleBackPressed()
        }

        // 标题输入监听
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val title = s?.toString() ?:""
                if(title.length > PublishViewModel.MAX_TITLE_LENGTH) {
                    etTitle.setText(title.take(PublishViewModel.MAX_TITLE_LENGTH))
                    etTitle.setSelection(PublishViewModel.MAX_TITLE_LENGTH)  // 保持光标在末尾
                }
                viewModel.updateTitle(title)
            }
        })

        // 正文输入监听
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val content = s?.toString() ?:""
                if(content.length > PublishViewModel.MAX_CONTENT_LENGTH) {
                    etTitle.setText(title.take(PublishViewModel.MAX_CONTENT_LENGTH))
                    etTitle.setSelection(PublishViewModel.MAX_CONTENT_LENGTH)  // 保持光标在末尾
                }
                viewModel.updateContent(content)
            }
        })

        // 发布按钮
        btnPublish.setOnClickListener {
            viewModel.requestPublish()
        }

        // 存草稿按钮
        btnSaveDraft.setOnClickListener {
            viewModel.saveDraft()
        }
    }

    /**
     * 观察ViewModel数据
     * 响应数据变化
     */
    private fun observeViewModel() {
        // 观察图片列表变化
        viewModel.selectedImages.observe(this) { images ->
            imageAdapter.updateImages(images)
        }

        // 观察是否可以发布
        viewModel.canPublish.observe(this) { canPublish ->
            btnPublish.isEnabled = canPublish
            btnPublish.alpha = if (canPublish) 1.0f else 0.5f
        }

        // 观察草稿恢复事件
        viewModel.restoreDraftEvent.observe(this) { restored ->
            if (restored) {
                Toast.makeText(this, "已自动恢复上次的草稿", Toast.LENGTH_SHORT).show()
                // 手动刷新一下 EditText 的显示（虽然 LiveData 双向绑定应该会自动更新，但确保光标位置等）
                etTitle.setSelection(etTitle.text.length)
                etContent.setSelection(etContent.text.length)
            }
        }

        // 观察发布事件
        viewModel.publishEvent.observe(this) { post ->
            post?.let {
                // 修改提示语
                Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show()

                // 发布成功后关闭页面
                finish()
                viewModel.publishEventHandled()
            }
        }
        // 观察标题变化，用于草稿恢复
        viewModel.title.observe(this) { title ->
            //为了防止死循环（setText触发TextWatcher，TextWatcher又更新ViewModel），加一个判断
            if (etTitle.text.toString() != title) {
                etTitle.setText(title)
                // 将光标移到末尾
                etTitle.setSelection(title.length)
            }
        }

        // 观察内容变化，用于草稿恢复
        viewModel.content.observe(this) { content ->
            if (etContent.text.toString() != content) {
                etContent.setText(content)
                etContent.setSelection(content.length)
            }
        }

        // 观察保存草稿事件
        viewModel.saveDraftEvent.observe(this) {
            // [修改] 这里的逻辑稍微简化，只要事件触发就退出
            Toast.makeText(this, "草稿已保存", Toast.LENGTH_SHORT).show()
            finish() // 退出页面
            overridePendingTransition(0, R.anim.slide_down_out)
        }
    }

    /**
     * 处理返回事件
     */
    private fun handleBackPressed() {
        // 如果有未保存的内容，提示用户
        if (viewModel.hasUnsavedContent()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage("是否保存为草稿？")
                .setPositiveButton("保存") { _, _ ->
                    viewModel.saveDraft()
                }
                .setNegativeButton("放弃") { _, _ ->
                    finish()
                }
                .setNeutralButton("取消", null)
                .show()
        } else {
            finish()
        }
    }

    /**
     * 重写返回键
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPressed()
    }
}
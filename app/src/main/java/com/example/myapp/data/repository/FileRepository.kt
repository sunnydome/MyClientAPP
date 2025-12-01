package com.example.myapp.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责本地文件操作的仓库
 */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context // Hilt 自动注入 Application Context
) {
    private val TAG = "FileRepository"

    /**
     * 将 Uri 对应的图片复制到应用私有目录下
     */
    suspend fun copyImagesToAppStorage(uris: List<Uri>): List<String> {
        return withContext(Dispatchers.IO) {
            val paths = mutableListOf<String>()

            // 创建专门存放发布图片的目录
            val imagesDir = File(context.filesDir, "published_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            for (uri in uris) {
                try {
                    // 生成唯一文件名
                    val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                    val destFile = File(imagesDir, fileName)

                    // 复制文件流
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // 返回 file:// 路径
                    paths.add(destFile.absolutePath)
                } catch (e: Exception) {
                    Log.e(TAG, "复制图片失败: $uri", e)
                    // 如果复制失败，回退到原始 Uri
                    paths.add(uri.toString())
                }
            }
            paths
        }
    }

    /**
     * 计算图片宽高比 (Width / Height)
     * (这个方法之前也在 ViewModel 里，建议一起移过来)
     */
    suspend fun calculateImageAspectRatio(uri: Uri): Float {
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options()
                // 只解码边界，不加载整个图片到内存
                options.inJustDecodeBounds = true

                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.outWidth.toFloat() / options.outHeight.toFloat()
                } else {
                    1.0f // 默认比例
                }
            } catch (e: Exception) {
                Log.e(TAG, "计算比例失败", e)
                1.0f
            }
        }
    }
}
package com.example.myapp

import android.app.Application
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.repository.CommentRepository
import com.example.myapp.data.repository.DraftRepository
import com.example.myapp.data.repository.PostRepository
import com.example.myapp.data.repository.UserRepository

/**
 * 应用Application类
 * 用于初始化全局组件
 */
class MyApplication : Application() {


    // 懒加载数据库实例
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // 懒加载Repository实例
    val postRepository: PostRepository by lazy {
        PostRepository.getInstance(database)
    }

    val commentRepository: CommentRepository by lazy {
        CommentRepository.getInstance(database)
    }

    val userRepository: UserRepository by lazy {
        UserRepository.getInstance(database)
    }

    val draftRepository: DraftRepository by lazy {
        DraftRepository.getInstance(database)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 预热数据库（可选，用于提前加载模拟数据）
        // 这会在后台线程触发数据库创建和初始数据插入
        Thread {
            database.openHelper.writableDatabase
        }.start()
    }
    companion object {
        @Volatile
        private var instance: MyApplication? = null

        /**
         * 获取Application实例
         */
        fun getInstance(): MyApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }

        /**
         * 便捷方法：获取数据库实例
         */
        fun getDatabase(): AppDatabase = getInstance().database

        /**
         * 便捷方法：获取PostRepository
         */
        fun getPostRepository(): PostRepository = getInstance().postRepository

        /**
         * 便捷方法：获取CommentRepository
         */
        fun getCommentRepository(): CommentRepository = getInstance().commentRepository

        /**
         * 便捷方法：获取UserRepository
         */
        fun getUserRepository(): UserRepository = getInstance().userRepository

        /**
         * 便捷方法：获取DraftRepository
         */
        fun getDraftRepository(): DraftRepository = getInstance().draftRepository
    }
}
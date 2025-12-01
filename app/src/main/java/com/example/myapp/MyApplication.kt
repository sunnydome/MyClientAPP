package com.example.myapp

import android.app.Application
import com.example.myapp.data.database.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    // Hilt 会自动注入这个实例
    @Inject
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()

        // 数据库预热逻辑保留
        Thread {
            database.openHelper.writableDatabase
        }.start()
    }

}
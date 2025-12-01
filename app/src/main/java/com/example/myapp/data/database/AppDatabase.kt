package com.example.myapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapp.data.database.dao.CommentDao
import com.example.myapp.data.database.dao.DraftDao
import com.example.myapp.data.database.dao.PostDao
import com.example.myapp.data.database.dao.UserDao
import com.example.myapp.data.mock.MockDataProvider
import com.example.myapp.data.model.Comment
import com.example.myapp.data.model.Draft
import com.example.myapp.data.model.Post
import com.example.myapp.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用主数据库
 * 使用Room持久化库
 */
@Database(
    entities = [User::class, Post::class, Comment::class, Draft::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例（单例模式）
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myapp_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 获取已初始化的实例（用于Repository等不方便传Context的地方）
         * 注意：必须在Application中先调用getInstance初始化
         */
        fun getInstanceOrNull(): AppDatabase? = INSTANCE
    }

    /**
     * 数据库创建回调
     * 用于插入初始测试数据
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 在后台线程中插入初始数据
            //INSTANCE?.let { database ->
            //    CoroutineScope(Dispatchers.IO).launch {
            //        populateDatabase(database)
            //    }
            //}
        }

        /**
         * 填充初始测试数据
         */
        private suspend fun populateDatabase(database: AppDatabase) {
            // 插入模拟用户
            //database.userDao().insertAll(MockDataProvider.getMockUsers())

            // 插入模拟帖子
            //database.postDao().insertAll(MockDataProvider.getMockPosts())

            // 插入模拟评论
            //database.commentDao().insertAll(MockDataProvider.getMockComments())
        }
    }
}
package com.example.myapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapp.data.database.AppDatabase
import com.example.myapp.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // 如果需要这里可以注入 Provider<AppDatabase> 来处理循环依赖或回调
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "myapp_database"
        )
            // .addCallback(...) // 如果需要回调，逻辑会稍微复杂一点，通常不再需要手动 populate
            .fallbackToDestructiveMigration()
            .build()
    }

    // 提供各个 DAO，这样 Repository 可以直接注入 DAO 而不是 Database
    @Provides
    fun providePostDao(database: AppDatabase): PostDao = database.postDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideCommentDao(database: AppDatabase): CommentDao = database.commentDao()

    @Provides
    fun provideDraftDao(database: AppDatabase): DraftDao = database.draftDao()
}
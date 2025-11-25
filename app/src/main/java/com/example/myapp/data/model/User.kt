package com.example.myapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户数据模型
 * 用于表示应用中的用户信息
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,

    // 用户名
    val userName: String,

    // 头像URL
    val avatarUrl: String,

    // 个人简介
    val bio: String = "",

    // 关注数
    val followingCount: Int = 0,

    // 粉丝数
    val followerCount: Int = 0,

    // 获赞数
    val likeCount: Int = 0,

    // 是否已关注（当前用户视角）
    val isFollowing: Boolean = false,

    // 创建时间
    val createTime: Long = System.currentTimeMillis()
)
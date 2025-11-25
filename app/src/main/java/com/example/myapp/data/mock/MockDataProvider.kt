package com.example.myapp.data.mock

import com.example.myapp.data.model.Comment
import com.example.myapp.data.model.Post
import com.example.myapp.data.model.User

/**
 * 模拟数据提供者
 * 用于本地开发和测试
 */
object MockDataProvider {

    // 模拟图片URL（使用 picsum.photos 随机图片服务）
    private val mockImageUrls = listOf(
        "https://picsum.photos/seed/1/400/500",
        "https://picsum.photos/seed/2/400/600",
        "https://picsum.photos/seed/3/400/400",
        "https://picsum.photos/seed/4/400/550",
        "https://picsum.photos/seed/5/400/450",
        "https://picsum.photos/seed/6/400/500",
        "https://picsum.photos/seed/7/400/600",
        "https://picsum.photos/seed/8/400/400",
        "https://picsum.photos/seed/9/400/500",
        "https://picsum.photos/seed/10/400/550"
    )

    // 模拟头像URL
    private val mockAvatarUrls = listOf(
        "https://picsum.photos/seed/avatar1/100/100",
        "https://picsum.photos/seed/avatar2/100/100",
        "https://picsum.photos/seed/avatar3/100/100",
        "https://picsum.photos/seed/avatar4/100/100",
        "https://picsum.photos/seed/avatar5/100/100"
    )

    /**
     * 获取模拟用户列表
     */
    fun getMockUsers(): List<User> {
        return listOf(
            User(
                id = "user_1",
                userName = "旅行达人小王",
                avatarUrl = mockAvatarUrls[0],
                bio = "热爱旅行，记录生活中的美好瞬间",
                followingCount = 128,
                followerCount = 5620,
                likeCount = 23500,
                isFollowing = true
            ),
            User(
                id = "user_2",
                userName = "美食家小李",
                avatarUrl = mockAvatarUrls[1],
                bio = "探索城市美食，分享味蕾体验",
                followingCount = 89,
                followerCount = 3200,
                likeCount = 15800,
                isFollowing = true
            ),
            User(
                id = "user_3",
                userName = "摄影师阿明",
                avatarUrl = mockAvatarUrls[2],
                bio = "用镜头捕捉世界的美",
                followingCount = 256,
                followerCount = 12000,
                likeCount = 89000,
                isFollowing = false
            ),
            User(
                id = "user_4",
                userName = "健身教练Lisa",
                avatarUrl = mockAvatarUrls[3],
                bio = "健身爱好者，分享健康生活方式",
                followingCount = 45,
                followerCount = 8900,
                likeCount = 45000,
                isFollowing = false
            ),
            User(
                id = "user_5",
                userName = "穿搭博主小雨",
                avatarUrl = mockAvatarUrls[4],
                bio = "每日穿搭分享，做精致的自己",
                followingCount = 167,
                followerCount = 25000,
                likeCount = 120000,
                isFollowing = true
            ),
            User(
                id = "current_user",
                userName = "我自己",
                avatarUrl = mockAvatarUrls[0],
                bio = "这是我的个人简介",
                followingCount = 50,
                followerCount = 100,
                likeCount = 500,
                isFollowing = false
            )
        )
    }

    /**
     * 获取模拟帖子列表
     */
    fun getMockPosts(): List<Post> {
        val users = getMockUsers()
        val now = System.currentTimeMillis()

        return listOf(
            // 关注分类的帖子
            Post(
                id = "post_1",
                authorId = "user_1",
                authorName = users[0].userName,
                authorAvatar = users[0].avatarUrl,
                title = "周末去了趟杭州西湖",
                content = "天气很好，湖边的风景太美了！推荐大家春天来，柳树发芽的时候最好看。",
                imageUrls = listOf(mockImageUrls[0], mockImageUrls[1], mockImageUrls[2]),
                coverUrl = mockImageUrls[0],
                coverAspectRatio = 0.8f,
                category = "关注",
                tags = listOf("旅行", "杭州", "西湖"),
                location = "杭州·西湖",
                likeCount = 328,
                commentCount = 45,
                collectCount = 89,
                isLiked = true,
                publishTime = now - 3600_000 * 2 // 2小时前
            ),
            Post(
                id = "post_2",
                authorId = "user_2",
                authorName = users[1].userName,
                authorAvatar = users[1].avatarUrl,
                title = "发现一家超好吃的火锅店",
                content = "锅底是秘制的，牛油味道超香！人均80左右，性价比很高。",
                imageUrls = listOf(mockImageUrls[3], mockImageUrls[4]),
                coverUrl = mockImageUrls[3],
                coverAspectRatio = 1.1f,
                category = "关注",
                tags = listOf("美食", "火锅", "探店"),
                location = "北京·朝阳区",
                likeCount = 567,
                commentCount = 123,
                collectCount = 234,
                isLiked = false,
                publishTime = now - 3600_000 * 5 // 5小时前
            ),

            // 发现分类的帖子
            Post(
                id = "post_3",
                authorId = "user_3",
                authorName = users[2].userName,
                authorAvatar = users[2].avatarUrl,
                title = "日落时分的城市天际线，这个环境真是令人心情愉悦，大家一定要来",
                content = "等了两个小时终于拍到了想要的画面，分享给大家。相机参数：f/8, 1/125s, ISO100",
                imageUrls = listOf(mockImageUrls[5]),
                coverUrl = mockImageUrls[5],
                coverAspectRatio = 0.66f,
                category = "发现",
                tags = listOf("摄影", "城市", "日落"),
                location = "上海·陆家嘴",
                likeCount = 2345,
                commentCount = 189,
                collectCount = 567,
                isLiked = true,
                publishTime = now - 3600_000 * 8 // 8小时前
            ),
            Post(
                id = "post_4",
                authorId = "user_4",
                authorName = users[3].userName,
                authorAvatar = users[3].avatarUrl,
                title = "居家健身计划分享",
                content = "不去健身房也能保持好身材！这套动作每天15分钟，坚持一个月效果明显。",
                imageUrls = listOf(mockImageUrls[6], mockImageUrls[7], mockImageUrls[8]),
                coverUrl = mockImageUrls[6],
                coverAspectRatio = 1.2f,
                category = "发现",
                tags = listOf("健身", "居家运动", "塑形"),
                likeCount = 1890,
                commentCount = 256,
                collectCount = 890,
                isLiked = false,
                publishTime = now - 86400_000 // 1天前
            ),
            Post(
                id = "post_5",
                authorId = "user_5",
                authorName = users[4].userName,
                authorAvatar = users[4].avatarUrl,
                title = "秋季穿搭灵感｜温柔知性风",
                content = "今天这套搭配大家喜欢吗？外套是优衣库的，内搭是某宝买的，链接放评论区啦～",
                imageUrls = listOf(mockImageUrls[9], mockImageUrls[0]),
                coverUrl = mockImageUrls[9],
                coverAspectRatio = 0.75f,
                category = "发现",
                tags = listOf("穿搭", "秋装", "知性风"),
                likeCount = 4567,
                commentCount = 678,
                collectCount = 1234,
                isLiked = true,
                publishTime = now - 86400_000 * 2 // 2天前
            ),

            // 同城分类的帖子
            Post(
                id = "post_6",
                authorId = "user_1",
                authorName = users[0].userName,
                authorAvatar = users[0].avatarUrl,
                title = "周末遛娃好去处",
                content = "带孩子来这个公园玩，设施很新，有沙坑、秋千、滑梯，关键是人不多！停车也方便。",
                imageUrls = listOf(mockImageUrls[1], mockImageUrls[2]),
                coverUrl = mockImageUrls[1],
                coverAspectRatio = 0.9f,
                category = "同城",
                tags = listOf("遛娃", "公园", "亲子"),
                location = "深圳·南山区",
                likeCount = 234,
                commentCount = 56,
                collectCount = 123,
                isLiked = false,
                publishTime = now - 3600_000 * 3 // 3小时前
            ),
            Post(
                id = "post_7",
                authorId = "user_2",
                authorName = users[1].userName,
                authorAvatar = users[1].avatarUrl,
                title = "本地人推荐的早茶店",
                content = "这家茶楼开了20多年了，虾饺、叉烧包都很正宗，价格也实惠。周末要早点去，不然要排队！",
                imageUrls = listOf(mockImageUrls[4], mockImageUrls[5], mockImageUrls[6]),
                coverUrl = mockImageUrls[4],
                coverAspectRatio = 1.0f,
                category = "同城",
                tags = listOf("早茶", "粤菜", "老字号"),
                location = "广州·荔湾区",
                likeCount = 890,
                commentCount = 167,
                collectCount = 345,
                isLiked = true,
                publishTime = now - 86400_000 * 3 // 3天前
            )
        )
    }

    /**
     * 获取模拟评论列表
     */
    fun getMockComments(): List<Comment> {
        val users = getMockUsers()
        val now = System.currentTimeMillis()

        return listOf(
            // post_1 的评论
            Comment(
                id = "comment_1",
                postId = "post_1",
                authorId = "user_2",
                authorName = users[1].userName,
                authorAvatar = users[1].avatarUrl,
                content = "西湖真的很美！我上次去的时候赶上下雨，别有一番风味",
                likeCount = 23,
                replyCount = 2,
                createTime = now - 3600_000
            ),
            Comment(
                id = "comment_1_reply_1",
                postId = "post_1",
                authorId = "user_1",
                authorName = users[0].userName,
                authorAvatar = users[0].avatarUrl,
                content = "雨中西湖确实很有意境！",
                parentId = "comment_1",
                replyToName = users[1].userName,
                likeCount = 5,
                createTime = now - 3500_000
            ),
            Comment(
                id = "comment_1_reply_2",
                postId = "post_1",
                authorId = "user_3",
                authorName = users[2].userName,
                authorAvatar = users[2].avatarUrl,
                content = "下次可以带相机去拍",
                parentId = "comment_1",
                replyToName = users[1].userName,
                likeCount = 2,
                createTime = now - 3400_000
            ),
            Comment(
                id = "comment_2",
                postId = "post_1",
                authorId = "user_4",
                authorName = users[3].userName,
                authorAvatar = users[3].avatarUrl,
                content = "请问住宿有推荐吗？",
                likeCount = 8,
                replyCount = 1,
                createTime = now - 7200_000
            ),
            Comment(
                id = "comment_2_reply_1",
                postId = "post_1",
                authorId = "user_1",
                authorName = users[0].userName,
                authorAvatar = users[0].avatarUrl,
                content = "推荐住在断桥附近，早起可以看日出",
                parentId = "comment_2",
                replyToName = users[3].userName,
                likeCount = 12,
                createTime = now - 7000_000
            ),

            // post_3 的评论
            Comment(
                id = "comment_3",
                postId = "post_3",
                authorId = "user_5",
                authorName = users[4].userName,
                authorAvatar = users[4].avatarUrl,
                content = "太美了！这个角度选得真好",
                likeCount = 45,
                createTime = now - 28800_000
            ),
            Comment(
                id = "comment_4",
                postId = "post_3",
                authorId = "user_1",
                authorName = users[0].userName,
                authorAvatar = users[0].avatarUrl,
                content = "请问是用什么镜头拍的？",
                likeCount = 12,
                replyCount = 1,
                createTime = now - 25200_000
            ),
            Comment(
                id = "comment_4_reply_1",
                postId = "post_3",
                authorId = "user_3",
                authorName = users[2].userName,
                authorAvatar = users[2].avatarUrl,
                content = "用的是24-70mm f/2.8，这张是70端拍的",
                parentId = "comment_4",
                replyToName = users[0].userName,
                likeCount = 8,
                createTime = now - 24000_000
            )
        )
    }

    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String = "current_user"

    /**
     * 生成新的帖子ID
     */
    fun generatePostId(): String = "post_${System.currentTimeMillis()}"

    /**
     * 生成新的评论ID
     */
    fun generateCommentId(): String = "comment_${System.currentTimeMillis()}"
}
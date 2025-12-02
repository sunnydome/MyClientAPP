# 🌞 SunnyDome (仿小红书 App)

> 一个基于 Google MAD (Modern Android Development) 架构的现代化社交社区应用。

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg) ![Jetpack](https://img.shields.io/badge/Jetpack-MVVM-green.svg) ![Hilt](https://img.shields.io/badge/DI-Hilt-orange.svg) ![Room](https://img.shields.io/badge/DB-Room-red.svg)

## 📖 项目简介

**SunnyDome** 是一个高仿小红书的 Android 客户端项目，旨在打造流畅、沉浸式的图文内容消费与创作体验。

本项目严格遵循 **Google 推荐架构 (MAD)**，采用 **MVVM** 模式与 **单一数据源 (Single Source of Truth)** 策略。核心亮点在于实现了 **离线优先 (Offline-first)** 的数据同步机制，解决了弱网环境下的浏览体验问题，并深入优化了 **瀑布流布局** 与 **图片手势交互** 的性能。

## ✨ 核心功能

* **首页 Feed 流**：高性能双列瀑布流布局，支持下拉刷新、上拉加载，通过预计算宽高比彻底解决图片高度跳动问题。
* **沉浸式详情页**：支持共享元素转场动画，集成图片轮播、多级评论列表与底部互动栏。
* **内容创作**：支持多图选择、图文编辑，并具备**本地草稿箱**机制，防止用户创作数据意外丢失。
* **图片浏览**：自研 `DragPhotoView`，支持双指缩放及仿微信的**拖拽下滑关闭**交互，背景透明度随手势动态渐变。
* **离线体验**：基于 Room 数据库的全量缓存策略，无网状态下依然可浏览历史内容。


## 🛠 技术栈

* **语言**: [Kotlin](https://kotlinlang.org/) (2.0.21)
* **架构模式**: MVVM, Repository Pattern, Offline-first
* **依赖注入**: [Hilt](https://dagger.dev/hilt/)
* **异步处理**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flow
* **UI 组件**: ViewBinding, RecyclerView (ConcatAdapter, StaggeredGrid), ViewPager2, Material Design 3
* **数据存储**: [Room](https://developer.android.com/training/data-storage/room) (Local), [Retrofit](https://square.github.io/retrofit/) + OkHttp (Network)
* **图片加载**: [Glide](https://bumptech.github.io/glide/)
* **Mock 数据**: [Apifox](https://apifox.com/)

## 🏗 系统架构

本项目采用标准的分层架构，数据流向清晰，易于维护与测试。

```
graph TD
    UI[UI Layer<br/>(Activity/Fragment)] --> VM[Presentation Layer<br/>(ViewModel)]
    VM --> Repo[Data Layer<br/>(Repository)]
    Repo --> Local[Local Source<br/>(Room Database)]
    Repo --> Remote[Remote Source<br/>(Retrofit API)]
```
* **UI Layer**: 负责展示数据，观察 `LiveData` 状态。
* **Data Layer**: `Repository` 作为唯一数据源，协调 `Room` 与 `Network`。
* **同步策略**: 网络请求成功后写入数据库，数据库变动驱动 UI 更新（单一数据源原则），确保数据一致性。

## 🚀 快速开始

### 环境要求
* Android Studio Ladybug | 2024.2.1+
* JDK 17
* Android SDK API 36 (Compile SDK)

### 构建步骤
1.  **克隆仓库**：
    ```bash
    git clone https://github.com/sunnydome/MyClientAPP.git
    cd MyClientAPP
    ```
2.  **导入项目**：
    在 Android Studio 中打开项目根目录，等待 Gradle Sync 完成（项目包含 Gradle Wrapper，会自动下载 Gradle 8.13.1）。
3.  **运行应用**：
    连接真机或模拟器（建议 API 30+ 以获得最佳动画体验），点击运行按钮。

> **注意**：项目目前连接的是 Apifox 云端 Mock 数据，无需配置本地后端即可直接体验完整流程。

## 📂 项目结构

```text
com.example.myapp
├── data                 // 数据层：Entity, DAO, API, Repository
│   ├── database         // Room 数据库实现
│   ├── network          // Retrofit 网络实现
│   └── repository       // 核心仓库层
├── di                   // 依赖注入：Hilt Modules
├── ui                   // 界面层
│   ├── home             // 首页瀑布流 (FeedFragment)
│   ├── post             // 帖子详情 (PostActivity)
│   ├── publish          // 发布与草稿 (PublishActivity)
│   └── imageviewer      // 图片浏览 (DragPhotoView)
└── ...
```

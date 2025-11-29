plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.myapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    // Retrofit 核心库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson 转换器
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp 核心库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // OkHttp 日志拦截器
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    // ==================== 协程 ====================
    // Kotlin 协程核心
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // Kotlin 协程 Android 支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Gson (用于Room类型转换)
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    // ViewModel和LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // CardView (用于图片圆角显示)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // RecyclerView（用于列表/网格）
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment)
    kapt("com.github.bumptech.glide:compiler:5.0.5")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ... 其他依赖
    implementation("androidx.tracing:tracing-ktx:1.2.0")
}
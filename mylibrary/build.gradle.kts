plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.akingyin.mylibrary"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api("com.baidu.lbsyun:BaiduMapSDK_Util:7.6.2")
    api("com.baidu.lbsyun:BaiduMapSDK_Map:7.6.2")
    api("com.baidu.lbsyun:BaiduMapSDK_Search:7.6.2")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.okhttp.core)
    implementation(libs.github.baseRecyclerViewAdapterHelper4)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.android.google.material)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
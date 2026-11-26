plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.daytick"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.daytick"
        minSdk = 26
        targetSdk = 35
        // GitHub Actions 每次編譯的編號當版本號，新版才能覆蓋舊版
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "1.$buildNumber"
    }

    // 簽章金鑰不放在程式碼裡（repository 是公開的）。
    // GitHub Actions 會從 Secrets 還原金鑰並用環境變數傳進來；
    // 本機編譯若沒有金鑰，就自動改用 Android 預設的 debug 金鑰。
    val releaseKeystore = System.getenv("DAYTICK_KEYSTORE_FILE")?.let { file(it) }
    signingConfigs {
        if (releaseKeystore != null && releaseKeystore.exists()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("DAYTICK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DAYTICK_KEY_ALIAS") ?: "daytick"
                keyPassword = System.getenv("DAYTICK_KEY_PASSWORD") ?: System.getenv("DAYTICK_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
}

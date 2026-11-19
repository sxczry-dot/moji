import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.autobook.app"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.autobook.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 152
        versionName = "5.6"
        resConfigs("zh")
    }

    testOptions {
        unitTests {
            // Robolectric 跑 Room 迁移测试需要 Android 资源
            isIncludeAndroidResources = true
        }
    }

    signingConfigs {
        // 继续用 debug 密钥签名发布：已装机的 v5.1 即 debug 签名，
        // 换密钥会导致无法覆盖升级（allowBackup=false，卸载即丢数据）。
        // 同时开启 v3 方案，支持未来密钥轮换。
        named("debug") {
            enableV3Signing = true
        }
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // 实测使用 40+ 个扩展图标（Restaurant/TrendingUp/ReceiptLong 等），
    // icons-core 覆盖不了；R8 会裁剪未用图标，保留 extended
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    // androidx.biometric:1.1.0 会传递引入 appcompat 1.2.0（2021 年），显式提升
    implementation("androidx.appcompat:appcompat:1.8.0")
    // 生物识别：1.1.0 至今仍是最新稳定版（1.4.0 尚在 alpha/beta）
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    // SAF DocumentFile：自动备份写入用户选定目录
    implementation("androidx.documentfile:documentfile:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.8.5")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

ksp {
    // Room schema 导出：直接生成到单测资产目录（KSP 会自动追加数据库类名子目录），
    // Robolectric 迁移测试经 AssetManager 读取
    arg("room.schemaLocation", "$projectDir/src/test/assets")
}


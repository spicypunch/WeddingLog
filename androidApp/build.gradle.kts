import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
val kakaoNativeAppKeySchemeSuffix: String = kakaoNativeAppKey.ifBlank { "disabled" }
val admobAndroidAppId: String = localProperties.getProperty("ADMOB_ANDROID_APP_ID")
    ?.trim()
    .orEmpty()
    .ifBlank { "ca-app-pub-3940256099942544~3347511713" }
val releaseStoreFile: String = localProperties.getProperty("storeFile")?.trim().orEmpty()
val releaseStorePassword: String = localProperties.getProperty("storePassword")?.trim().orEmpty()
val releaseKeyAlias: String = localProperties.getProperty("keyAlias")?.trim().orEmpty()
val releaseKeyPassword: String = localProperties.getProperty("keyPassword")?.trim().orEmpty()
val hasReleaseSigningConfig: Boolean = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isNotBlank() }
val appVersionCode: Int = 3
val appVersionName: String = "1.2.0"

android {
    namespace = "kr.jm.weddingledger"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kr.jm.weddinglog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["kakaoNativeAppKeySchemeSuffix"] = kakaoNativeAppKeySchemeSuffix
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobApplicationId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        getByName("release") {
            isMinifyEnabled = false
            manifestPlaceholders["admobApplicationId"] = admobAndroidAppId
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }

    jvmToolchain(17)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.google.mobile.ads)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.kakao.user)
}

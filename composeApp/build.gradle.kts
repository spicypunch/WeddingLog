import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL") ?: ""
val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY") ?: ""
val firebaseAndroidApplicationId: String = localProperties.getProperty("FIREBASE_ANDROID_APPLICATION_ID") ?: ""
val firebaseProjectId: String = localProperties.getProperty("FIREBASE_PROJECT_ID") ?: ""
val firebaseApiKey: String = localProperties.getProperty("FIREBASE_API_KEY") ?: ""
val firebaseGcmSenderId: String = localProperties.getProperty("FIREBASE_GCM_SENDER_ID") ?: ""
val admobAndroidBannerUnitId: String = localProperties.getProperty("ADMOB_ANDROID_BANNER_UNIT_ID")
    ?.trim()
    .orEmpty()
val admobIosAppId: String = localProperties.getProperty("ADMOB_IOS_APP_ID")
    ?.trim()
    .orEmpty()
val admobIosBannerUnitId: String = localProperties.getProperty("ADMOB_IOS_BANNER_UNIT_ID")
    ?.trim()
    .orEmpty()
val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
val termsOfServiceUrl: String = localProperties.getProperty("TERMS_OF_SERVICE_URL")?.trim().orEmpty()
val privacyPolicyUrl: String = localProperties.getProperty("PRIVACY_POLICY_URL")?.trim().orEmpty()
val kakaoNativeAppKeySchemeSuffix: String = kakaoNativeAppKey.ifBlank { "disabled" }
val enableKakaoLogin: Boolean = localProperties.getProperty("ENABLE_KAKAO_LOGIN")
    ?.trim()
    ?.lowercase()
    ?.toBooleanStrictOrNull()
    ?: true
val enableGoogleLogin: Boolean = localProperties.getProperty("ENABLE_GOOGLE_LOGIN")
    ?.trim()
    ?.lowercase()
    ?.toBooleanStrictOrNull()
    ?: true
val enableAppleLogin: Boolean = localProperties.getProperty("ENABLE_APPLE_LOGIN")
    ?.trim()
    ?.lowercase()
    ?.toBooleanStrictOrNull()
    ?: false
val enableTestAdBanner: Boolean = localProperties.getProperty("ENABLE_TEST_AD_BANNER")
    ?.trim()
    ?.lowercase()
    ?.toBooleanStrictOrNull()
    ?: false
val appVersionCode: Int = 3
val appVersionName: String = "1.2.0"

kotlin {
    androidLibrary {
        namespace = "kr.jm.weddingledger.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        androidResources {
            enable = true
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.google.mobile.ads)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kakao.user)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material) // needed for pull-to-refresh (multiplatform)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Navigation
            implementation(libs.navigation.compose)
            
            // Koin DI
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            
            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            
            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)
            
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            
            // DataStore
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            
            // Supabase
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            
            // Kotlinx Datetime
            implementation(libs.kotlinx.datetime)
        }
        
        commonTest {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
dependencies {
    // Room KSP
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val generateBuildConfig by tasks.registering {
    val outputFile = layout.buildDirectory.file("generated/buildconfig/kr/jm/weddingledger/core/config/BuildConfig.kt")

    inputs.file(localPropertiesFile)
        .optional()
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(outputFile)
    notCompatibleWithConfigurationCache("BuildConfig is generated from local.properties at execution time.")

    doLast {
        val buildConfigFile = outputFile.get().asFile
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText(
            """
            package kr.jm.weddingledger.core.config

            object BuildConfig {
                const val SUPABASE_URL = "$supabaseUrl"
                const val SUPABASE_ANON_KEY = "$supabaseAnonKey"
                const val FIREBASE_ANDROID_APPLICATION_ID = "$firebaseAndroidApplicationId"
                const val FIREBASE_PROJECT_ID = "$firebaseProjectId"
                const val FIREBASE_API_KEY = "$firebaseApiKey"
                const val FIREBASE_GCM_SENDER_ID = "$firebaseGcmSenderId"
                const val ADMOB_ANDROID_BANNER_UNIT_ID = "$admobAndroidBannerUnitId"
                const val ADMOB_IOS_APP_ID = "$admobIosAppId"
                const val ADMOB_IOS_BANNER_UNIT_ID = "$admobIosBannerUnitId"
                const val KAKAO_NATIVE_APP_KEY = "$kakaoNativeAppKey"
                const val TERMS_OF_SERVICE_URL = "$termsOfServiceUrl"
                const val PRIVACY_POLICY_URL = "$privacyPolicyUrl"
                const val ENABLE_KAKAO_LOGIN = $enableKakaoLogin
                const val ENABLE_GOOGLE_LOGIN = $enableGoogleLogin
                const val ENABLE_APPLE_LOGIN = $enableAppleLogin
                const val ENABLE_TEST_AD_BANNER = $enableTestAdBanner
                const val APP_VERSION_NAME = "$appVersionName"
                const val APP_VERSION_CODE = $appVersionCode
            }
            """.trimIndent()
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(layout.buildDirectory.dir("generated/buildconfig"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateBuildConfig)
}

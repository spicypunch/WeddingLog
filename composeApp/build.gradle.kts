import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64
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

val requestedSupabaseEnvironment: String? = providers.gradleProperty("weddingLedgerEnvironment").orNull
    ?: providers.environmentVariable("WEDDINGLEDGER_ENV").orNull
val xcodeConfiguration: String? = providers.environmentVariable("CONFIGURATION").orNull
val isReleaseInvocation: Boolean = xcodeConfiguration
    ?.equals("release", ignoreCase = true) == true ||
    gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
val isDebugInvocation: Boolean = xcodeConfiguration
    ?.equals("debug", ignoreCase = true) == true ||
    gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) }
val hasAmbiguousAggregateTask: Boolean = gradle.startParameter.taskNames.any { taskName ->
    taskName.substringAfterLast(':').lowercase() in setOf("assemble", "build", "bundle")
}
val supabaseEnvironment: String = when (
    requestedSupabaseEnvironment
        ?.trim()
        ?.lowercase()
        ?: if (isReleaseInvocation) "production" else "development"
) {
    "dev", "development" -> "development"
    "prod", "production", "release" -> "production"
    else -> throw GradleException(
        "Unknown Supabase environment. Use development/dev or production/prod."
    )
}
val supabasePropertyPrefix: String = if (supabaseEnvironment == "production") {
    "SUPABASE_PROD"
} else {
    "SUPABASE_DEV"
}
val supabaseUrl: String = localProperties.getProperty("${supabasePropertyPrefix}_URL")
    ?.trim()
    .orEmpty()
val supabaseAnonKey: String = localProperties.getProperty("${supabasePropertyPrefix}_ANON_KEY")
    ?.trim()
    .orEmpty()
val expectedSupabaseProjectRef: String = if (supabaseEnvironment == "production") {
    "wmfzhnoxdmfniewgcmzu"
} else {
    "naxjvhpulhgcnantbsdx"
}
val expectedSupabaseUrl: String = "https://$expectedSupabaseProjectRef.supabase.co"
val supabaseAnonKeyPayload: String? = runCatching {
    val payloadSegment = supabaseAnonKey.split('.').getOrNull(1) ?: error("Not a JWT")
    val paddedPayload = payloadSegment.padEnd(
        payloadSegment.length + (4 - payloadSegment.length % 4) % 4,
        '=',
    )
    Base64.getUrlDecoder().decode(paddedPayload).toString(Charsets.UTF_8)
}.getOrNull()

gradle.taskGraph.whenReady {
    val containsReleaseVariant = isReleaseInvocation ||
        allTasks.any { it.name.endsWith("Release", ignoreCase = true) }
    val containsDebugVariant = isDebugInvocation ||
        allTasks.any { it.name.endsWith("Debug", ignoreCase = true) }

    if (hasAmbiguousAggregateTask) {
        throw GradleException(
            "Aggregate build tasks are disabled because Debug and Release use different Supabase projects. " +
                "Run an explicit variant task such as assembleDebug or bundleRelease."
        )
    }
    if (containsReleaseVariant && containsDebugVariant) {
        throw GradleException(
            "Debug and Release cannot be built in one Gradle invocation because they use different " +
                "Supabase projects. Run the variants separately."
        )
    }
    if (containsReleaseVariant && supabaseEnvironment != "production") {
        throw GradleException(
            "A Release task is present, but Supabase is $supabaseEnvironment."
        )
    }
    if (containsDebugVariant && supabaseEnvironment != "development") {
        throw GradleException(
            "A Debug task is present, but Supabase is $supabaseEnvironment."
        )
    }
}
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
val enableAdMobBanner: Boolean = localProperties.getProperty("ENABLE_ADMOB_BANNER")
    ?.trim()
    ?.lowercase()
    ?.toBooleanStrictOrNull()
    ?: true
val appVersionCode: Int = 10
val appVersionName: String = "1.3.4"

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
    inputs.property("supabaseEnvironment", supabaseEnvironment)
    outputs.file(outputFile)
    notCompatibleWithConfigurationCache("BuildConfig is generated from local.properties at execution time.")

    doFirst {
        if (supabaseUrl != expectedSupabaseUrl) {
            throw GradleException(
                "${supabasePropertyPrefix}_URL must point to $expectedSupabaseProjectRef " +
                    "for the $supabaseEnvironment build."
            )
        }
        val keyProjectRef = supabaseAnonKeyPayload
            ?.let { Regex("\"ref\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
        val keyRole = supabaseAnonKeyPayload
            ?.let { Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
        if (keyProjectRef != expectedSupabaseProjectRef || keyRole != "anon") {
            throw GradleException(
                "${supabasePropertyPrefix}_ANON_KEY must be the anon JWT for " +
                    "$expectedSupabaseProjectRef; got ref=${keyProjectRef ?: "invalid"}, " +
                    "role=${keyRole ?: "invalid"}."
            )
        }
    }

    doLast {
        val buildConfigFile = outputFile.get().asFile
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText(
            """
            package kr.jm.weddingledger.core.config

            object BuildConfig {
                const val SUPABASE_ENVIRONMENT = "$supabaseEnvironment"
                const val SUPABASE_PROJECT_REF = "$expectedSupabaseProjectRef"
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
                const val ENABLE_ADMOB_BANNER = $enableAdMobBanner
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

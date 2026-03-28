package kr.jm.weddingledger.feature.notifications.data.platform

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.network.logSupabaseInfo
import kr.jm.weddingledger.core.network.logSupabaseWarning
import org.koin.mp.KoinPlatformTools

actual suspend fun fetchRemotePushTokenOrNull(): String? {
    val context = resolveAppContext() ?: return null
    if (!ensureFirebaseInitialized(context)) return null

    return suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                logSupabaseInfo(
                    featureName = FEATURE_NAME,
                    message = "remote_push_token_fetch_success hasToken=${!token.isNullOrBlank()} tokenSuffix=${token?.takeLast(8).orEmpty()}",
                )
                if (continuation.isActive) continuation.resume(token?.trim().takeUnless { it.isNullOrEmpty() })
            }
            .addOnFailureListener { throwable ->
                logSupabaseWarning(
                    featureName = FEATURE_NAME,
                    message = "remote_push_token_fetch_failed type=${throwable::class.simpleName}",
                )
                if (continuation.isActive) continuation.resume(null)
            }
    }
}

actual fun currentPushPlatform(): PushPlatform = PushPlatform.Android

private fun ensureFirebaseInitialized(context: Context): Boolean {
    val applicationId = BuildConfig.FIREBASE_ANDROID_APPLICATION_ID.trim()
    val projectId = BuildConfig.FIREBASE_PROJECT_ID.trim()
    val apiKey = BuildConfig.FIREBASE_API_KEY.trim()
    val senderId = BuildConfig.FIREBASE_GCM_SENDER_ID.trim()
    if (applicationId.isBlank() || projectId.isBlank() || apiKey.isBlank() || senderId.isBlank()) {
        logSupabaseWarning(
            featureName = FEATURE_NAME,
            message = "firebase_init_skipped reason=blank_config applicationId=${applicationId.isNotBlank()} projectId=${projectId.isNotBlank()} apiKey=${apiKey.isNotBlank()} senderId=${senderId.isNotBlank()}",
        )
        return false
    }

    val existing = FirebaseApp.getApps(context).firstOrNull()
    if (existing != null) {
        logSupabaseInfo(
            featureName = FEATURE_NAME,
            message = "firebase_already_initialized app=${existing.name}",
        )
        return true
    }

    val options = FirebaseOptions.Builder()
        .setApplicationId(applicationId)
        .setProjectId(projectId)
        .setApiKey(apiKey)
        .setGcmSenderId(senderId)
        .build()

    val initialized = FirebaseApp.initializeApp(context, options) != null
    if (initialized) {
        logSupabaseInfo(
            featureName = FEATURE_NAME,
            message = "firebase_initialized projectId=$projectId senderId=$senderId",
        )
    } else {
        logSupabaseWarning(
            featureName = FEATURE_NAME,
            message = "firebase_init_failed projectId=$projectId",
        )
    }
    return initialized
}

private fun resolveAppContext(): Context? = runCatching {
    KoinPlatformTools.defaultContext().get().get<Context>()
}.getOrNull()

private const val FEATURE_NAME: String = "PartnerPushAndroid"

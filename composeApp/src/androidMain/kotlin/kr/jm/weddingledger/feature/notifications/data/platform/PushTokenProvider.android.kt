package kr.jm.weddingledger.feature.notifications.data.platform

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.jm.weddingledger.core.config.BuildConfig
import org.koin.mp.KoinPlatformTools

actual suspend fun fetchRemotePushTokenOrNull(): String? {
    val context = resolveAppContext() ?: return null
    if (!ensureFirebaseInitialized(context)) return null

    return suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (continuation.isActive) continuation.resume(token?.trim().takeUnless { it.isNullOrEmpty() })
            }
            .addOnFailureListener {
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
        return false
    }

    val existing = FirebaseApp.getApps(context).firstOrNull()
    if (existing != null) return true

    val options = FirebaseOptions.Builder()
        .setApplicationId(applicationId)
        .setProjectId(projectId)
        .setApiKey(apiKey)
        .setGcmSenderId(senderId)
        .build()

    return FirebaseApp.initializeApp(context, options) != null
}

private fun resolveAppContext(): Context? = runCatching {
    KoinPlatformTools.defaultContext().get().get<Context>()
}.getOrNull()

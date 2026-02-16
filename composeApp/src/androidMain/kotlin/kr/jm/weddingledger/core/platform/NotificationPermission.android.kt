package kr.jm.weddingledger.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatformTools

actual suspend fun requestNotificationPermission(): NotificationPermissionRequestResult {
    return AndroidNotificationPermissionRequester.requestPermission()
}

internal object AndroidNotificationPermissionRequester {
    private var launcher: ((String) -> Unit)? = null
    private var pendingContinuation: kotlinx.coroutines.CancellableContinuation<NotificationPermissionRequestResult>? = null

    fun bindLauncher(block: (String) -> Unit) {
        launcher = block
    }

    fun unbindLauncher() {
        launcher = null
        pendingContinuation?.let { continuation ->
            pendingContinuation = null
            continuation.resume(NotificationPermissionRequestResult.Unavailable)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        pendingContinuation?.let { continuation ->
            pendingContinuation = null
            continuation.resume(
                if (granted) {
                    NotificationPermissionRequestResult.Granted
                } else {
                    NotificationPermissionRequestResult.Denied
                }
            )
        }
    }

    suspend fun requestPermission(): NotificationPermissionRequestResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionRequestResult.Granted
        }

        val context = resolveAppContext() ?: return NotificationPermissionRequestResult.Unavailable
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return NotificationPermissionRequestResult.Granted

        val requestLauncher = launcher ?: return NotificationPermissionRequestResult.Unavailable
        return suspendCancellableCoroutine { continuation ->
            pendingContinuation?.let { previous ->
                previous.resume(NotificationPermissionRequestResult.Unavailable)
            }
            pendingContinuation = continuation
            requestLauncher(Manifest.permission.POST_NOTIFICATIONS)
            continuation.invokeOnCancellation {
                if (pendingContinuation === continuation) {
                    pendingContinuation = null
                }
            }
        }
    }

    private fun resolveAppContext(): Context? = runCatching {
        KoinPlatformTools.defaultContext().get().get<Context>()
    }.getOrNull()
}

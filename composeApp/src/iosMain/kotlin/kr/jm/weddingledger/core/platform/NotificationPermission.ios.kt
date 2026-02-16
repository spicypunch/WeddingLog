package kr.jm.weddingledger.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual suspend fun requestNotificationPermission(): NotificationPermissionRequestResult {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    return suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            when (status) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                -> continuation.resume(NotificationPermissionRequestResult.Granted)

                UNAuthorizationStatusDenied -> continuation.resume(NotificationPermissionRequestResult.Denied)
                else -> {
                    center.requestAuthorizationWithOptions(
                        options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
                    ) { granted, _ ->
                        continuation.resume(
                            if (granted) {
                                NotificationPermissionRequestResult.Granted
                            } else {
                                NotificationPermissionRequestResult.Denied
                            }
                        )
                    }
                }
            }
        }
    }
}

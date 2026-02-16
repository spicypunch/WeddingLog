package kr.jm.weddingledger.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.time.Clock

actual suspend fun scheduleLocalNotification(
    payload: LocalNotificationPayload,
): LocalNotificationScheduleResult {
    val id = payload.id.trim()
    val title = payload.title.trim()
    val body = payload.body.trim()
    if (id.isBlank() || title.isBlank() || body.isBlank()) {
        return LocalNotificationScheduleResult.InvalidInput
    }

    val center = UNUserNotificationCenter.currentNotificationCenter()
    val granted = ensurePermission(center)
    if (!granted) return LocalNotificationScheduleResult.PermissionDenied

    val now = Clock.System.now().toEpochMilliseconds()
    val delaySeconds = max(payload.triggerAtEpochMillis, now + 1_000L)
        .minus(now)
        .toDouble()
        .div(1_000.0)
        .coerceAtLeast(1.0)

    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound())
    }

    val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
        timeInterval = delaySeconds,
        repeats = false,
    )
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = id,
        content = content,
        trigger = trigger,
    )

    return suspendCancellableCoroutine { cont ->
        center.addNotificationRequest(request) { error ->
            cont.resume(
                if (error == null) LocalNotificationScheduleResult.Scheduled else LocalNotificationScheduleResult.Failed
            )
        }
    }
}

actual suspend fun cancelLocalNotification(
    id: String,
): Boolean {
    val reminderId = id.trim()
    if (reminderId.isBlank()) return false

    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.removePendingNotificationRequestsWithIdentifiers(listOf(reminderId))
    center.removeDeliveredNotificationsWithIdentifiers(listOf(reminderId))
    return true
}

private suspend fun ensurePermission(
    center: UNUserNotificationCenter,
): Boolean = suspendCancellableCoroutine { cont ->
    center.getNotificationSettingsWithCompletionHandler { settings ->
        val status = settings?.authorizationStatus
        when (status) {
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            -> cont.resume(true)

            UNAuthorizationStatusDenied -> cont.resume(false)

            else -> {
                center.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
                ) { granted, _ ->
                    cont.resume(granted)
                }
            }
        }
    }
}

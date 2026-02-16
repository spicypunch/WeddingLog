package kr.jm.weddingledger.core.platform

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.koin.mp.KoinPlatformTools

actual suspend fun scheduleLocalNotification(
    payload: LocalNotificationPayload,
): LocalNotificationScheduleResult {
    val context = resolveAppContext() ?: return LocalNotificationScheduleResult.Failed

    val title = payload.title.trim()
    val body = payload.body.trim()
    val id = payload.id.trim()
    if (id.isBlank() || title.isBlank() || body.isBlank()) {
        return LocalNotificationScheduleResult.InvalidInput
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return LocalNotificationScheduleResult.PermissionDenied
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        ?: return LocalNotificationScheduleResult.Failed

    LocalNotificationAndroid.ensureChannel(context)

    val triggerAt = payload.triggerAtEpochMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L)
    val pendingIntent = reminderPendingIntent(
        context = context,
        reminderId = id,
        title = title,
        body = body,
    )

    val scheduled = runCatching {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }

            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        }
    }.recoverCatching {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent,
        )
    }.isSuccess

    return if (scheduled) LocalNotificationScheduleResult.Scheduled else LocalNotificationScheduleResult.Failed
}

actual suspend fun cancelLocalNotification(
    id: String,
): Boolean {
    val context = resolveAppContext() ?: return false
    val reminderId = id.trim()
    if (reminderId.isBlank()) return false

    val pendingIntent = reminderPendingIntent(
        context = context,
        reminderId = reminderId,
        title = "",
        body = "",
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    runCatching { alarmManager?.cancel(pendingIntent) }
    LocalNotificationAndroid.cancelDelivered(context, reminderId)
    return true
}

private fun resolveAppContext(): Context? = runCatching {
    KoinPlatformTools.defaultContext().get().get<Context>()
}.getOrNull()

private fun reminderPendingIntent(
    context: Context,
    reminderId: String,
    title: String,
    body: String,
): PendingIntent {
    val intent = Intent(context, WeddingLedgerReminderReceiver::class.java).apply {
        action = LocalNotificationAndroid.actionTriggerReminder
        putExtra(LocalNotificationAndroid.extraReminderId, reminderId)
        putExtra(LocalNotificationAndroid.extraReminderTitle, title)
        putExtra(LocalNotificationAndroid.extraReminderBody, body)
    }

    return PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

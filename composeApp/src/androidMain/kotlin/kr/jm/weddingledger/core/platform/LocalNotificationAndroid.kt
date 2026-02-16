package kr.jm.weddingledger.core.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

internal object LocalNotificationAndroid {
    const val channelId: String = "weddingledger_reminders"
    const val channelName: String = "웨딩 리마인더"
    const val channelDescription: String = "WeddingLedger 일정/체크리스트 리마인더"

    const val actionTriggerReminder: String = "kr.jm.weddingledger.ACTION_TRIGGER_REMINDER"
    const val extraReminderId: String = "extra_reminder_id"
    const val extraReminderTitle: String = "extra_reminder_title"
    const val extraReminderBody: String = "extra_reminder_body"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = channelDescription
        }

        manager.createNotificationChannel(channel)
    }

    fun cancelDelivered(context: Context, reminderId: String) {
        NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
    }
}

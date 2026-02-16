package kr.jm.weddingledger.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class WeddingLedgerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocalNotificationAndroid.actionTriggerReminder) return

        val reminderId = intent.getStringExtra(LocalNotificationAndroid.extraReminderId).orEmpty()
        if (reminderId.isBlank()) return

        val title = intent.getStringExtra(LocalNotificationAndroid.extraReminderTitle)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "웨딩 리마인더"

        val body = intent.getStringExtra(LocalNotificationAndroid.extraReminderBody)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "확인할 일정이 있어요."

        LocalNotificationAndroid.ensureChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                reminderId.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, LocalNotificationAndroid.channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
        }
    }
}

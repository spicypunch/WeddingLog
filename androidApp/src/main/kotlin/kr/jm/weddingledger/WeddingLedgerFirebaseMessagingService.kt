package kr.jm.weddingledger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier
import org.koin.core.context.GlobalContext

class WeddingLedgerFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (token.isBlank()) {
            Log.w(LOG_TAG, "[WARN][PushReceiver] onNewToken blank_token")
            return
        }
        Log.i(LOG_TAG, "[INFO][PushReceiver] onNewToken tokenSuffix=${token.takeLast(8)}")
        serviceScope.launch {
            runCatching {
                GlobalContext.get().get<PartnerActivityNotifier>().syncCurrentDevicePushToken()
            }.onFailure { throwable ->
                Log.e(LOG_TAG, "[ERROR][PushReceiver] onNewToken sync_failed type=${throwable::class.simpleName}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notificationTitle = message.notification?.title.orEmpty()
        val notificationBody = message.notification?.body.orEmpty()
        Log.i(
            LOG_TAG,
            "[INFO][PushReceiver] onMessageReceived from=${message.from.orEmpty()} hasNotification=${message.notification != null} dataKeys=${message.data.keys.joinToString(",")} title=${notificationTitle.take(40)} body=${notificationBody.take(60)}",
        )

        showPartnerActivityNotification(
            title = notificationTitle.ifBlank { "새 활동이 있어요" },
            body = notificationBody.ifBlank { "파트너가 새로운 내용을 추가했어요." },
            notificationId = message.messageId ?: "${message.from.orEmpty()}-${System.currentTimeMillis()}",
        )
    }

    private fun showPartnerActivityNotification(
        title: String,
        body: String,
        notificationId: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(LOG_TAG, "[WARN][PushReceiver] notification_display_skipped reason=permission_denied")
                return
            }
        }

        ensurePartnerActivityChannel()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                notificationId.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(notificationId.hashCode(), notification)
            Log.i(LOG_TAG, "[INFO][PushReceiver] notification_displayed id=${notificationId.takeLast(8)}")
        }.onFailure { throwable ->
            Log.e(
                LOG_TAG,
                "[ERROR][PushReceiver] notification_display_failed type=${throwable::class.simpleName}",
            )
        }
    }

    private fun ensurePartnerActivityChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}

private const val LOG_TAG: String = "WL_SUPABASE"
private const val CHANNEL_ID: String = "weddingledger_partner_activity"
private const val CHANNEL_NAME: String = "파트너 활동 알림"
private const val CHANNEL_DESCRIPTION: String = "WeddingLedger 파트너 일정/지출 활동 알림"

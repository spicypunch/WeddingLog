package kr.jm.weddingledger.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.koin.mp.KoinPlatformTools

actual fun openAppNotificationSettings(): Boolean {
    val context = runCatching {
        KoinPlatformTools.defaultContext().get().get<Context>()
    }.getOrNull() ?: return false

    val packageName = context.packageName
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra("android.provider.extra.APP_PACKAGE", packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val opened = runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
    if (opened) return true

    val fallbackIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(fallbackIntent)
        true
    }.getOrDefault(false)
}

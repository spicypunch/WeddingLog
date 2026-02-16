package kr.jm.weddingledger.core.platform

import android.content.Context
import android.content.Intent
import org.koin.mp.KoinPlatformTools

actual fun shareText(text: String) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return

    val context = runCatching {
        KoinPlatformTools.defaultContext().get().get<Context>()
    }.getOrNull() ?: return

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, trimmed)
        type = "text/plain"
    }

    val chooser = Intent.createChooser(sendIntent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { context.startActivity(chooser) }
}

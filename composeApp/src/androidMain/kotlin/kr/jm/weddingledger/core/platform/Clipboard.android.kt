package kr.jm.weddingledger.core.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.koin.mp.KoinPlatformTools

actual fun copyText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false

    val context = runCatching {
        KoinPlatformTools.defaultContext().get().get<Context>()
    }.getOrNull() ?: return false

    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", trimmed))
        true
    }.getOrDefault(false)
}

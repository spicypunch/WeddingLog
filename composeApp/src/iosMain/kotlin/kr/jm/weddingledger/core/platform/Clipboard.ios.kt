package kr.jm.weddingledger.core.platform

import platform.UIKit.UIPasteboard

actual fun copyText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false

    return runCatching {
        UIPasteboard.generalPasteboard.string = trimmed
        true
    }.getOrDefault(false)
}

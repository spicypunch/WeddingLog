package kr.jm.weddingledger.core.logging

import android.util.Log

internal actual fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
) {
    when (level) {
        LogLevel.Info -> Log.i(tag, message)
        LogLevel.Warning -> Log.w(tag, message)
        LogLevel.Error -> Log.e(tag, message)
    }
}

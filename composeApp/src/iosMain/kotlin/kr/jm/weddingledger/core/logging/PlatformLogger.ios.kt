package kr.jm.weddingledger.core.logging

import platform.Foundation.NSLog

internal actual fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
) {
    NSLog(buildLogMessage(tag = tag, level = level, message = message).escapePercentForFormat())
}

private fun buildLogMessage(
    tag: String,
    level: LogLevel,
    message: String,
): String = "[$tag][${level.name}] $message"

private fun String.escapePercentForFormat(): String = replace("%", "%%")

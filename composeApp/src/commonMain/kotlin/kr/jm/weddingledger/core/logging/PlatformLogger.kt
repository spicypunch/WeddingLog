package kr.jm.weddingledger.core.logging

internal enum class LogLevel {
    Info,
    Warning,
    Error,
}

internal expect fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
)

package kr.jm.weddingledger.core.platform

enum class NotificationPermissionRequestResult {
    Granted,
    Denied,
    Unavailable,
}

expect suspend fun requestNotificationPermission(): NotificationPermissionRequestResult

package kr.jm.weddingledger.core.platform

data class LocalNotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val triggerAtEpochMillis: Long,
)

enum class LocalNotificationScheduleResult {
    Scheduled,
    PermissionDenied,
    InvalidInput,
    Failed,
}

expect suspend fun scheduleLocalNotification(
    payload: LocalNotificationPayload,
): LocalNotificationScheduleResult

expect suspend fun cancelLocalNotification(
    id: String,
): Boolean

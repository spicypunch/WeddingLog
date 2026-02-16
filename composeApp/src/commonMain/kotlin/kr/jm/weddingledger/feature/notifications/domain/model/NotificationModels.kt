package kr.jm.weddingledger.feature.notifications.domain.model

enum class NotificationItemType {
    Schedule,
    Checklist,
    BudgetWarning,
}

data class NotificationReminderPlan(
    val notificationId: String,
    val title: String,
    val body: String,
    val triggerAtEpochMillis: Long,
)

data class NotificationFeedItem(
    val id: String,
    val type: NotificationItemType,
    val title: String,
    val subtitle: String,
    val detail: String,
    val sortKeyEpochMillis: Long,
    val reminderPlan: NotificationReminderPlan? = null,
)

enum class NotificationCenterErrorCode {
    Unknown,
    PermissionDenied,
    InvalidInput,
}

sealed interface NotificationCenterResult<out T> {
    data class Success<T>(val data: T) : NotificationCenterResult<T>
    data class Error(
        val message: String,
        val code: NotificationCenterErrorCode = NotificationCenterErrorCode.Unknown,
    ) : NotificationCenterResult<Nothing>
}

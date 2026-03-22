package kr.jm.weddingledger.feature.notifications.presentation

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType

data class NotificationCenterUiState(
    val title: String,
    val isLoading: Boolean,
    val activityItems: List<NotificationItemUi>,
    val reminderItems: List<NotificationItemUi>,
    val message: String?,
    val showPermissionAction: Boolean,
) {
    companion object {
        fun initial(): NotificationCenterUiState = NotificationCenterUiState(
            title = "알림센터",
            isLoading = true,
            activityItems = emptyList(),
            reminderItems = emptyList(),
            message = null,
            showPermissionAction = false,
        )
    }
}

data class NotificationItemUi(
    val id: String,
    val type: NotificationItemType,
    val title: String,
    val subtitle: String,
    val detail: String,
    val canScheduleReminder: Boolean,
    val isReminderScheduled: Boolean,
    val isUnread: Boolean,
)

sealed interface NotificationCenterAction {
    data object OnRefresh : NotificationCenterAction
    data class OnClickScheduleReminder(val itemId: String) : NotificationCenterAction
    data class OnClickCancelReminder(val itemId: String) : NotificationCenterAction
    data object OnClickRequestPermission : NotificationCenterAction
    data object OnDismissMessage : NotificationCenterAction
}

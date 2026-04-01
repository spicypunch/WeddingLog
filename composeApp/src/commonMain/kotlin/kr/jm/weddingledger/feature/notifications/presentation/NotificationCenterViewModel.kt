package kr.jm.weddingledger.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.core.platform.NotificationPermissionRequestResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterErrorCode
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedSection
import kr.jm.weddingledger.feature.notifications.domain.usecase.CancelReminderUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchNotificationFeedUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchScheduledReminderIdsUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.MarkActivityItemsReadUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.ScheduleReminderUseCase

class NotificationCenterViewModel(
    private val fetchNotificationFeedUseCase: FetchNotificationFeedUseCase,
    private val fetchScheduledReminderIdsUseCase: FetchScheduledReminderIdsUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase,
    private val markActivityItemsReadUseCase: MarkActivityItemsReadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationCenterUiState.initial())
    val state: StateFlow<NotificationCenterUiState> = _state.asStateFlow()

    private var feedItems: List<NotificationFeedItem> = emptyList()
    private val scheduledReminderIds = mutableSetOf<String>()
    private var pendingPermissionItemId: String? = null

    init {
        refresh()
    }

    fun onAction(action: NotificationCenterAction) {
        when (action) {
            NotificationCenterAction.OnRefresh -> refresh()
            NotificationCenterAction.OnDismissMessage -> {
                _state.update { it.copy(message = null, showPermissionAction = false) }
            }
            is NotificationCenterAction.OnClickActivityItem -> markActivityItemRead(action.itemId)
            is NotificationCenterAction.OnClickScheduleReminder -> scheduleReminder(action.itemId)
            is NotificationCenterAction.OnClickCancelReminder -> cancelReminder(action.itemId)
            NotificationCenterAction.OnClickRequestPermission -> Unit
        }
    }

    fun onNotificationPermissionResult(result: NotificationPermissionRequestResult) {
        when (result) {
            NotificationPermissionRequestResult.Granted -> {
                val pendingItemId = pendingPermissionItemId
                pendingPermissionItemId = null
                _state.update {
                    it.copy(
                        message = "알림 권한을 허용했습니다.",
                        showPermissionAction = false,
                    )
                }
                if (pendingItemId != null) {
                    scheduleReminder(pendingItemId)
                }
            }

            NotificationPermissionRequestResult.Denied -> {
                _state.update {
                    it.copy(
                        message = "알림 권한이 거부되었습니다. 기기 설정에서 알림 권한을 허용해주세요.",
                        showPermissionAction = true,
                    )
                }
            }

            NotificationPermissionRequestResult.Unavailable -> {
                _state.update {
                    it.copy(
                        message = "현재 환경에서는 권한 요청을 바로 실행할 수 없습니다.",
                        showPermissionAction = false,
                    )
                }
            }
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true, message = null, showPermissionAction = false) }

        viewModelScope.launch {
            when (val feedResult = fetchNotificationFeedUseCase()) {
                is NotificationCenterResult.Success -> {
                    feedItems = feedResult.data
                    when (val reminderIdsResult = fetchScheduledReminderIdsUseCase()) {
                        is NotificationCenterResult.Success -> {
                            scheduledReminderIds.clear()
                            scheduledReminderIds += reminderIdsResult.data
                        }

                        is NotificationCenterResult.Error -> Unit
                    }
                    _state.update {
                        val activityItems = feedItems
                            .filter { item -> item.section == NotificationFeedSection.Activity }
                            .map(::toUiItem)
                        val reminderItems = feedItems
                            .filter { item -> item.section == NotificationFeedSection.Reminder }
                            .map(::toUiItem)
                        it.copy(
                            isLoading = false,
                            activityItems = activityItems,
                            reminderItems = reminderItems,
                            message = null,
                            showPermissionAction = false,
                        )
                    }
                }

                is NotificationCenterResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            activityItems = emptyList(),
                            reminderItems = emptyList(),
                            message = feedResult.message,
                            showPermissionAction = false,
                        )
                    }
                }
            }
        }
    }

    private fun toUiItem(item: NotificationFeedItem): NotificationItemUi {
        val reminderId = item.reminderPlan?.notificationId
        return NotificationItemUi(
            id = item.id,
            type = item.type,
            title = item.title,
            subtitle = item.subtitle,
            detail = item.detail,
            canScheduleReminder = item.reminderPlan != null,
            isReminderScheduled = reminderId != null && scheduledReminderIds.contains(reminderId),
            isUnread = item.isUnread,
        )
    }

    private fun markActivityItemRead(itemId: String) {
        val target = feedItems.firstOrNull { it.id == itemId } ?: return
        if (target.section != NotificationFeedSection.Activity || !target.isUnread) return

        updateActivityUnread(itemId = itemId, isUnread = false)

        viewModelScope.launch {
            when (val result = markActivityItemsReadUseCase(listOf(itemId))) {
                is NotificationCenterResult.Success -> Unit
                is NotificationCenterResult.Error -> {
                    updateActivityUnread(itemId = itemId, isUnread = true)
                    _state.update {
                        it.copy(
                            message = result.message,
                            showPermissionAction = false,
                        )
                    }
                }
            }
        }
    }

    private fun updateActivityUnread(itemId: String, isUnread: Boolean) {
        feedItems = feedItems.map { item ->
            if (item.id == itemId) {
                item.copy(isUnread = isUnread)
            } else {
                item
            }
        }
        _state.update {
            it.copy(
                activityItems = it.activityItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(isUnread = isUnread)
                    } else {
                        item
                    }
                }
            )
        }
    }

    private fun scheduleReminder(itemId: String) {
        val target = feedItems.firstOrNull { it.id == itemId } ?: return
        val reminder = target.reminderPlan ?: run {
            _state.update { it.copy(message = "이 항목은 리마인더를 예약할 수 없습니다.", showPermissionAction = false) }
            return
        }
        val reminderId = reminder.notificationId
        if (scheduledReminderIds.contains(reminderId)) {
            _state.update { it.copy(message = "이미 리마인더를 예약했어요.") }
            return
        }

        viewModelScope.launch {
            when (val result = scheduleReminderUseCase(reminder)) {
                is NotificationCenterResult.Success -> {
                    scheduledReminderIds += reminderId
                    pendingPermissionItemId = null
                    _state.update {
                        it.copy(
                            reminderItems = it.reminderItems.map { item ->
                                if (item.id == itemId) item.copy(isReminderScheduled = true) else item
                            },
                            message = "리마인더를 예약했습니다.",
                            showPermissionAction = false,
                        )
                    }
                }

                is NotificationCenterResult.Error -> {
                    val requiresPermission = result.code == NotificationCenterErrorCode.PermissionDenied
                    if (requiresPermission) {
                        pendingPermissionItemId = itemId
                    }
                    _state.update {
                        it.copy(
                            message = result.message,
                            showPermissionAction = requiresPermission,
                        )
                    }
                }
            }
        }
    }

    private fun cancelReminder(itemId: String) {
        val target = feedItems.firstOrNull { it.id == itemId } ?: return
        val reminderId = target.reminderPlan?.notificationId ?: run {
            _state.update { it.copy(message = "취소할 리마인더가 없습니다.", showPermissionAction = false) }
            return
        }

        viewModelScope.launch {
            when (val result = cancelReminderUseCase(reminderId)) {
                is NotificationCenterResult.Success -> {
                    scheduledReminderIds.remove(reminderId)
                    _state.update {
                        it.copy(
                            reminderItems = it.reminderItems.map { item ->
                                if (item.id == itemId) item.copy(isReminderScheduled = false) else item
                            },
                            message = "리마인더를 취소했습니다.",
                            showPermissionAction = false,
                        )
                    }
                }

                is NotificationCenterResult.Error -> {
                    _state.update { it.copy(message = result.message, showPermissionAction = false) }
                }
            }
        }
    }
}

package kr.jm.weddingledger.feature.notifications.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.core.platform.NotificationPermissionRequestResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterErrorCode
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedSection
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository
import kr.jm.weddingledger.feature.notifications.domain.usecase.CancelReminderUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchNotificationFeedUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.FetchScheduledReminderIdsUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.MarkActivityItemsReadUseCase
import kr.jm.weddingledger.feature.notifications.domain.usecase.ScheduleReminderUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class NotificationCenterViewModelTest {

    @Test
    fun `initial load reflects scheduled reminder ids`() = runMainDispatcherTest {
        val repository = FakeNotificationRepositoryForViewModel(
            feedItems = listOf(reminderFeedItem(id = "item-1", reminderId = "event-reminder-1")),
            scheduledIds = mutableSetOf("event-reminder-1"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.activityItems.size)
        assertEquals(1, state.reminderItems.size)
        assertEquals(true, state.reminderItems.first().isReminderScheduled)
    }

    @Test
    fun `permission denied then granted retries reminder scheduling`() = runMainDispatcherTest {
        val repository = FakeNotificationRepositoryForViewModel(
            feedItems = listOf(reminderFeedItem(id = "item-1", reminderId = "event-reminder-1")),
            scheduledIds = mutableSetOf(),
        ).apply {
            scheduleResults += NotificationCenterResult.Error(
                message = "권한이 필요합니다.",
                code = NotificationCenterErrorCode.PermissionDenied,
            )
            scheduleResults += NotificationCenterResult.Success(Unit)
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationCenterAction.OnClickScheduleReminder("item-1"))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(true, state.showPermissionAction)
        assertEquals("권한이 필요합니다.", state.message)

        viewModel.onNotificationPermissionResult(NotificationPermissionRequestResult.Granted)
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(false, state.showPermissionAction)
        assertEquals(true, state.reminderItems.first().isReminderScheduled)
        assertTrue(repository.scheduledIds.contains("event-reminder-1"))
    }

    @Test
    fun `cancel reminder updates scheduled flag`() = runMainDispatcherTest {
        val repository = FakeNotificationRepositoryForViewModel(
            feedItems = listOf(reminderFeedItem(id = "item-1", reminderId = "event-reminder-1")),
            scheduledIds = mutableSetOf("event-reminder-1"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationCenterAction.OnClickCancelReminder("item-1"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.reminderItems.first().isReminderScheduled)
        assertEquals("리마인더를 취소했습니다.", state.message)
    }

    @Test
    fun `activity item is marked read only after click`() = runMainDispatcherTest {
        val repository = FakeNotificationRepositoryForViewModel(
            feedItems = listOf(
                NotificationFeedItem(
                    id = "activity-1",
                    type = NotificationItemType.PartnerExpenseAdded,
                    section = NotificationFeedSection.Activity,
                    title = "새 활동",
                    subtitle = "방금 추가됨",
                    detail = "지출이 등록됐어요",
                    sortKeyEpochMillis = 1L,
                    isUnread = true,
                )
            ),
            scheduledIds = mutableSetOf(),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.activityItems.first().isUnread)
        assertEquals(emptyList(), repository.markedReadIds)

        viewModel.onAction(NotificationCenterAction.OnClickActivityItem("activity-1"))
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.activityItems.first().isUnread)
        assertEquals(listOf("activity-1"), repository.markedReadIds)
    }

    @Test
    fun `mark read failure restores unread state and shows message`() = runMainDispatcherTest {
        val repository = FakeNotificationRepositoryForViewModel(
            feedItems = listOf(
                NotificationFeedItem(
                    id = "activity-1",
                    type = NotificationItemType.PartnerExpenseAdded,
                    section = NotificationFeedSection.Activity,
                    title = "새 활동",
                    subtitle = "방금 추가됨",
                    detail = "지출이 등록됐어요",
                    sortKeyEpochMillis = 1L,
                    isUnread = true,
                )
            ),
            scheduledIds = mutableSetOf(),
        ).apply {
            markReadResults += NotificationCenterResult.Error("읽음 처리에 실패했습니다.")
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(NotificationCenterAction.OnClickActivityItem("activity-1"))
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.activityItems.first().isUnread)
        assertEquals("읽음 처리에 실패했습니다.", viewModel.state.value.message)
        assertEquals(listOf("activity-1"), repository.markedReadIds)
    }
}

private fun createViewModel(repository: FakeNotificationRepositoryForViewModel): NotificationCenterViewModel {
    return NotificationCenterViewModel(
        fetchNotificationFeedUseCase = FetchNotificationFeedUseCase(repository),
        fetchScheduledReminderIdsUseCase = FetchScheduledReminderIdsUseCase(repository),
        scheduleReminderUseCase = ScheduleReminderUseCase(repository),
        cancelReminderUseCase = CancelReminderUseCase(repository),
        markActivityItemsReadUseCase = MarkActivityItemsReadUseCase(repository),
    )
}

private class FakeNotificationRepositoryForViewModel(
    private val feedItems: List<NotificationFeedItem>,
    val scheduledIds: MutableSet<String>,
) : NotificationCenterRepository {
    val scheduleResults = mutableListOf<NotificationCenterResult<Unit>>()
    val markReadResults = mutableListOf<NotificationCenterResult<Unit>>()
    val markedReadIds = mutableListOf<String>()

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        return NotificationCenterResult.Success(feedItems)
    }

    override suspend fun markActivityItemsRead(notificationIds: List<String>): NotificationCenterResult<Unit> {
        markedReadIds += notificationIds
        return markReadResults.removeFirstOrNull() ?: NotificationCenterResult.Success(Unit)
    }

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        val next = scheduleResults.removeFirstOrNull() ?: NotificationCenterResult.Success(Unit)
        return when (next) {
            is NotificationCenterResult.Success -> {
                scheduledIds += plan.notificationId
                NotificationCenterResult.Success(Unit)
            }
            is NotificationCenterResult.Error -> next
        }
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        scheduledIds.remove(notificationId)
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(scheduledIds)
    }
}

private fun reminderFeedItem(
    id: String,
    reminderId: String,
): NotificationFeedItem {
    return NotificationFeedItem(
        id = id,
        type = NotificationItemType.Schedule,
        section = NotificationFeedSection.Reminder,
        title = "일정 알림",
        subtitle = "내일 일정",
        detail = "일정이 있습니다",
        sortKeyEpochMillis = 1L,
        reminderPlan = NotificationReminderPlan(
            notificationId = reminderId,
            title = "일정 알림",
            body = "일정이 있습니다",
            triggerAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds() + 60_000L,
        ),
    )
}

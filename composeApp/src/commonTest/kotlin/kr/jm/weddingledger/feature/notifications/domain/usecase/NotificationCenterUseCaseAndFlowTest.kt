package kr.jm.weddingledger.feature.notifications.domain.usecase

import kotlin.time.Clock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class NotificationCenterUseCaseAndFlowTest {

    @Test
    fun `fetch notification feed returns repository result`() = runBlocking {
        val repository = InMemoryNotificationCenterRepository()
        val useCase = FetchNotificationFeedUseCase(repository)

        val result = useCase()

        val success = assertIs<NotificationCenterResult.Success<List<NotificationFeedItem>>>(result)
        assertEquals(2, success.data.size)
        assertEquals(1, repository.fetchFeedCallCount)
    }

    @Test
    fun `integration flow schedule and cancel reminder`() = runBlocking {
        val repository = InMemoryNotificationCenterRepository()
        val scheduleUseCase = ScheduleReminderUseCase(repository)
        val cancelUseCase = CancelReminderUseCase(repository)
        val fetchScheduledUseCase = FetchScheduledReminderIdsUseCase(repository)

        val plan = NotificationReminderPlan(
            notificationId = "event-reminder-1",
            title = "스튜디오 촬영",
            body = "내일 촬영 일정이 있어요",
            triggerAtEpochMillis = Clock.System.now().toEpochMilliseconds() + 60_000L,
        )

        scheduleUseCase(plan)
        var scheduled = assertIs<NotificationCenterResult.Success<Set<String>>>(fetchScheduledUseCase()).data
        assertEquals(setOf("event-reminder-1"), scheduled)

        cancelUseCase("event-reminder-1")
        scheduled = assertIs<NotificationCenterResult.Success<Set<String>>>(fetchScheduledUseCase()).data
        assertEquals(emptySet(), scheduled)
    }
}

private class InMemoryNotificationCenterRepository : NotificationCenterRepository {
    private val feed = listOf(
        NotificationFeedItem(
            id = "feed-1",
            type = NotificationItemType.Schedule,
            title = "다가오는 일정",
            subtitle = "스튜디오 촬영",
            detail = "내일 오후 2시",
            sortKeyEpochMillis = 1_000L,
            reminderPlan = NotificationReminderPlan(
                notificationId = "event-reminder-1",
                title = "스튜디오 촬영",
                body = "내일 촬영 일정이 있어요",
                triggerAtEpochMillis = Clock.System.now().toEpochMilliseconds() + 86_400_000L,
            ),
        ),
        NotificationFeedItem(
            id = "feed-2",
            type = NotificationItemType.Checklist,
            title = "체크리스트",
            subtitle = "청첩장 전달",
            detail = "이번 주말까지 완료",
            sortKeyEpochMillis = 900L,
            reminderPlan = null,
        ),
    )

    private val scheduledIds = linkedSetOf<String>()

    var fetchFeedCallCount: Int = 0

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        fetchFeedCallCount += 1
        return NotificationCenterResult.Success(feed)
    }

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        scheduledIds += plan.notificationId
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        scheduledIds.remove(notificationId)
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(scheduledIds.toSet())
    }
}

package kr.jm.weddingledger.feature.notifications.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class ScheduleReminderUseCaseTest {

    @Test
    fun `returns error when title is blank`() = runBlocking {
        val repository = FakeNotificationCenterRepository()
        val useCase = ScheduleReminderUseCase(repository)

        val result = useCase(
            NotificationReminderPlan(
                notificationId = "n-1",
                title = " ",
                body = "본문",
                triggerAtEpochMillis = Clock.System.now().toEpochMilliseconds() + 60_000L,
            )
        )

        val error = assertIs<NotificationCenterResult.Error>(result)
        assertEquals("알림 제목/내용이 비어 있습니다.", error.message)
        assertEquals(0, repository.scheduleCallCount)
    }

    @Test
    fun `returns error when trigger time is past`() = runBlocking {
        val repository = FakeNotificationCenterRepository()
        val useCase = ScheduleReminderUseCase(repository)

        val result = useCase(
            NotificationReminderPlan(
                notificationId = "n-2",
                title = "내일 일정",
                body = "일정 확인",
                triggerAtEpochMillis = Clock.System.now().toEpochMilliseconds() - 1L,
            )
        )

        val error = assertIs<NotificationCenterResult.Error>(result)
        assertEquals("예약 시간이 이미 지났습니다.", error.message)
        assertEquals(0, repository.scheduleCallCount)
    }

    @Test
    fun `delegates to repository when reminder is valid`() = runBlocking {
        val repository = FakeNotificationCenterRepository()
        val useCase = ScheduleReminderUseCase(repository)
        val plan = NotificationReminderPlan(
            notificationId = "n-3",
            title = "내일 체크리스트",
            body = "체크리스트 확인",
            triggerAtEpochMillis = Clock.System.now().toEpochMilliseconds() + 120_000L,
        )

        val result = useCase(plan)

        assertIs<NotificationCenterResult.Success<Unit>>(result)
        assertEquals(1, repository.scheduleCallCount)
        assertEquals(plan, repository.lastPlan)
    }
}

private class FakeNotificationCenterRepository : NotificationCenterRepository {
    var scheduleCallCount: Int = 0
    var lastPlan: NotificationReminderPlan? = null

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> =
        NotificationCenterResult.Success(emptyList())

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        scheduleCallCount += 1
        lastPlan = plan
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun markActivityItemsRead(notificationIds: List<String>): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(emptySet())
    }
}

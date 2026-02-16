package kr.jm.weddingledger.feature.notifications.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class CancelReminderUseCaseTest {

    @Test
    fun `returns error when reminder id is blank`() = runBlocking {
        val repository = FakeCancelRepository()
        val useCase = CancelReminderUseCase(repository)

        val result = useCase("   ")

        val error = assertIs<NotificationCenterResult.Error>(result)
        assertEquals("리마인더 ID가 올바르지 않습니다.", error.message)
        assertEquals(0, repository.cancelCallCount)
    }

    @Test
    fun `delegates to repository when id is valid`() = runBlocking {
        val repository = FakeCancelRepository()
        val useCase = CancelReminderUseCase(repository)

        val result = useCase("event-reminder-1")

        assertIs<NotificationCenterResult.Success<Unit>>(result)
        assertEquals(1, repository.cancelCallCount)
        assertEquals("event-reminder-1", repository.lastCancelId)
    }
}

private class FakeCancelRepository : NotificationCenterRepository {
    var cancelCallCount: Int = 0
    var lastCancelId: String? = null

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        return NotificationCenterResult.Success(emptyList())
    }

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        cancelCallCount += 1
        lastCancelId = notificationId
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(emptySet())
    }
}

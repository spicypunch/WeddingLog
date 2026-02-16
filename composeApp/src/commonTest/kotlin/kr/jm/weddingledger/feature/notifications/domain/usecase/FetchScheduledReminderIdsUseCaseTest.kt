package kr.jm.weddingledger.feature.notifications.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class FetchScheduledReminderIdsUseCaseTest {

    @Test
    fun `returns ids from repository`() = runBlocking {
        val repository = FakeScheduledReminderIdsRepository(setOf("event-reminder-1", "checklist-reminder-7"))
        val useCase = FetchScheduledReminderIdsUseCase(repository)

        val result = useCase()

        val success = assertIs<NotificationCenterResult.Success<Set<String>>>(result)
        assertEquals(setOf("event-reminder-1", "checklist-reminder-7"), success.data)
    }
}

private class FakeScheduledReminderIdsRepository(
    private val ids: Set<String>,
) : NotificationCenterRepository {

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        return NotificationCenterResult.Success(emptyList())
    }

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(ids)
    }
}

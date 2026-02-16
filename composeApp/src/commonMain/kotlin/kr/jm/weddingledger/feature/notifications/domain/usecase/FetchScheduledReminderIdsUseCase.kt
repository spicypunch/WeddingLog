package kr.jm.weddingledger.feature.notifications.domain.usecase

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class FetchScheduledReminderIdsUseCase(
    private val repository: NotificationCenterRepository,
) {
    suspend operator fun invoke(): NotificationCenterResult<Set<String>> =
        repository.fetchScheduledReminderIds()
}

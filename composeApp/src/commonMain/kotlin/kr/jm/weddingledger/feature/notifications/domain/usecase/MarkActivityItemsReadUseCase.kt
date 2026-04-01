package kr.jm.weddingledger.feature.notifications.domain.usecase

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class MarkActivityItemsReadUseCase(
    private val repository: NotificationCenterRepository,
) {
    suspend operator fun invoke(notificationIds: List<String>): NotificationCenterResult<Unit> {
        val sanitizedIds = notificationIds.map(String::trim).filter(String::isNotEmpty).distinct()
        if (sanitizedIds.isEmpty()) {
            return NotificationCenterResult.Success(Unit)
        }
        return repository.markActivityItemsRead(sanitizedIds)
    }
}

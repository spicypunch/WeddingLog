package kr.jm.weddingledger.feature.notifications.domain.usecase

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class FetchNotificationFeedUseCase(
    private val repository: NotificationCenterRepository,
) {
    suspend operator fun invoke(): NotificationCenterResult<List<NotificationFeedItem>> =
        repository.fetchFeed()
}

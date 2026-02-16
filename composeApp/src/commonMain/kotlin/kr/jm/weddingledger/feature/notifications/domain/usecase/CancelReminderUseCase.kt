package kr.jm.weddingledger.feature.notifications.domain.usecase

import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class CancelReminderUseCase(
    private val repository: NotificationCenterRepository,
) {
    suspend operator fun invoke(notificationId: String): NotificationCenterResult<Unit> {
        val normalized = notificationId.trim()
        if (normalized.isBlank()) {
            return NotificationCenterResult.Error("리마인더 ID가 올바르지 않습니다.")
        }
        return repository.cancelReminder(normalized)
    }
}

package kr.jm.weddingledger.feature.notifications.domain.usecase

import kotlin.time.Clock
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository

class ScheduleReminderUseCase(
    private val repository: NotificationCenterRepository,
) {
    suspend operator fun invoke(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        if (plan.notificationId.isBlank()) {
            return NotificationCenterResult.Error("알림 ID가 올바르지 않습니다.")
        }
        if (plan.title.isBlank() || plan.body.isBlank()) {
            return NotificationCenterResult.Error("알림 제목/내용이 비어 있습니다.")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        if (plan.triggerAtEpochMillis <= now) {
            return NotificationCenterResult.Error("예약 시간이 이미 지났습니다.")
        }

        return repository.scheduleReminder(plan)
    }
}

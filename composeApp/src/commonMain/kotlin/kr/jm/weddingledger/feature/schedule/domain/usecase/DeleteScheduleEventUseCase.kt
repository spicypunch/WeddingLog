package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class DeleteScheduleEventUseCase(
    private val repository: ScheduleEventRepository,
) {
    suspend operator fun invoke(id: String): ScheduleResult<Unit> {
        if (id.isBlank()) return ScheduleResult.Error("일정 ID가 유효하지 않습니다.")
        return repository.deleteEvent(id)
    }
}


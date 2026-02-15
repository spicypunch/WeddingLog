package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class DeleteChecklistUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(id: String): ScheduleResult<Unit> {
        if (id.isBlank()) return ScheduleResult.Error("삭제할 항목을 찾지 못했습니다.")
        return repository.deleteChecklist(id)
    }
}


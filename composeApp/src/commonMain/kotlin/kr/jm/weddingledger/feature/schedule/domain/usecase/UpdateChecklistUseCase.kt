package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class UpdateChecklistUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(id: String, task: NewChecklistTask): ScheduleResult<Unit> {
        if (id.isBlank()) return ScheduleResult.Error("수정할 항목을 찾지 못했습니다.")
        validateChecklistTask(task)?.let { return it }
        return repository.updateChecklist(id = id, task = task)
    }
}

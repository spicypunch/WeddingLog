package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class AddChecklistUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(task: NewChecklistTask): ScheduleResult<Unit> {
        validateChecklistTask(task)?.let { return it }
        return repository.createChecklist(task)
    }
}

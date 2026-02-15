package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class AddChecklistUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(task: NewChecklistTask): ScheduleResult<Unit> {
        if (task.title.isBlank()) return ScheduleResult.Error("할 일 제목을 입력해주세요.")
        return repository.createChecklist(task)
    }
}


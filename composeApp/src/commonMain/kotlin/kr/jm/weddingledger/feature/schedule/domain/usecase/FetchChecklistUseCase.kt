package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class FetchChecklistUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(): ScheduleResult<List<ChecklistTask>> = repository.fetchChecklist()
}


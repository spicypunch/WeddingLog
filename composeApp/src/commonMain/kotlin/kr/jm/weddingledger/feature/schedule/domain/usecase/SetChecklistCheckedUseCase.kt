package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class SetChecklistCheckedUseCase(
    private val repository: ChecklistRepository,
) {
    suspend operator fun invoke(id: String, checked: Boolean): ScheduleResult<Unit> {
        if (id.isBlank()) return ScheduleResult.Error("항목을 찾지 못했습니다.")
        return repository.setChecked(id = id, checked = checked)
    }
}


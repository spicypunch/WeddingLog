package kr.jm.weddingledger.feature.schedule.domain.repository

import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult

interface ChecklistRepository {
    suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>>
    suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit>
    suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit>
    suspend fun deleteChecklist(id: String): ScheduleResult<Unit>
}


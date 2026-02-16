package kr.jm.weddingledger.feature.schedule.domain.repository

import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult

interface ScheduleEventRepository {
    suspend fun fetchEvents(): ScheduleResult<List<ScheduleEvent>>
    suspend fun createEvent(event: NewScheduleEvent): ScheduleResult<Unit>
    suspend fun updateEvent(id: String, event: NewScheduleEvent): ScheduleResult<Unit>
    suspend fun deleteEvent(id: String): ScheduleResult<Unit>
}

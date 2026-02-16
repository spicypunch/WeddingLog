package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class FetchScheduleEventsUseCase(
    private val repository: ScheduleEventRepository,
) {
    suspend operator fun invoke(): ScheduleResult<List<ScheduleEvent>> = repository.fetchEvents()
}


package kr.jm.weddingledger.feature.schedule.domain.usecase

import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class UpdateScheduleEventUseCase(
    private val repository: ScheduleEventRepository,
) {
    suspend operator fun invoke(id: String, event: NewScheduleEvent): ScheduleResult<Unit> {
        if (id.isBlank()) return ScheduleResult.Error("일정 ID가 유효하지 않습니다.")
        if (event.title.isBlank()) return ScheduleResult.Error("일정 제목을 입력해주세요.")
        val time = event.startTime?.trim()
        if (!time.isNullOrEmpty() && !isValidTime(time)) {
            return ScheduleResult.Error("시간 형식은 HH:mm 으로 입력해주세요.")
        }
        return repository.updateEvent(id = id, event = event.copy(startTime = time))
    }
}

private fun isValidTime(value: String): Boolean {
    if (value.length != 5 || value[2] != ':') return false
    val hh = value.substring(0, 2).toIntOrNull() ?: return false
    val mm = value.substring(3, 5).toIntOrNull() ?: return false
    return hh in 0..23 && mm in 0..59
}


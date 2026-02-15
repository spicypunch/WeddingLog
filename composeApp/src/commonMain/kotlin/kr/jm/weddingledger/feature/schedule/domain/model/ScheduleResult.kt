package kr.jm.weddingledger.feature.schedule.domain.model

sealed class ScheduleResult<out T> {
    data class Success<T>(val data: T) : ScheduleResult<T>()
    data class Error(val message: String) : ScheduleResult<Nothing>()
}


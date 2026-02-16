package kr.jm.weddingledger.feature.schedule.domain.model

import kotlinx.datetime.LocalDate

data class NewScheduleEvent(
    val title: String,
    val eventDate: LocalDate,
    val startTime: String?,
    val location: String?,
    val memo: String?,
    val repeatRule: EventRepeatRule = EventRepeatRule.None,
)

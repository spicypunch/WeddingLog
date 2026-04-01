package kr.jm.weddingledger.feature.schedule.presentation

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule

data class AddScheduleEventUiState(
    val title: String,
    val eventDate: LocalDate?,
    val startTime: String,
    val location: String,
    val memo: String,
    val repeatRule: EventRepeatRule,
) {
    val eventDateText: String
        get() = eventDate?.let(::toTargetDateText) ?: "미정"

    val startTimeErrorMessage: String?
        get() = validateStartTime24h(startTime)

    val isSaveEnabled: Boolean
        get() = title.trim().isNotEmpty() && eventDate != null && startTimeErrorMessage == null

    companion object {
        fun empty(): AddScheduleEventUiState = AddScheduleEventUiState(
            title = "",
            eventDate = null,
            startTime = "",
            location = "",
            memo = "",
            repeatRule = EventRepeatRule.None,
        )
    }
}

sealed interface AddScheduleEventAction {
    data object OnClickClose : AddScheduleEventAction
    data object OnClickSave : AddScheduleEventAction
    data object OnClickEventDate : AddScheduleEventAction
    data class OnTitleChange(val value: String) : AddScheduleEventAction
    data class OnStartTimeChange(val value: String) : AddScheduleEventAction
    data class OnLocationChange(val value: String) : AddScheduleEventAction
    data class OnMemoChange(val value: String) : AddScheduleEventAction
    data class OnSelectRepeatRule(val value: EventRepeatRule) : AddScheduleEventAction
}

internal fun maskStartTime24hInput(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    return when {
        digits.length <= 2 -> digits
        else -> "${digits.substring(0, 2)}:${digits.substring(2)}"
    }
}

internal fun validateStartTime24h(value: String): String? {
    if (value.isBlank()) return null
    if (value.length != 5 || value[2] != ':') {
        return "시간 형식은 HH:mm 으로 입력해주세요."
    }

    val hour = value.substring(0, 2).toIntOrNull()
    val minute = value.substring(3, 5).toIntOrNull()

    if (hour == null || minute == null) {
        return "시간 형식은 HH:mm 으로 입력해주세요."
    }
    if (hour !in 0..23 || minute !in 0..59) {
        return "시간은 00:00~23:59 범위로 입력해주세요."
    }
    return null
}

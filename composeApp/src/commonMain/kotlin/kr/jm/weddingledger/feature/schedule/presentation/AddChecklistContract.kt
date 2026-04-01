package kr.jm.weddingledger.feature.schedule.presentation

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

data class AddChecklistUiState(
    val title: String,
    val targetDate: LocalDate?,
    val dTagOptions: List<String>,
    val selectedDTag: String,
    val isCompleted: Boolean,
    val memo: String,
) {
    val targetDateText: String
        get() = targetDate?.let(::toTargetDateText) ?: "미정"

    val isSaveEnabled: Boolean
        get() {
            val titleLength = title.trim().length
            val memoLength = memo.trim().length
            return titleLength in 2..TITLE_MAX_LENGTH &&
                memoLength <= MEMO_MAX_LENGTH &&
                selectedDTag.isValidDTagValue()
        }

    val titleLength: Int
        get() = title.trim().length

    val memoLength: Int
        get() = memo.trim().length

    companion object {
        const val TITLE_MAX_LENGTH: Int = 40
        const val MEMO_MAX_LENGTH: Int = 120

        fun empty(): AddChecklistUiState = AddChecklistUiState(
            title = "",
            targetDate = null,
            dTagOptions = listOf("D-180", "D-100", "D-60", "D-30", "D-7"),
            selectedDTag = "D-180",
            isCompleted = false,
            memo = "",
        )
    }
}

sealed interface AddChecklistAction {
    data object OnClickClose : AddChecklistAction
    data object OnClickSave : AddChecklistAction
    data object OnClickTargetDate : AddChecklistAction
    data object OnClickClearTargetDate : AddChecklistAction
    data class OnSelectTargetDate(val value: LocalDate) : AddChecklistAction
    data class OnTitleChange(val value: String) : AddChecklistAction
    data class OnSelectDTag(val value: String) : AddChecklistAction
    data class OnToggleCompleted(val value: Boolean) : AddChecklistAction
    data class OnMemoChange(val value: String) : AddChecklistAction
}

internal fun toTargetDateText(date: LocalDate): String {
    val month = date.month.number.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
    return "${date.year}.$month.$day ($weekday)"
}

internal fun String.isValidDTagValue(): Boolean {
    val normalized = trim().uppercase()
    if (!normalized.startsWith("D-")) return false
    val number = normalized.removePrefix("D-").toIntOrNull() ?: return false
    return number in 0..365
}

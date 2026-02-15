package kr.jm.weddingledger.feature.schedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.SetChecklistCheckedUseCase

class ScheduleViewModel(
    private val fetchChecklistUseCase: FetchChecklistUseCase,
    private val addChecklistUseCase: AddChecklistUseCase,
    private val setChecklistCheckedUseCase: SetChecklistCheckedUseCase,
    private val deleteChecklistUseCase: DeleteChecklistUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScheduleUiState(
            selectedTab = ScheduleTab.Schedule,
            calendar = CalendarUiState.sample(),
            checklist = ChecklistUiState.empty(),
        )
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        refreshChecklist(showLoading = true)
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.OnSelectTab -> _state.update { it.copy(selectedTab = action.tab) }
            ScheduleAction.OnClickPrevMonth -> Unit // TODO
            ScheduleAction.OnClickNextMonth -> Unit // TODO
            is ScheduleAction.OnSelectCalendarDay -> selectCalendarDay(action.dayId)
            ScheduleAction.OnClickViewAllUpcoming -> Unit // TODO
            is ScheduleAction.OnClickUpcomingSchedule -> Unit // TODO
            is ScheduleAction.OnToggleChecklistItem -> toggleChecklistItem(action.itemId)
            is ScheduleAction.OnClickChecklistItemMore -> Unit // handled in Route (dialog)
            ScheduleAction.OnClickAdd -> Unit // handled in Route (bottom sheet)

            ScheduleAction.OnRefreshChecklist -> refreshChecklist(showLoading = true)
            is ScheduleAction.OnCreateChecklist -> createChecklist(action)
            is ScheduleAction.OnDeleteChecklist -> deleteChecklist(action.id)
        }
    }

    private fun selectCalendarDay(dayId: String) {
        _state.update { current ->
            current.copy(
                calendar = current.calendar.copy(
                    cells = current.calendar.cells.map { cell ->
                        if (cell.id == dayId && cell.inMonth && !cell.isDDay) {
                            cell.copy(isSelected = true)
                        } else {
                            cell.copy(isSelected = false)
                        }
                    }
                )
            )
        }
    }

    private fun refreshChecklist(showLoading: Boolean) {
        _state.update { current ->
            current.copy(
                checklist = current.checklist.copy(
                    isLoading = showLoading,
                    message = null,
                )
            )
        }

        viewModelScope.launch {
            when (val result = fetchChecklistUseCase()) {
                is ScheduleResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            checklist = result.data.toChecklistUiState().copy(isLoading = false)
                        )
                    }
                }
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            checklist = current.checklist.copy(
                                isLoading = false,
                                message = result.message,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createChecklist(action: ScheduleAction.OnCreateChecklist) {
        _state.update { current ->
            current.copy(
                checklist = current.checklist.copy(isLoading = true, message = null)
            )
        }

        viewModelScope.launch {
            val inferredGroupTitle = defaultGroupTitleFor(action.dTag)
            val task = NewChecklistTask(
                title = action.title,
                dTag = action.dTag,
                groupTitle = inferredGroupTitle,
                isChecked = action.isCompleted,
                targetDate = null, // TODO: date picker 연결 시 저장
                memo = action.memo,
            )

            when (val result = addChecklistUseCase(task)) {
                is ScheduleResult.Success -> refreshChecklist(showLoading = false)
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            checklist = current.checklist.copy(
                                isLoading = false,
                                message = result.message,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun toggleChecklistItem(itemId: String) {
        val snapshot = _state.value
        val currentItem = snapshot.checklist
            .groups
            .asSequence()
            .flatMap { it.items.asSequence() }
            .firstOrNull { it.id == itemId }
            ?: return

        val prevChecked = currentItem.isDone
        val newChecked = !prevChecked

        // Optimistic update for immediate UI feedback.
        _state.update { current ->
            current.copy(
                checklist = current.checklist.copy(
                    message = null,
                    groups = current.checklist.groups.map { group ->
                        group.copy(
                            items = group.items.map { item ->
                                if (item.id == itemId) {
                                    item.copy(
                                        isDone = newChecked,
                                        subtitle = if (newChecked) "완료" else null,
                                        subtitleTone = ChecklistItemSubtitleTone.Muted,
                                    )
                                } else {
                                    item
                                }
                            }
                        )
                    }
                )
            )
        }

        viewModelScope.launch {
            when (val result = setChecklistCheckedUseCase(itemId, newChecked)) {
                is ScheduleResult.Success -> refreshChecklist(showLoading = false)
                is ScheduleResult.Error -> {
                    // 서버 반영 실패 시 optimistic update를 원복합니다.
                    _state.update { current ->
                        current.copy(
                            checklist = current.checklist.copy(
                                message = result.message,
                                groups = current.checklist.groups.map { group ->
                                    group.copy(
                                        items = group.items.map { item ->
                                            if (item.id == itemId) {
                                                item.copy(
                                                    isDone = prevChecked,
                                                    subtitle = if (prevChecked) "완료" else null,
                                                    subtitleTone = ChecklistItemSubtitleTone.Muted,
                                                )
                                            } else {
                                                item
                                            }
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun deleteChecklist(id: String) {
        _state.update { current ->
            current.copy(
                checklist = current.checklist.copy(isLoading = true, message = null)
            )
        }

        viewModelScope.launch {
            when (val result = deleteChecklistUseCase(id)) {
                is ScheduleResult.Success -> refreshChecklist(showLoading = false)
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            checklist = current.checklist.copy(
                                isLoading = false,
                                message = result.message,
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun List<ChecklistTask>.toChecklistUiState(): ChecklistUiState {
    if (isEmpty()) return ChecklistUiState.empty()

    val grouped = groupBy { it.dTag?.trim().orEmpty().ifEmpty { "기타" } }
    val maxDTagNumber = grouped.keys.mapNotNull { parseDTagNumberOrNull(it) }.maxOrNull()

    val groups = grouped
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, List<ChecklistTask>>> { parseDTagNumberOrNull(it.key) ?: Int.MIN_VALUE }
            .thenBy { it.key })
        .map { (dTag, tasks) ->
            val inferredTitle = tasks.firstNotNullOfOrNull { it.groupTitle?.trim()?.takeIf { t -> t.isNotEmpty() } }
                ?: defaultGroupTitleFor(dTag)

            val tagStyle = if (maxDTagNumber != null && parseDTagNumberOrNull(dTag) == maxDTagNumber) {
                DTagStyle.Primary
            } else {
                DTagStyle.Neutral
            }

            ChecklistGroup(
                id = dTag,
                dTag = dTag,
                title = inferredTitle,
                tagStyle = tagStyle,
                items = tasks.map { it.toChecklistItem() }
            )
        }

    return ChecklistUiState(groups = groups)
}

private fun ChecklistTask.toChecklistItem(): ChecklistItem {
    val isDone = isChecked
    val subtitle = when {
        isDone -> "완료"
        targetDate != null -> formatLocalDate(targetDate)
        !memo.isNullOrBlank() -> memo.trim()
        else -> null
    }

    val subtitleTone = when {
        isDone -> ChecklistItemSubtitleTone.Muted
        subtitle != null && subtitle.contains("예정") -> ChecklistItemSubtitleTone.Primary
        else -> ChecklistItemSubtitleTone.Muted
    }

    return ChecklistItem(
        id = id,
        title = title,
        subtitle = subtitle,
        subtitleTone = subtitleTone,
        isDone = isDone,
    )
}

private fun parseDTagNumberOrNull(value: String): Int? {
    // Examples: D-180, D-30, D+6
    val trimmed = value.trim()
    if (!trimmed.startsWith("D")) return null
    val signIndex = trimmed.indexOfFirst { it == '-' || it == '+' }
    if (signIndex == -1) return null
    val sign = trimmed[signIndex]
    val num = trimmed.substring(signIndex + 1).toIntOrNull() ?: return null
    return if (sign == '-') num else -num.absoluteValue
}

private fun defaultGroupTitleFor(dTag: String): String {
    return when (dTag.trim()) {
        "D-365" -> "결혼 준비 시작"
        "D-180" -> "상견례 및 웨딩홀"
        "D-100" -> "스튜디오 촬영"
        "D-60" -> "혼수/신혼집"
        "D-30" -> "본식 준비"
        "D-7" -> "마지막 점검"
        else -> "할 일"
    }
}

private fun formatLocalDate(date: LocalDate): String {
    // Matches the style used in the HTML mocks: 2023.10.15
    val mm = date.month.number.toString().padStart(2, '0')
    val dd = date.day.toString().padStart(2, '0')
    return "${date.year}.$mm.$dd"
}

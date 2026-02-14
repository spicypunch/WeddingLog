package kr.jm.weddingledger.feature.schedule.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ScheduleRoute() {
    var uiState by remember { mutableStateOf(ScheduleUiState.sample()) }

    var activeSheet by remember { mutableStateOf<ScheduleSheet?>(null) }
    var addChecklistState by remember { mutableStateOf(AddChecklistUiState.sample()) }

    ScheduleScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is ScheduleAction.OnSelectTab -> uiState = uiState.copy(selectedTab = action.tab)
                ScheduleAction.OnClickAdd -> {
                    activeSheet = when (uiState.selectedTab) {
                        ScheduleTab.Schedule -> ScheduleSheet.AddChecklist // TODO: add schedule item later
                        ScheduleTab.Checklist -> ScheduleSheet.AddChecklist
                    }
                }
                ScheduleAction.OnClickPrevMonth -> Unit // TODO
                ScheduleAction.OnClickNextMonth -> Unit // TODO
                is ScheduleAction.OnSelectCalendarDay -> {
                    uiState = uiState.copy(
                        calendar = uiState.calendar.copy(
                            cells = uiState.calendar.cells.map { cell ->
                                if (cell.id == action.dayId && cell.inMonth && !cell.isDDay) {
                                    cell.copy(isSelected = true)
                                } else {
                                    cell.copy(isSelected = false)
                                }
                            }
                        )
                    )
                }
                ScheduleAction.OnClickViewAllUpcoming -> Unit // TODO
                is ScheduleAction.OnClickUpcomingSchedule -> Unit // TODO
                is ScheduleAction.OnToggleChecklistItem -> {
                    uiState = uiState.copy(
                        checklist = uiState.checklist.copy(
                            groups = uiState.checklist.groups.map { group ->
                                group.copy(
                                    items = group.items.map { item ->
                                        if (item.id == action.itemId) item.copy(isDone = !item.isDone) else item
                                    }
                                )
                            }
                        )
                    )
                }
                is ScheduleAction.OnClickChecklistItemMore -> Unit // TODO: context menu
            }
        }
    )

    when (activeSheet) {
        null -> Unit
        ScheduleSheet.AddChecklist -> {
            AddChecklistBottomSheet(
                state = addChecklistState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddChecklistAction.OnClickClose -> activeSheet = null
                        AddChecklistAction.OnClickSave -> activeSheet = null // TODO: persist + sync
                        AddChecklistAction.OnClickTargetDate -> Unit // TODO: date picker
                        is AddChecklistAction.OnTitleChange -> addChecklistState = addChecklistState.copy(title = act.value)
                        is AddChecklistAction.OnSelectDTag -> addChecklistState = addChecklistState.copy(selectedDTag = act.value)
                        is AddChecklistAction.OnToggleCompleted -> addChecklistState = addChecklistState.copy(isCompleted = act.value)
                        is AddChecklistAction.OnMemoChange -> addChecklistState = addChecklistState.copy(memo = act.value)
                    }
                }
            )
        }
    }
}

private sealed interface ScheduleSheet {
    data object AddChecklist : ScheduleSheet
}

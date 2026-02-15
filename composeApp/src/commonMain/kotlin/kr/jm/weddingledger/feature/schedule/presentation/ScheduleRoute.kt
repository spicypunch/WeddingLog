package kr.jm.weddingledger.feature.schedule.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleRoute() {
    val viewModel: ScheduleViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<ScheduleSheet?>(null) }
    var addChecklistState by remember { mutableStateOf(AddChecklistUiState.sample()) }
    var pendingDeleteChecklistId by remember { mutableStateOf<String?>(null) }

    ScheduleScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                ScheduleAction.OnClickAdd -> {
                    activeSheet = when (uiState.selectedTab) {
                        ScheduleTab.Schedule -> ScheduleSheet.AddChecklist // TODO: add schedule item later
                        ScheduleTab.Checklist -> ScheduleSheet.AddChecklist
                    }
                    addChecklistState = AddChecklistUiState.sample()
                }
                is ScheduleAction.OnClickChecklistItemMore -> pendingDeleteChecklistId = action.itemId
                else -> viewModel.onAction(action)
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
                        AddChecklistAction.OnClickSave -> {
                            viewModel.onAction(
                                ScheduleAction.OnCreateChecklist(
                                    title = addChecklistState.title,
                                    dTag = addChecklistState.selectedDTag,
                                    isCompleted = addChecklistState.isCompleted,
                                    memo = addChecklistState.memo,
                                )
                            )
                            activeSheet = null
                            addChecklistState = AddChecklistUiState.sample()
                        }
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

    pendingDeleteChecklistId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteChecklistId = null },
            title = { Text(text = "삭제할까요?") },
            text = { Text(text = "이 할 일을 체크리스트에서 삭제합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(ScheduleAction.OnDeleteChecklist(id))
                        pendingDeleteChecklistId = null
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteChecklistId = null }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

private sealed interface ScheduleSheet {
    data object AddChecklist : ScheduleSheet
}

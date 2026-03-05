package kr.jm.weddingledger.feature.guests.presentation

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
fun GuestsRoute() {
    GuestsRoute(onOpenNotifications = {})
}

@Composable
fun GuestsRoute(
    onOpenNotifications: () -> Unit,
) {
    val viewModel: GuestsViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<GuestsSheet?>(null) }
    var addGuestState by remember { mutableStateOf(AddGuestUiState.sample()) }
    var selectedGuestDetail by remember { mutableStateOf<GuestListItem?>(null) }
    var pendingDeleteGuestId by remember { mutableStateOf<String?>(null) }

    GuestsScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                GuestsAction.OnClickAdd -> {
                    activeSheet = GuestsSheet.AddGuestCreate
                    addGuestState = AddGuestUiState.sample()
                    selectedGuestDetail = null
                }

                is GuestsAction.OnClickGuest -> {
                    selectedGuestDetail = uiState.sections
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .firstOrNull { it.id == action.id }
                }

                GuestsAction.OnClickNotifications -> onOpenNotifications()
                else -> viewModel.onAction(action)
            }
        }
    )

    when (val sheet = activeSheet) {
        null -> Unit
        GuestsSheet.AddGuestCreate -> {
            AddGuestBottomSheet(
                state = addGuestState,
                onDismiss = { activeSheet = null },
                sheetTitle = "하객 추가",
                saveButtonText = "저장하기",
                onAction = { act ->
                    when (act) {
                        AddGuestAction.OnClickClose -> activeSheet = null
                        AddGuestAction.OnClickSave -> {
                            viewModel.onAction(
                                GuestsAction.OnCreateGuest(
                                    name = addGuestState.name,
                                    side = addGuestState.side,
                                    relationship = addGuestState.relationship,
                                    expectedGiftAmount = parseDigitsToLongOrNull(addGuestState.expectedGiftAmount),
                                    attendance = addGuestState.attendance,
                                    note = addGuestState.note,
                                )
                            )
                            activeSheet = null
                            addGuestState = AddGuestUiState.sample()
                        }

                        is AddGuestAction.OnNameChange -> addGuestState = addGuestState.copy(name = act.value)
                        is AddGuestAction.OnSelectSide -> addGuestState = addGuestState.copy(side = act.side)
                        is AddGuestAction.OnSelectRelationship -> addGuestState = addGuestState.copy(relationship = act.level)
                        is AddGuestAction.OnExpectedGiftAmountChange -> addGuestState = addGuestState.copy(expectedGiftAmount = act.value)
                        is AddGuestAction.OnQuickAddExpectedGift -> {
                            val current = parseDigitsToLong(addGuestState.expectedGiftAmount)
                            val add = parseDigitsToLong(act.value)
                            addGuestState = addGuestState.copy(expectedGiftAmount = formatWithComma(current + add))
                        }

                        is AddGuestAction.OnSelectAttendance -> addGuestState = addGuestState.copy(attendance = act.value)
                        is AddGuestAction.OnNoteChange -> addGuestState = addGuestState.copy(note = act.value)
                    }
                }
            )
        }

        is GuestsSheet.EditGuest -> {
            val editingId = sheet.guestId
            AddGuestBottomSheet(
                state = addGuestState,
                onDismiss = { activeSheet = null },
                sheetTitle = "하객 수정",
                saveButtonText = "수정 저장",
                onClickDelete = { pendingDeleteGuestId = editingId },
                onAction = { act ->
                    when (act) {
                        AddGuestAction.OnClickClose -> activeSheet = null
                        AddGuestAction.OnClickSave -> {
                            viewModel.onAction(
                                GuestsAction.OnUpdateGuest(
                                    id = editingId,
                                    name = addGuestState.name,
                                    side = addGuestState.side,
                                    relationship = addGuestState.relationship,
                                    expectedGiftAmount = parseDigitsToLongOrNull(addGuestState.expectedGiftAmount),
                                    attendance = addGuestState.attendance,
                                    note = addGuestState.note,
                                )
                            )
                            activeSheet = null
                            addGuestState = AddGuestUiState.sample()
                        }

                        is AddGuestAction.OnNameChange -> addGuestState = addGuestState.copy(name = act.value)
                        is AddGuestAction.OnSelectSide -> addGuestState = addGuestState.copy(side = act.side)
                        is AddGuestAction.OnSelectRelationship -> addGuestState = addGuestState.copy(relationship = act.level)
                        is AddGuestAction.OnExpectedGiftAmountChange -> addGuestState = addGuestState.copy(expectedGiftAmount = act.value)
                        is AddGuestAction.OnQuickAddExpectedGift -> {
                            val current = parseDigitsToLong(addGuestState.expectedGiftAmount)
                            val add = parseDigitsToLong(act.value)
                            addGuestState = addGuestState.copy(expectedGiftAmount = formatWithComma(current + add))
                        }

                        is AddGuestAction.OnSelectAttendance -> addGuestState = addGuestState.copy(attendance = act.value)
                        is AddGuestAction.OnNoteChange -> addGuestState = addGuestState.copy(note = act.value)
                    }
                }
            )
        }
    }

    selectedGuestDetail?.let { item ->
        GuestDetailBottomSheet(
            item = item,
            onDismiss = { selectedGuestDetail = null },
            onEdit = {
                addGuestState = item.toEditState()
                selectedGuestDetail = null
                activeSheet = GuestsSheet.EditGuest(item.id)
            },
            onDelete = {
                pendingDeleteGuestId = item.id
                selectedGuestDetail = null
            },
            onQuickSetAttendance = { status ->
                viewModel.onAction(GuestsAction.OnQuickSetAttendance(item.id, status))
                selectedGuestDetail = null
            }
        )
    }

    pendingDeleteGuestId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGuestId = null },
            title = { Text(text = "하객을 삭제할까요?") },
            text = { Text(text = "선택한 하객 정보는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(GuestsAction.OnDeleteGuest(id))
                        pendingDeleteGuestId = null
                        activeSheet = null
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGuestId = null }) {
                    Text(text = "취소")
                }
            },
        )
    }
}

private sealed interface GuestsSheet {
    data object AddGuestCreate : GuestsSheet
    data class EditGuest(val guestId: String) : GuestsSheet
}

private fun parseDigitsToLong(text: String): Long {
    val digits = text.filter { it.isDigit() }
    return digits.toLongOrNull() ?: 0L
}

private fun parseDigitsToLongOrNull(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    return digits.toLongOrNull()
}

private fun formatWithComma(value: Long): String {
    val s = value.toString()
    val sb = StringBuilder(s.length + (s.length / 3))
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append(',')
    }
    return sb.reverse().toString()
}

private fun GuestListItem.toEditState(): AddGuestUiState {
    return AddGuestUiState.sample().copy(
        name = name,
        side = side,
        relationship = relationship,
        expectedGiftAmount = expectedGiftAmount?.let(::formatWithComma).orEmpty(),
        note = note.orEmpty(),
        attendance = status,
    )
}

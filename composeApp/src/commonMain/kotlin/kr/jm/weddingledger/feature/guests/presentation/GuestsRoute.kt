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
    var guestEditorState by remember { mutableStateOf(AddGuestUiState.expectedSample()) }
    var guaranteedGuestCountInput by remember { mutableStateOf("") }
    var pendingDeleteGuestId by remember { mutableStateOf<String?>(null) }

    GuestsScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                GuestsAction.OnClickAdd -> {
                    val defaultSide = uiState.currentUserRole.toDefaultGuestSide()
                    when (uiState.selectedTab) {
                        GuestsTab.Expected -> {
                            guestEditorState = AddGuestUiState.expectedSample(defaultSide)
                            activeSheet = GuestsSheet.CreateExpected
                        }
                        GuestsTab.GiftLedger -> {
                            guestEditorState = AddGuestUiState.giftSample(defaultSide)
                            activeSheet = GuestsSheet.CreateGiftLedger
                        }
                    }
                }

                GuestsAction.OnClickNotifications -> onOpenNotifications()
                GuestsAction.OnClickEditGuaranteedCount -> {
                    guaranteedGuestCountInput = uiState.expected.guaranteedGuestCountText.filter(Char::isDigit)
                    activeSheet = GuestsSheet.EditGuaranteedCount
                }

                is GuestsAction.OnClickExpectedGuest -> {
                    uiState.expected.sections
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .firstOrNull { it.id == action.id }
                        ?.let { item ->
                            guestEditorState = item.editorSnapshot.toEditorState(GuestEditorMode.Expected)
                            activeSheet = GuestsSheet.EditExpected(action.id)
                        }
                }

                is GuestsAction.OnClickGiftGuest -> {
                    uiState.giftLedger.sections
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .firstOrNull { it.id == action.id }
                        ?.let { item ->
                            guestEditorState = item.editorSnapshot.toEditorState(GuestEditorMode.GiftLedger)
                            activeSheet = GuestsSheet.EditGiftLedger(action.id)
                        }
                }

                else -> viewModel.onAction(action)
            }
        }
    )

    when (val sheet = activeSheet) {
        null -> Unit
        GuestsSheet.CreateExpected,
        GuestsSheet.CreateGiftLedger,
        is GuestsSheet.EditExpected,
        is GuestsSheet.EditGiftLedger,
        -> {
            AddGuestBottomSheet(
                state = guestEditorState,
                onDismiss = { activeSheet = null },
                onClickDelete = when (sheet) {
                    is GuestsSheet.EditExpected -> { { pendingDeleteGuestId = sheet.guestId } }
                    is GuestsSheet.EditGiftLedger -> { { pendingDeleteGuestId = sheet.guestId } }
                    else -> null
                },
                onAction = { act ->
                    when (act) {
                        AddGuestAction.OnClickClose -> activeSheet = null
                        AddGuestAction.OnClickSave -> {
                            when (sheet) {
                                GuestsSheet.CreateExpected,
                                GuestsSheet.CreateGiftLedger,
                                -> {
                                    viewModel.onAction(
                                        GuestsAction.OnCreateGuest(
                                            name = guestEditorState.name,
                                            side = guestEditorState.side,
                                            relationship = guestEditorState.relationship,
                                            expectedStatus = guestEditorState.expectedStatus,
                                            companionCount = guestEditorState.companionCount.toIntOrNull() ?: 0,
                                            actualAttended = guestEditorState.actualAttendance,
                                            actualGiftAmount = parseAmountOrNull(guestEditorState.actualGiftAmount),
                                            note = guestEditorState.note.ifBlank { null },
                                        )
                                    )
                                }
                                is GuestsSheet.EditExpected -> {
                                    viewModel.onAction(
                                        GuestsAction.OnUpdateGuest(
                                            id = sheet.guestId,
                                            name = guestEditorState.name,
                                            side = guestEditorState.side,
                                            relationship = guestEditorState.relationship,
                                            expectedStatus = guestEditorState.expectedStatus,
                                            companionCount = guestEditorState.companionCount.toIntOrNull() ?: 0,
                                            actualAttended = guestEditorState.actualAttendance,
                                            actualGiftAmount = parseAmountOrNull(guestEditorState.actualGiftAmount),
                                            note = guestEditorState.note.ifBlank { null },
                                        )
                                    )
                                }
                                is GuestsSheet.EditGiftLedger -> {
                                    viewModel.onAction(
                                        GuestsAction.OnUpdateGuest(
                                            id = sheet.guestId,
                                            name = guestEditorState.name,
                                            side = guestEditorState.side,
                                            relationship = guestEditorState.relationship,
                                            expectedStatus = guestEditorState.expectedStatus,
                                            companionCount = guestEditorState.companionCount.toIntOrNull() ?: 0,
                                            actualAttended = guestEditorState.actualAttendance,
                                            actualGiftAmount = parseAmountOrNull(guestEditorState.actualGiftAmount),
                                            note = guestEditorState.note.ifBlank { null },
                                        )
                                    )
                                }
                            }
                            activeSheet = null
                        }

                        is AddGuestAction.OnNameChange -> guestEditorState = guestEditorState.copy(name = act.value)
                        is AddGuestAction.OnSelectSide -> guestEditorState = guestEditorState.copy(side = act.side)
                        is AddGuestAction.OnSelectRelationship -> guestEditorState = guestEditorState.copy(relationship = act.level)
                        is AddGuestAction.OnSelectExpectedStatus -> guestEditorState = guestEditorState.copy(expectedStatus = act.value)
                        is AddGuestAction.OnCompanionCountChange -> guestEditorState = guestEditorState.copy(companionCount = act.value)
                        is AddGuestAction.OnSelectActualAttendance -> guestEditorState = guestEditorState.copy(actualAttendance = act.value)
                        is AddGuestAction.OnActualGiftAmountChange -> guestEditorState = guestEditorState.copy(actualGiftAmount = act.value)
                        is AddGuestAction.OnNoteChange -> guestEditorState = guestEditorState.copy(note = act.value)
                    }
                }
            )
        }

        GuestsSheet.EditGuaranteedCount -> {
            GuestSettingsBottomSheet(
                guaranteedGuestCount = guaranteedGuestCountInput,
                onDismiss = { activeSheet = null },
                onGuaranteedGuestCountChange = { guaranteedGuestCountInput = it },
                onSave = {
                    viewModel.onAction(
                        GuestsAction.OnUpdateGuaranteedCount(
                            guaranteedGuestCountInput.toIntOrNull() ?: 0,
                        )
                    )
                    activeSheet = null
                },
            )
        }
    }

    pendingDeleteGuestId?.let { guestId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGuestId = null },
            title = { Text(text = "하객을 삭제할까요?") },
            text = { Text(text = "선택한 하객 정보는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(GuestsAction.OnDeleteGuest(guestId))
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
    data object CreateExpected : GuestsSheet
    data object CreateGiftLedger : GuestsSheet
    data class EditExpected(val guestId: String) : GuestsSheet
    data class EditGiftLedger(val guestId: String) : GuestsSheet
    data object EditGuaranteedCount : GuestsSheet
}

private fun GuestEditorSnapshot.toEditorState(mode: GuestEditorMode): AddGuestUiState {
    return AddGuestUiState(
        mode = mode,
        name = name,
        side = side,
        relationship = relationship,
        expectedStatus = expectedStatus,
        companionCount = companionCount.toString(),
        actualAttendance = actualAttendance,
        actualGiftAmount = actualGiftAmount?.let(::formatAmount) ?: "",
        note = note.orEmpty(),
    )
}

private fun parseAmountOrNull(text: String): Long? {
    val digits = text.filter(Char::isDigit)
    return digits.toLongOrNull()?.takeIf { it > 0L }
}

private fun formatAmount(value: Long): String {
    val s = value.toString()
    val sb = StringBuilder(s.length + (s.length / 3))
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count += 1
        if (count % 3 == 0 && i != 0) sb.append(',')
    }
    return sb.reverse().toString()
}

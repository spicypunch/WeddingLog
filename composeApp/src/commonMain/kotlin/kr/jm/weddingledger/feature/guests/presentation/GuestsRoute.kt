package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun GuestsRoute() {
    var uiState by remember { mutableStateOf(GuestsUiState.sample()) }

    var activeSheet by remember { mutableStateOf<GuestsSheet?>(null) }
    var addGuestState by remember { mutableStateOf(AddGuestUiState.sample()) }

    GuestsScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                is GuestsAction.OnSearchQueryChange -> uiState = uiState.copy(searchQuery = action.value)
                is GuestsAction.OnSelectFilter -> {
                    uiState = uiState.copy(
                        filters = uiState.filters.map { it.copy(selected = it.id == action.id) }
                    )
                }
                GuestsAction.OnClickAdd -> activeSheet = GuestsSheet.AddGuest
                is GuestsAction.OnClickGuest -> Unit // TODO: guest detail
            }
        }
    )

    when (activeSheet) {
        null -> Unit
        GuestsSheet.AddGuest -> {
            AddGuestBottomSheet(
                state = addGuestState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddGuestAction.OnClickClose -> activeSheet = null
                        AddGuestAction.OnClickSave -> activeSheet = null // TODO: persist + sync
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
                    }
                }
            )
        }
    }
}

private sealed interface GuestsSheet {
    data object AddGuest : GuestsSheet
}

private fun parseDigitsToLong(text: String): Long {
    val digits = text.filter { it.isDigit() }
    return digits.toLongOrNull() ?: 0L
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


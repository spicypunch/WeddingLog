package kr.jm.weddingledger.feature.settings.presentation

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditRoute(
    onBack: () -> Unit,
) {
    val viewModel: ProfileEditViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEditEffect.Saved -> onBack()
            }
        }
    }

    ProfileEditScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                ProfileEditAction.OnClickWeddingDate -> {
                    if (!state.isDateUndecided && !state.isLoading) {
                        showDatePicker = true
                    }
                }

                else -> viewModel.onAction(action)
            }
        },
    )

    if (showDatePicker) {
        val zone = TimeZone.UTC
        val initialDate = state.weddingDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        val initialMillis = initialDate.atStartOfDayIn(zone).toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis
                        if (selected != null) {
                            val date = Instant.fromEpochMilliseconds(selected)
                                .toLocalDateTime(zone)
                                .date
                            viewModel.onAction(ProfileEditAction.OnSelectWeddingDate(date))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = "확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "취소")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

package kr.jm.weddingledger.feature.onboarding.presentation

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.step) {
        if (state.step != OnboardingStep.ProfileSetup) showDatePicker = false
    }

    when (state.step) {
        OnboardingStep.Loading,
        OnboardingStep.Done,
        -> Unit // handled in Gate

        OnboardingStep.ProfileSetup -> {
            ProfileSetupScreen(
                state = state.profile,
                isLoading = state.isLoading,
                message = state.message,
                onAction = { action ->
                    when (action) {
                        OnboardingAction.OnClickWeddingDate -> {
                            if (!state.profile.isDateUndecided) showDatePicker = true
                        }
                        else -> viewModel.onAction(action)
                    }
                },
            )

            if (showDatePicker) {
                val timeZone = TimeZone.UTC
                val initialMillis = (state.profile.weddingDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()))
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val date = Instant
                                        .fromEpochMilliseconds(millis)
                                        .toLocalDateTime(timeZone)
                                        .date
                                    viewModel.onAction(OnboardingAction.OnWeddingDateSelected(date))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text(text = "확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = "취소")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        OnboardingStep.PartnerConnect -> {
            PartnerConnectScreen(
                state = state.partner,
                isLoading = state.isLoading,
                message = state.message,
                onAction = { action ->
                    when (action) {
                        OnboardingAction.OnClickInviteShare -> {
                            if (state.partner.inviteCode.isNotBlank()) {
                                clipboard.setText(AnnotatedString(state.partner.inviteCode))
                            }
                            viewModel.onAction(action)
                        }
                        else -> viewModel.onAction(action)
                    }
                },
            )
        }
    }
}

package kr.jm.weddingledger.feature.schedule.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoute() {
    ScheduleRoute(onOpenNotifications = {})
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoute(
    onOpenNotifications: () -> Unit,
) {
    val viewModel: ScheduleViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<ScheduleSheet?>(null) }
    var addChecklistState by remember { mutableStateOf(AddChecklistUiState.sample()) }
    var addChecklistValidationMessage by remember { mutableStateOf<String?>(null) }
    var addScheduleEventState by remember { mutableStateOf(AddScheduleEventUiState.sample()) }
    var selectedEventDetail by remember { mutableStateOf<UpcomingScheduleItem?>(null) }
    var pendingDeleteChecklistId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteScheduleEventId by remember { mutableStateOf<String?>(null) }
    var activeDatePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }

    LaunchedEffect(activeSheet) {
        if (activeSheet == null) {
            activeDatePickerTarget = null
            addChecklistValidationMessage = null
        }
    }

    ScheduleScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                ScheduleAction.OnClickAdd -> {
                    selectedEventDetail = null
                    val selectedDate = parseDateOrNull(uiState.calendar.selectedDayId.orEmpty())
                    when (uiState.selectedTab) {
                        ScheduleTab.Schedule -> {
                            activeSheet = ScheduleSheet.AddScheduleEventCreate
                            addScheduleEventState = AddScheduleEventUiState.sample().copy(
                                eventDate = selectedDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                            )
                        }

                        ScheduleTab.Checklist -> {
                            activeSheet = ScheduleSheet.AddChecklist
                            addChecklistState = if (selectedDate != null) {
                                AddChecklistUiState.sample().copy(targetDate = selectedDate)
                            } else {
                                AddChecklistUiState.sample()
                            }
                            addChecklistValidationMessage = null
                        }
                    }
                }

                ScheduleAction.OnClickNotifications -> onOpenNotifications()

                is ScheduleAction.OnClickChecklistItemMore -> {
                    val selectedChecklist = uiState.checklist.groups
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .firstOrNull { it.id == action.itemId }
                    if (selectedChecklist != null) {
                        addChecklistState = selectedChecklist.toEditState()
                        addChecklistValidationMessage = null
                        activeSheet = ScheduleSheet.EditChecklist(action.itemId)
                    }
                }

                is ScheduleAction.OnClickUpcomingSchedule -> {
                    val selected = uiState.calendar.upcoming.firstOrNull { it.id == action.id }
                    if (selected != null && selected.source == UpcomingSource.Event) {
                        selectedEventDetail = selected
                    } else {
                        viewModel.onAction(action)
                    }
                }

                is ScheduleAction.OnClickUpcomingMore -> {
                    val selected = uiState.calendar.upcoming.firstOrNull { it.id == action.id }
                    if (selected != null && selected.source == UpcomingSource.Event) {
                        addScheduleEventState = selected.toEditState()
                        selectedEventDetail = null
                        activeSheet = ScheduleSheet.EditScheduleEvent(selected.sourceEntityId)
                    }
                }

                else -> viewModel.onAction(action)
            }
        }
    )

    when (val sheet = activeSheet) {
        null -> Unit
        ScheduleSheet.AddChecklist -> {
            AddChecklistBottomSheet(
                state = addChecklistState,
                onDismiss = { activeSheet = null },
                sheetTitle = "새 할 일",
                saveButtonText = "체크리스트에 추가",
                validationMessage = addChecklistValidationMessage,
                onAction = { act ->
                    when (act) {
                        AddChecklistAction.OnClickClose -> activeSheet = null
                        AddChecklistAction.OnClickSave -> {
                            val validationError = validateChecklistInput(addChecklistState)
                            if (validationError != null) {
                                addChecklistValidationMessage = validationError
                                return@AddChecklistBottomSheet
                            }
                            viewModel.onAction(
                                ScheduleAction.OnCreateChecklist(
                                    title = addChecklistState.title,
                                    dTag = addChecklistState.selectedDTag,
                                    targetDate = addChecklistState.targetDate,
                                    isCompleted = addChecklistState.isCompleted,
                                    memo = addChecklistState.memo,
                                )
                            )
                            activeSheet = null
                            addChecklistState = AddChecklistUiState.sample()
                            addChecklistValidationMessage = null
                        }

                        AddChecklistAction.OnClickTargetDate -> {
                            activeDatePickerTarget = DatePickerTarget.ChecklistTargetDate
                            addChecklistValidationMessage = null
                        }

                        AddChecklistAction.OnClickClearTargetDate -> {
                            addChecklistState = addChecklistState.copy(targetDate = null)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnSelectTargetDate -> {
                            addChecklistState = addChecklistState.copy(targetDate = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnTitleChange -> {
                            addChecklistState = addChecklistState.copy(
                                title = act.value.take(AddChecklistUiState.TITLE_MAX_LENGTH),
                            )
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnSelectDTag -> {
                            addChecklistState = addChecklistState.copy(selectedDTag = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnToggleCompleted -> {
                            addChecklistState = addChecklistState.copy(isCompleted = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnMemoChange -> {
                            addChecklistState = addChecklistState.copy(
                                memo = act.value.take(AddChecklistUiState.MEMO_MAX_LENGTH),
                            )
                            addChecklistValidationMessage = null
                        }
                    }
                }
            )
        }

        is ScheduleSheet.EditChecklist -> {
            val editingId = sheet.checklistId
            AddChecklistBottomSheet(
                state = addChecklistState,
                onDismiss = { activeSheet = null },
                sheetTitle = "할 일 수정",
                saveButtonText = "수정 저장",
                onClickDelete = {
                    pendingDeleteChecklistId = editingId
                },
                validationMessage = addChecklistValidationMessage,
                onAction = { act ->
                    when (act) {
                        AddChecklistAction.OnClickClose -> activeSheet = null
                        AddChecklistAction.OnClickSave -> {
                            val validationError = validateChecklistInput(addChecklistState)
                            if (validationError != null) {
                                addChecklistValidationMessage = validationError
                                return@AddChecklistBottomSheet
                            }
                            viewModel.onAction(
                                ScheduleAction.OnUpdateChecklist(
                                    id = editingId,
                                    title = addChecklistState.title,
                                    dTag = addChecklistState.selectedDTag,
                                    targetDate = addChecklistState.targetDate,
                                    isCompleted = addChecklistState.isCompleted,
                                    memo = addChecklistState.memo,
                                )
                            )
                            activeSheet = null
                            addChecklistState = AddChecklistUiState.sample()
                            addChecklistValidationMessage = null
                        }

                        AddChecklistAction.OnClickTargetDate -> {
                            activeDatePickerTarget = DatePickerTarget.ChecklistTargetDate
                            addChecklistValidationMessage = null
                        }

                        AddChecklistAction.OnClickClearTargetDate -> {
                            addChecklistState = addChecklistState.copy(targetDate = null)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnSelectTargetDate -> {
                            addChecklistState = addChecklistState.copy(targetDate = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnTitleChange -> {
                            addChecklistState = addChecklistState.copy(
                                title = act.value.take(AddChecklistUiState.TITLE_MAX_LENGTH),
                            )
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnSelectDTag -> {
                            addChecklistState = addChecklistState.copy(selectedDTag = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnToggleCompleted -> {
                            addChecklistState = addChecklistState.copy(isCompleted = act.value)
                            addChecklistValidationMessage = null
                        }

                        is AddChecklistAction.OnMemoChange -> {
                            addChecklistState = addChecklistState.copy(
                                memo = act.value.take(AddChecklistUiState.MEMO_MAX_LENGTH),
                            )
                            addChecklistValidationMessage = null
                        }
                    }
                }
            )
        }

        ScheduleSheet.AddScheduleEventCreate -> {
            AddScheduleEventBottomSheet(
                state = addScheduleEventState,
                onDismiss = { activeSheet = null },
                sheetTitle = "새 일정",
                saveButtonText = "일정 저장",
                onAction = { act ->
                    when (act) {
                        AddScheduleEventAction.OnClickClose -> activeSheet = null
                        AddScheduleEventAction.OnClickSave -> {
                            val eventDate = addScheduleEventState.eventDate
                            if (eventDate == null) return@AddScheduleEventBottomSheet
                            viewModel.onAction(
                                ScheduleAction.OnCreateScheduleEvent(
                                    title = addScheduleEventState.title,
                                    eventDate = eventDate,
                                    startTime = addScheduleEventState.startTime,
                                    location = addScheduleEventState.location,
                                    memo = addScheduleEventState.memo,
                                    repeatRule = addScheduleEventState.repeatRule,
                                )
                            )
                            activeSheet = null
                            addScheduleEventState = AddScheduleEventUiState.sample()
                        }

                        AddScheduleEventAction.OnClickEventDate -> {
                            activeDatePickerTarget = DatePickerTarget.EventDate
                        }

                        is AddScheduleEventAction.OnTitleChange -> {
                            addScheduleEventState = addScheduleEventState.copy(title = act.value)
                        }

                        is AddScheduleEventAction.OnStartTimeChange -> {
                            addScheduleEventState = addScheduleEventState.copy(
                                startTime = maskStartTime24hInput(act.value),
                            )
                        }

                        is AddScheduleEventAction.OnLocationChange -> {
                            addScheduleEventState = addScheduleEventState.copy(location = act.value)
                        }

                        is AddScheduleEventAction.OnMemoChange -> {
                            addScheduleEventState = addScheduleEventState.copy(memo = act.value)
                        }

                        is AddScheduleEventAction.OnSelectRepeatRule -> {
                            addScheduleEventState = addScheduleEventState.copy(repeatRule = act.value)
                        }
                    }
                }
            )
        }

        is ScheduleSheet.EditScheduleEvent -> {
            val editingId = sheet.eventId
            AddScheduleEventBottomSheet(
                state = addScheduleEventState,
                onDismiss = { activeSheet = null },
                sheetTitle = "일정 수정",
                saveButtonText = "수정 저장",
                onClickDelete = {
                    pendingDeleteScheduleEventId = editingId
                },
                onAction = { act ->
                    when (act) {
                        AddScheduleEventAction.OnClickClose -> activeSheet = null
                        AddScheduleEventAction.OnClickSave -> {
                            val eventDate = addScheduleEventState.eventDate
                            if (eventDate == null) return@AddScheduleEventBottomSheet
                            viewModel.onAction(
                                ScheduleAction.OnUpdateScheduleEvent(
                                    id = editingId,
                                    title = addScheduleEventState.title,
                                    eventDate = eventDate,
                                    startTime = addScheduleEventState.startTime,
                                    location = addScheduleEventState.location,
                                    memo = addScheduleEventState.memo,
                                    repeatRule = addScheduleEventState.repeatRule,
                                )
                            )
                            activeSheet = null
                            addScheduleEventState = AddScheduleEventUiState.sample()
                        }

                        AddScheduleEventAction.OnClickEventDate -> {
                            activeDatePickerTarget = DatePickerTarget.EventDate
                        }

                        is AddScheduleEventAction.OnTitleChange -> {
                            addScheduleEventState = addScheduleEventState.copy(title = act.value)
                        }

                        is AddScheduleEventAction.OnStartTimeChange -> {
                            addScheduleEventState = addScheduleEventState.copy(
                                startTime = maskStartTime24hInput(act.value),
                            )
                        }

                        is AddScheduleEventAction.OnLocationChange -> {
                            addScheduleEventState = addScheduleEventState.copy(location = act.value)
                        }

                        is AddScheduleEventAction.OnMemoChange -> {
                            addScheduleEventState = addScheduleEventState.copy(memo = act.value)
                        }

                        is AddScheduleEventAction.OnSelectRepeatRule -> {
                            addScheduleEventState = addScheduleEventState.copy(repeatRule = act.value)
                        }
                    }
                }
            )
        }
    }

    selectedEventDetail?.let { item ->
        ScheduleEventDetailBottomSheet(
            item = item,
            onDismiss = { selectedEventDetail = null },
            onEdit = {
                addScheduleEventState = item.toEditState()
                selectedEventDetail = null
                activeSheet = ScheduleSheet.EditScheduleEvent(item.sourceEntityId)
            }
        )
    }

    activeDatePickerTarget?.let { target ->
        val timeZone = TimeZone.UTC
        val initialDate = when (target) {
            DatePickerTarget.ChecklistTargetDate -> addChecklistState.targetDate
            DatePickerTarget.EventDate -> addScheduleEventState.eventDate
        } ?: Clock.System.now().toLocalDateTime(timeZone).date

        val initialMillis = initialDate
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { activeDatePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant
                                .fromEpochMilliseconds(millis)
                                .toLocalDateTime(timeZone)
                                .date
                            when (target) {
                                DatePickerTarget.ChecklistTargetDate -> {
                                    addChecklistState = addChecklistState.copy(targetDate = date)
                                }

                                DatePickerTarget.EventDate -> {
                                    addScheduleEventState = addScheduleEventState.copy(eventDate = date)
                                }
                            }
                        }
                        activeDatePickerTarget = null
                    }
                ) {
                    Text(text = "확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerTarget = null }) {
                    Text(text = "취소")
                }
            },
        ) {
            DatePicker(state = datePickerState)
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
                        activeSheet = null
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

    pendingDeleteScheduleEventId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteScheduleEventId = null },
            title = { Text(text = "일정을 삭제할까요?") },
            text = { Text(text = "선택한 일정은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(ScheduleAction.OnDeleteScheduleEvent(id))
                        pendingDeleteScheduleEventId = null
                        activeSheet = null
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteScheduleEventId = null }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

private sealed interface ScheduleSheet {
    data object AddChecklist : ScheduleSheet
    data class EditChecklist(val checklistId: String) : ScheduleSheet
    data object AddScheduleEventCreate : ScheduleSheet
    data class EditScheduleEvent(val eventId: String) : ScheduleSheet
}

private enum class DatePickerTarget {
    ChecklistTargetDate,
    EventDate,
}

private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private fun UpcomingScheduleItem.toEditState(): AddScheduleEventUiState {
    return AddScheduleEventUiState.sample().copy(
        title = title,
        eventDate = date,
        startTime = eventStartTime.orEmpty(),
        location = eventLocation.orEmpty(),
        memo = eventMemo.orEmpty(),
        repeatRule = repeatRule,
    )
}

private fun ChecklistItem.toEditState(): AddChecklistUiState {
    return AddChecklistUiState.sample().copy(
        title = title,
        targetDate = targetDate,
        selectedDTag = dTag?.takeIf { it.isNotBlank() } ?: AddChecklistUiState.sample().selectedDTag,
        isCompleted = isDone,
        memo = memo.orEmpty(),
    )
}

private fun validateChecklistInput(state: AddChecklistUiState): String? {
    val normalizedTitle = state.title.trim()
    if (normalizedTitle.length < 2) {
        return "할 일 제목은 2자 이상 입력해주세요."
    }
    if (normalizedTitle.length > AddChecklistUiState.TITLE_MAX_LENGTH) {
        return "할 일 제목은 ${AddChecklistUiState.TITLE_MAX_LENGTH}자 이하로 입력해주세요."
    }
    if (!state.selectedDTag.isValidDTagValue()) {
        return "D-Tag 형식이 올바르지 않습니다. (예: D-30)"
    }
    if (state.memo.trim().length > AddChecklistUiState.MEMO_MAX_LENGTH) {
        return "메모는 ${AddChecklistUiState.MEMO_MAX_LENGTH}자 이하로 입력해주세요."
    }
    return null
}

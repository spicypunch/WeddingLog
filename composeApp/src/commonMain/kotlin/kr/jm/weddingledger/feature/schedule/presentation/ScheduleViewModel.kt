package kr.jm.weddingledger.feature.schedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kr.jm.weddingledger.feature.notifications.domain.usecase.CancelReminderUseCase
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchScheduleEventsUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.SetChecklistCheckedUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateScheduleEventUseCase

class ScheduleViewModel(
    private val fetchChecklistUseCase: FetchChecklistUseCase,
    private val addChecklistUseCase: AddChecklistUseCase,
    private val updateChecklistUseCase: UpdateChecklistUseCase,
    private val setChecklistCheckedUseCase: SetChecklistCheckedUseCase,
    private val deleteChecklistUseCase: DeleteChecklistUseCase,
    private val fetchScheduleEventsUseCase: FetchScheduleEventsUseCase,
    private val addScheduleEventUseCase: AddScheduleEventUseCase,
    private val updateScheduleEventUseCase: UpdateScheduleEventUseCase,
    private val deleteScheduleEventUseCase: DeleteScheduleEventUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase,
) : ViewModel() {

    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private var currentMonth: LocalDate = LocalDate(today.year, today.month, 1)
    private var selectedDayId: String? = null
    private var calendarSearchQuery: String = ""
    private var upcomingSortType: UpcomingSortType = UpcomingSortType.DateAsc

    private var checklistTasks: List<ChecklistTask> = emptyList()
    private var scheduleEvents: List<ScheduleEvent> = emptyList()

    private val _state = MutableStateFlow(
        ScheduleUiState(
            selectedTab = ScheduleTab.Schedule,
            calendar = buildCalendarState(
                month = currentMonth,
                selectedDayId = selectedDayId,
                referenceDate = today,
                searchQuery = calendarSearchQuery,
                sortType = upcomingSortType,
                checklistTasks = checklistTasks,
                scheduleEvents = scheduleEvents,
            ),
            checklist = ChecklistUiState.empty(),
        )
    )
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        refreshData(showLoading = true)
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            is ScheduleAction.OnSelectTab -> _state.update { it.copy(selectedTab = action.tab) }
            ScheduleAction.OnClickPrevMonth -> shiftMonth(-1)
            ScheduleAction.OnClickNextMonth -> shiftMonth(1)
            ScheduleAction.OnClickNotifications -> Unit // handled in Route
            is ScheduleAction.OnSelectCalendarDay -> selectCalendarDay(action.dayId)
            is ScheduleAction.OnCalendarQueryChange -> updateCalendarQuery(action.value)
            is ScheduleAction.OnSelectUpcomingSort -> updateUpcomingSort(action.value)
            ScheduleAction.OnClickViewAllUpcoming -> clearSelectedDayFilter()
            is ScheduleAction.OnClickUpcomingSchedule -> focusUpcomingSchedule(action.id)
            is ScheduleAction.OnClickUpcomingMore -> Unit // handled in Route (bottom sheet)
            is ScheduleAction.OnToggleChecklistItem -> toggleChecklistItem(action.itemId)
            is ScheduleAction.OnClickChecklistItemMore -> Unit // handled in Route (dialog)
            ScheduleAction.OnClickAdd -> Unit // handled in Route (bottom sheet)

            ScheduleAction.OnRefresh -> refreshData(showLoading = false, isRefreshing = true)
            ScheduleAction.OnRefreshChecklist -> refreshData(showLoading = true)
            is ScheduleAction.OnCreateChecklist -> createChecklist(action)
            is ScheduleAction.OnUpdateChecklist -> updateChecklist(action)
            is ScheduleAction.OnCreateScheduleEvent -> createScheduleEvent(action)
            is ScheduleAction.OnUpdateScheduleEvent -> updateScheduleEvent(action)
            is ScheduleAction.OnDeleteScheduleEvent -> deleteScheduleEvent(action.id)
            is ScheduleAction.OnDeleteChecklist -> deleteChecklist(action.id)
        }
    }

    private fun selectCalendarDay(dayId: String) {
        val selectedDate = parseDateOrNull(dayId) ?: return
        selectedDayId = selectedDate.toString()
        _state.update { current ->
            current.copy(
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun clearSelectedDayFilter() {
        if (selectedDayId == null) return
        selectedDayId = null

        _state.update { current ->
            current.copy(
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun focusUpcomingSchedule(itemId: String) {
        val date = _state.value.calendar.upcoming
            .firstOrNull { it.id == itemId }
            ?.date
            ?: return

        currentMonth = LocalDate(date.year, date.month, 1)
        selectedDayId = date.toString()

        _state.update { current ->
            current.copy(
                selectedTab = ScheduleTab.Schedule,
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun updateCalendarQuery(value: String) {
        calendarSearchQuery = value
        _state.update { current ->
            current.copy(
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun updateUpcomingSort(value: UpcomingSortType) {
        upcomingSortType = value
        _state.update { current ->
            current.copy(
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun refreshData(showLoading: Boolean, isRefreshing: Boolean = false) {
        _state.update { current ->
            current.copy(
                isRefreshing = isRefreshing,
                checklist = current.checklist.copy(
                    isLoading = showLoading,
                    message = null,
                ),
                calendar = current.calendar.copy(message = null),
            )
        }

        viewModelScope.launch {
            var checklistMessage: String? = null
            var calendarMessage: String? = null

            when (val checklistResult = fetchChecklistUseCase()) {
                is ScheduleResult.Success -> checklistTasks = checklistResult.data
                is ScheduleResult.Error -> checklistMessage = checklistResult.message
            }

            when (val eventsResult = fetchScheduleEventsUseCase()) {
                is ScheduleResult.Success -> scheduleEvents = eventsResult.data
                is ScheduleResult.Error -> calendarMessage = eventsResult.message
            }

            val checklistUi = checklistTasks.toChecklistUiState()
            _state.update {
                it.copy(
                    isRefreshing = false,
                    checklist = checklistUi.copy(
                        isLoading = false,
                        message = checklistMessage,
                    ),
                    calendar = buildCalendarState(
                        month = currentMonth,
                        selectedDayId = selectedDayId,
                        referenceDate = today,
                        searchQuery = calendarSearchQuery,
                        sortType = upcomingSortType,
                        checklistTasks = checklistTasks,
                        scheduleEvents = scheduleEvents,
                    ).copy(message = calendarMessage),
                )
            }
        }
    }

    private fun shiftMonth(delta: Int) {
        currentMonth = addMonths(currentMonth, delta)
        val selectedDate = parseDateOrNull(selectedDayId.orEmpty())
        if (selectedDate != null && !isDateInMonth(selectedDate, currentMonth)) {
            selectedDayId = null
        }

        val current = _state.value
        _state.update {
            it.copy(
                calendar = buildCalendarState(
                    month = currentMonth,
                    selectedDayId = selectedDayId,
                    referenceDate = today,
                    searchQuery = calendarSearchQuery,
                    sortType = upcomingSortType,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                ).copy(message = current.calendar.message)
            )
        }
    }

    private fun createChecklist(action: ScheduleAction.OnCreateChecklist) {
        _state.update { current ->
            current.copy(
                isRefreshing = false,
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
                targetDate = action.targetDate,
                memo = action.memo,
            )

            when (val result = addChecklistUseCase(task)) {
                is ScheduleResult.Success -> refreshData(showLoading = false)
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
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

    private fun updateChecklist(action: ScheduleAction.OnUpdateChecklist) {
        _state.update { current ->
            current.copy(
                isRefreshing = false,
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
                targetDate = action.targetDate,
                memo = action.memo,
            )

            when (val result = updateChecklistUseCase(action.id, task)) {
                is ScheduleResult.Success -> {
                    cancelChecklistReminder(action.id)
                    refreshData(showLoading = false)
                }
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
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

    private fun createScheduleEvent(action: ScheduleAction.OnCreateScheduleEvent) {
        _state.update { current ->
            current.copy(
                isRefreshing = false,
                calendar = current.calendar.copy(message = null)
            )
        }

        viewModelScope.launch {
            val event = NewScheduleEvent(
                title = action.title,
                eventDate = action.eventDate,
                startTime = action.startTime,
                location = action.location,
                memo = action.memo,
                repeatRule = action.repeatRule,
            )

            when (val result = addScheduleEventUseCase(event)) {
                is ScheduleResult.Success -> refreshData(showLoading = false)
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
                            calendar = current.calendar.copy(message = result.message)
                        )
                    }
                }
            }
        }
    }

    private fun updateScheduleEvent(action: ScheduleAction.OnUpdateScheduleEvent) {
        _state.update { current ->
            current.copy(
                isRefreshing = false,
                calendar = current.calendar.copy(message = null)
            )
        }

        viewModelScope.launch {
            val event = NewScheduleEvent(
                title = action.title,
                eventDate = action.eventDate,
                startTime = action.startTime,
                location = action.location,
                memo = action.memo,
                repeatRule = action.repeatRule,
            )

            when (val result = updateScheduleEventUseCase(action.id, event)) {
                is ScheduleResult.Success -> {
                    cancelScheduleEventReminder(action.id)
                    refreshData(showLoading = false)
                }
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
                            calendar = current.calendar.copy(message = result.message)
                        )
                    }
                }
            }
        }
    }

    private fun deleteScheduleEvent(id: String) {
        _state.update { current ->
            current.copy(
                isRefreshing = false,
                calendar = current.calendar.copy(message = null)
            )
        }

        viewModelScope.launch {
            when (val result = deleteScheduleEventUseCase(id)) {
                is ScheduleResult.Success -> {
                    cancelScheduleEventReminder(id)
                    refreshData(showLoading = false)
                }
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
                            calendar = current.calendar.copy(message = result.message)
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
                is ScheduleResult.Success -> {
                    if (newChecked) {
                        cancelChecklistReminder(itemId)
                    }
                    refreshData(showLoading = false)
                }
                is ScheduleResult.Error -> {
                    // 서버 반영 실패 시 optimistic update를 원복합니다.
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
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
                isRefreshing = false,
                checklist = current.checklist.copy(isLoading = true, message = null)
            )
        }

        viewModelScope.launch {
            when (val result = deleteChecklistUseCase(id)) {
                is ScheduleResult.Success -> {
                    cancelChecklistReminder(id)
                    refreshData(showLoading = false)
                }
                is ScheduleResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isRefreshing = false,
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

    private suspend fun cancelScheduleEventReminder(eventId: String) {
        val reminderId = toEventReminderId(eventId)
        cancelReminderUseCase(reminderId)
    }

    private suspend fun cancelChecklistReminder(checklistId: String) {
        val reminderId = toChecklistReminderId(checklistId)
        cancelReminderUseCase(reminderId)
    }
}

private fun toEventReminderId(eventId: String): String = "event-reminder-$eventId"

private fun toChecklistReminderId(checklistId: String): String = "checklist-reminder-$checklistId"

private fun buildCalendarState(
    month: LocalDate,
    selectedDayId: String?,
    referenceDate: LocalDate,
    searchQuery: String,
    sortType: UpcomingSortType,
    checklistTasks: List<ChecklistTask>,
    scheduleEvents: List<ScheduleEvent>,
): CalendarUiState {
    val selectedDate = parseDateOrNull(selectedDayId.orEmpty())
    val firstDay = LocalDate(month.year, month.month, 1)
    val previousMonth = addMonths(firstDay, -1)
    val nextMonth = addMonths(firstDay, 1)

    val cells = mutableListOf<CalendarDayCell>()
    val leadingDayCount = (firstDay.dayOfWeek.ordinal + 1) % 7

    val previousMonthDays = daysInMonth(previousMonth)
    val firstPrevDay = previousMonthDays - leadingDayCount + 1
    for (day in firstPrevDay..previousMonthDays) {
        if (day >= 1) {
            cells.add(
                buildDayCell(
                    date = LocalDate(previousMonth.year, previousMonth.month, day),
                    inMonth = false,
                    selectedDate = selectedDate,
                    selectedDayId = selectedDayId,
                    referenceDate = referenceDate,
                    checklistTasks = checklistTasks,
                    scheduleEvents = scheduleEvents,
                )
            )
        }
    }

    val currentMonthDays = daysInMonth(firstDay)
    for (day in 1..currentMonthDays) {
        cells.add(
            buildDayCell(
                date = LocalDate(firstDay.year, firstDay.month, day),
                inMonth = true,
                selectedDate = selectedDate,
                selectedDayId = selectedDayId,
                referenceDate = referenceDate,
                checklistTasks = checklistTasks,
                scheduleEvents = scheduleEvents,
            )
        )
    }

    val trailingDayCount = (7 - (cells.size % 7)) % 7
    for (day in 1..trailingDayCount) {
        cells.add(
            buildDayCell(
                date = LocalDate(nextMonth.year, nextMonth.month, day),
                inMonth = false,
                selectedDate = selectedDate,
                selectedDayId = selectedDayId,
                referenceDate = referenceDate,
                checklistTasks = checklistTasks,
                scheduleEvents = scheduleEvents,
            )
        )
    }

    val selectedUpcomingTitle = selectedDate?.let {
        "${formatLocalDate(it)} 일정 (${toRelativeDayTag(referenceDate, it)})"
    }
    val selectedDayItems = selectedDate?.let {
        buildUpcomingItems(
            checklistTasks = checklistTasks,
            scheduleEvents = scheduleEvents,
            today = referenceDate,
            selectedDate = it,
            searchQuery = searchQuery,
            sortType = sortType,
        )
    }.orEmpty()

    return CalendarUiState(
        monthLabel = "${firstDay.year}년 ${firstDay.month.number}월",
        dayOfWeekLabels = listOf("일", "월", "화", "수", "목", "금", "토"),
        cells = cells,
        upcoming = buildUpcomingItems(
            checklistTasks = checklistTasks,
            scheduleEvents = scheduleEvents,
            today = referenceDate,
            selectedDate = null,
            searchQuery = searchQuery,
            sortType = sortType,
        ),
        upcomingTitle = "다가오는 일정",
        upcomingEmptyText = "예정된 일정이 없어요.",
        selectedDayTitle = selectedUpcomingTitle,
        selectedDayItems = selectedDayItems,
        selectedDayEmptyText = "선택한 날짜에 일정이 없어요.",
        checklistFocusItems = buildChecklistFocusItems(
            checklistTasks = checklistTasks,
            today = referenceDate,
            searchQuery = searchQuery,
        ),
        selectedDayId = selectedDate?.toString() ?: selectedDayId,
        searchQuery = searchQuery,
        sortType = sortType,
    )
}

private fun buildDayCell(
    date: LocalDate,
    inMonth: Boolean,
    selectedDate: LocalDate?,
    selectedDayId: String?,
    referenceDate: LocalDate,
    checklistTasks: List<ChecklistTask>,
    scheduleEvents: List<ScheduleEvent>,
): CalendarDayCell {
    val dayTasks = checklistTasks.filter { it.targetDate == date }
    val dayEventCount = scheduleEvents.count { event -> occursOnDate(event, date) }
    val markerCount = dayTasks.size + dayEventCount

    val dotStyle = when {
        markerCount == 0 -> null
        dayTasks.any { !it.isChecked } || dayEventCount > 0 -> CalendarDotStyle.Primary
        else -> CalendarDotStyle.Neutral
    }

    val isSelected = selectedDate == date && selectedDayId == date.toString()
    val dDayLabel = if (isSelected && inMonth) toRelativeDayTag(referenceDate, date) else null

    return CalendarDayCell(
        id = date.toString(),
        day = date.day,
        inMonth = inMonth,
        isSelected = isSelected,
        isDDay = false,
        dDayLabel = dDayLabel,
        textTone = if (inMonth) CalendarDayTextTone.Normal else CalendarDayTextTone.Muted,
        backgroundTone = CalendarDayBackgroundTone.None,
        dotStyle = dotStyle,
        markerCount = markerCount,
    )
}

private fun buildUpcomingItems(
    checklistTasks: List<ChecklistTask>,
    scheduleEvents: List<ScheduleEvent>,
    today: LocalDate,
    selectedDate: LocalDate?,
    searchQuery: String,
    sortType: UpcomingSortType,
): List<UpcomingScheduleItem> {
    val checklistItems = checklistTasks
        .mapNotNull { task ->
            val date = task.targetDate ?: return@mapNotNull null
            UpcomingScheduleItem(
                id = "checklist-${task.id}",
                source = UpcomingSource.Checklist,
                sourceEntityId = task.id,
                date = date,
                monthAbbr = "${date.month.number}월",
                dayOfMonth = date.day.toString().padStart(2, '0'),
                title = task.title,
                subtitle = when {
                    task.isChecked -> "완료"
                    !task.memo.isNullOrBlank() -> task.memo.trim()
                    else -> "체크리스트"
                },
                dTagText = toRelativeDayTag(today, date),
                tagTone = if (task.isChecked) UpcomingTagTone.Neutral else UpcomingTagTone.Primary,
                isCompleted = task.isChecked,
            )
        }

    val horizon = addDays(today, UPCOMING_HORIZON_DAYS)
    val eventItems = scheduleEvents
        .flatMap { event ->
            expandOccurrences(
                event = event,
                today = today,
                selectedDate = selectedDate,
                horizon = horizon,
            )
        }
        .map { occurrence ->
            val date = occurrence.date
            val repeatText = occurrence.repeatRule.toLabel().takeIf { occurrence.repeatRule != EventRepeatRule.None }
            val subtitle = buildList {
                repeatText?.let(::add)
                occurrence.location?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
                occurrence.startTime?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
            }.joinToString(" · ").ifBlank {
                occurrence.memo?.trim()?.takeIf { it.isNotEmpty() } ?: "일정"
            }
            val isCompleted = date < today

            UpcomingScheduleItem(
                id = "event-${occurrence.sourceEventId}-${date}",
                source = UpcomingSource.Event,
                sourceEntityId = occurrence.sourceEventId,
                date = date,
                monthAbbr = "${date.month.number}월",
                dayOfMonth = date.day.toString().padStart(2, '0'),
                title = occurrence.title,
                subtitle = subtitle,
                dTagText = toRelativeDayTag(today, date),
                tagTone = if (isCompleted) UpcomingTagTone.Neutral else UpcomingTagTone.Primary,
                isCompleted = isCompleted,
                eventStartTime = occurrence.startTime,
                eventLocation = occurrence.location,
                eventMemo = occurrence.memo,
                repeatRule = occurrence.repeatRule,
            )
        }

    val base = (checklistItems + eventItems).asSequence()
        .filter { item ->
            if (selectedDate != null) {
                item.date == selectedDate
            } else {
                item.date >= today
            }
        }

    val query = searchQuery.trim()
    val searched = if (query.isBlank()) {
        base.toList()
    } else {
        base.filter { item ->
            buildString {
                append(item.title)
                append(' ')
                append(item.subtitle)
                append(' ')
                append(item.dTagText)
                append(' ')
                append(item.eventLocation.orEmpty())
                append(' ')
                append(item.eventMemo.orEmpty())
            }.contains(query, ignoreCase = true)
        }.toList()
    }

    val sorted = when (sortType) {
        UpcomingSortType.DateAsc -> searched.sortedWith(
            compareBy<UpcomingScheduleItem> { it.date }
                .thenBy { if (it.source == UpcomingSource.Event) 0 else 1 }
                .thenBy { it.title }
        )

        UpcomingSortType.DateDesc -> searched.sortedWith(
            compareByDescending<UpcomingScheduleItem> { it.date }
                .thenBy { if (it.source == UpcomingSource.Event) 0 else 1 }
                .thenBy { it.title }
        )

        UpcomingSortType.Title -> searched.sortedWith(
            compareBy<UpcomingScheduleItem> { it.title }
                .thenBy { it.date }
        )
    }

    return sorted.take(if (selectedDate != null || query.isNotBlank()) 50 else 6)
}

private fun buildChecklistFocusItems(
    checklistTasks: List<ChecklistTask>,
    today: LocalDate,
    searchQuery: String,
): List<ChecklistItem> {
    val query = searchQuery.trim()
    val filtered = checklistTasks.asSequence()
        .filterNot { it.isChecked }
        .filter { task ->
            if (query.isBlank()) {
                true
            } else {
                buildString {
                    append(task.title)
                    append(' ')
                    append(task.memo.orEmpty())
                    append(' ')
                    append(task.dTag.orEmpty())
                    append(' ')
                    append(task.groupTitle.orEmpty())
                    append(' ')
                    task.targetDate?.let(::append)
                }.contains(query, ignoreCase = true)
            }
        }
        .sortedWith(
            compareBy<ChecklistTask>(
                { it.targetDate == null },
                { it.targetDate ?: LocalDate(9999, Month.DECEMBER, 31) },
                { it.dTag.sortOrderForPreview() },
                { it.title },
            )
        )
        .map { it.toCalendarChecklistFocusItem(today) }
        .toList()

    return filtered.take(if (query.isBlank()) 4 else 12)
}

private data class EventOccurrence(
    val sourceEventId: String,
    val title: String,
    val date: LocalDate,
    val startTime: String?,
    val location: String?,
    val memo: String?,
    val repeatRule: EventRepeatRule,
)

private fun expandOccurrences(
    event: ScheduleEvent,
    today: LocalDate,
    selectedDate: LocalDate?,
    horizon: LocalDate,
): List<EventOccurrence> {
    if (selectedDate != null) {
        if (!occursOnDate(event, selectedDate)) return emptyList()
        return listOf(
            EventOccurrence(
                sourceEventId = event.id,
                title = event.title,
                date = selectedDate,
                startTime = event.startTime,
                location = event.location,
                memo = event.memo,
                repeatRule = event.repeatRule,
            )
        )
    }

    val first = firstOccurrenceOnOrAfter(event = event, pivot = today) ?: return emptyList()
    if (first > horizon) return emptyList()

    val result = mutableListOf<EventOccurrence>()
    var current = first
    while (current <= horizon) {
        result.add(
            EventOccurrence(
                sourceEventId = event.id,
                title = event.title,
                date = current,
                startTime = event.startTime,
                location = event.location,
                memo = event.memo,
                repeatRule = event.repeatRule,
            )
        )
        current = nextOccurrenceDate(event, current) ?: break
    }
    return result
}

private fun occursOnDate(
    event: ScheduleEvent,
    date: LocalDate,
): Boolean {
    if (date < event.eventDate) return false
    return when (event.repeatRule) {
        EventRepeatRule.None -> date == event.eventDate
        EventRepeatRule.Weekly -> event.eventDate.daysUntil(date) % 7 == 0
        EventRepeatRule.Monthly -> {
            var current = event.eventDate
            while (current < date) {
                current = nextOccurrenceDate(event, current) ?: break
            }
            current == date
        }
    }
}

private fun firstOccurrenceOnOrAfter(
    event: ScheduleEvent,
    pivot: LocalDate,
): LocalDate? {
    return when (event.repeatRule) {
        EventRepeatRule.None -> event.eventDate.takeIf { it >= pivot }
        EventRepeatRule.Weekly -> {
            if (event.eventDate >= pivot) {
                event.eventDate
            } else {
                val diff = event.eventDate.daysUntil(pivot)
                val remain = diff % 7
                if (remain == 0) pivot else addDays(pivot, 7 - remain)
            }
        }

        EventRepeatRule.Monthly -> {
            var current = event.eventDate
            while (current < pivot) {
                current = nextOccurrenceDate(event, current) ?: return null
            }
            current
        }
    }
}

private fun nextOccurrenceDate(
    event: ScheduleEvent,
    current: LocalDate,
): LocalDate? {
    return when (event.repeatRule) {
        EventRepeatRule.None -> null
        EventRepeatRule.Weekly -> addDays(current, 7)
        EventRepeatRule.Monthly -> {
            val nextMonthStart = LocalDate(current.year, current.month, 1).plus(1, DateTimeUnit.MONTH)
            val targetDay = minOf(event.eventDate.day, daysInMonth(nextMonthStart))
            LocalDate(nextMonthStart.year, nextMonthStart.month, targetDay)
        }
    }
}

private fun addMonths(monthStart: LocalDate, delta: Int): LocalDate {
    if (delta == 0) return LocalDate(monthStart.year, monthStart.month, monthStart.day)

    return if (delta > 0) {
        var current = monthStart
        repeat(delta) {
            current = current.plus(1, DateTimeUnit.MONTH)
        }
        current
    } else {
        var current = monthStart
        repeat(delta.absoluteValue) {
            current = current.plus(-1, DateTimeUnit.MONTH)
        }
        current
    }
}

private fun daysInMonth(monthStart: LocalDate): Int {
    return when (monthStart.month) {
        Month.JANUARY,
        Month.MARCH,
        Month.MAY,
        Month.JULY,
        Month.AUGUST,
        Month.OCTOBER,
        Month.DECEMBER -> 31
        Month.APRIL,
        Month.JUNE,
        Month.SEPTEMBER,
        Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(monthStart.year)) 29 else 28
    }
}

private fun isLeapYear(year: Int): Boolean {
    return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
}

private fun isDateInMonth(date: LocalDate, month: LocalDate): Boolean {
    return date.year == month.year && date.month == month.month
}

private fun parseDateOrNull(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private fun List<ChecklistTask>.toChecklistUiState(): ChecklistUiState {
    if (isEmpty()) return ChecklistUiState.empty()

    val grouped = groupBy { it.dTag?.trim().orEmpty().ifEmpty { "기타" } }
    val maxDTagNumber = grouped.keys.mapNotNull { parseDTagNumberOrNull(it) }.maxOrNull()

    val groups = grouped
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, List<ChecklistTask>>> { parseDTagNumberOrNull(it.key) ?: Int.MIN_VALUE }
                .thenBy { it.key },
        )
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
        dTag = dTag,
        title = title,
        targetDate = targetDate,
        memo = memo,
        subtitle = subtitle,
        subtitleTone = subtitleTone,
        isDone = isDone,
    )
}

private fun ChecklistTask.toCalendarChecklistFocusItem(
    today: LocalDate,
): ChecklistItem {
    val dueDate = targetDate
    val relativeTag = dueDate?.let { toRelativeDayTag(today, it) }
    val dueDateText = dueDate?.let(::formatLocalDate)
    val groupText = groupTitle?.trim()?.takeIf { it.isNotEmpty() } ?: dTag?.trim()?.takeIf { it.isNotEmpty() }
    val subtitle = buildList {
        relativeTag?.let(::add)
        dueDateText?.let(::add)
        groupText?.let(::add)
        if (isEmpty()) {
            memo?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
        }
    }.joinToString(" · ").ifBlank { memo?.trim()?.takeIf { it.isNotEmpty() } }

    val isUrgent = dueDate?.let { it <= addDays(today, 7) } ?: false

    return ChecklistItem(
        id = id,
        dTag = dTag,
        title = title,
        targetDate = targetDate,
        memo = memo,
        subtitle = subtitle,
        subtitleTone = if (isUrgent) ChecklistItemSubtitleTone.Primary else ChecklistItemSubtitleTone.Muted,
        isDone = false,
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

private fun String?.sortOrderForPreview(): Int {
    val parsed = this?.let(::parseDTagNumberOrNull) ?: return Int.MAX_VALUE
    return if (parsed >= 0) parsed else Int.MAX_VALUE / 2 + parsed.absoluteValue
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

private fun toRelativeDayTag(referenceDate: LocalDate, targetDate: LocalDate): String {
    val diff = referenceDate.daysUntil(targetDate)
    return when {
        diff > 0 -> "D-$diff"
        diff == 0 -> "D-DAY"
        else -> "D+${diff.absoluteValue}"
    }
}

private fun formatLocalDate(date: LocalDate): String {
    val mm = date.month.number.toString().padStart(2, '0')
    val dd = date.day.toString().padStart(2, '0')
    return "${date.year}.$mm.$dd"
}

private fun addDays(
    date: LocalDate,
    days: Int,
): LocalDate = date.plus(days, DateTimeUnit.DAY)

private fun EventRepeatRule.toLabel(): String {
    return when (this) {
        EventRepeatRule.None -> "반복 없음"
        EventRepeatRule.Weekly -> "매주 반복"
        EventRepeatRule.Monthly -> "매월 반복"
    }
}

private const val UPCOMING_HORIZON_DAYS: Int = 180

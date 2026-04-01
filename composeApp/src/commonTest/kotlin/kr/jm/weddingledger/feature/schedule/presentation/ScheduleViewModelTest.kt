package kr.jm.weddingledger.feature.schedule.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository
import kr.jm.weddingledger.feature.notifications.domain.usecase.CancelReminderUseCase
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.AddScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.DeleteScheduleEventUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchScheduleEventsUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.SetChecklistCheckedUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.UpdateScheduleEventUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class ScheduleViewModelTest {

    @Test
    fun `initial refresh populates checklist and upcoming schedule`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val checklistRepository = FakeChecklistRepositoryForViewModel(
            initialTasks = listOf(
                ChecklistTask(
                    id = "task-1",
                    title = "예식장 계약",
                    dTag = "D-30",
                    groupTitle = "본식 준비",
                    isChecked = false,
                    targetDate = today,
                    memo = null,
                )
            )
        )
        val eventRepository = FakeScheduleEventRepositoryForViewModel(
            initialEvents = listOf(
                ScheduleEvent(
                    id = "event-1",
                    title = "스튜디오 촬영",
                    eventDate = today.plus(1, DateTimeUnit.DAY),
                    startTime = "10:00",
                    location = "강남",
                    memo = null,
                    repeatRule = EventRepeatRule.None,
                )
            )
        )
        val notificationRepository = FakeNotificationCenterRepositoryForViewModel()

        val viewModel = createViewModel(checklistRepository, eventRepository, notificationRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.checklist.totalCount)
        assertTrue(state.calendar.upcoming.isNotEmpty())
        assertEquals("예식장 계약", state.calendar.upcoming.first().title)
        assertTrue(state.calendar.upcoming.any { it.title == "스튜디오 촬영" })
        assertEquals("예식장 계약", state.calendar.checklistFocusItems.first().title)
    }

    @Test
    fun `toggle checklist to done cancels reminder`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val checklistRepository = FakeChecklistRepositoryForViewModel(
            initialTasks = listOf(
                ChecklistTask(
                    id = "task-1",
                    title = "청첩장 발송",
                    dTag = "D-7",
                    groupTitle = "마지막 점검",
                    isChecked = false,
                    targetDate = today,
                    memo = null,
                )
            )
        )
        val eventRepository = FakeScheduleEventRepositoryForViewModel(initialEvents = emptyList())
        val notificationRepository = FakeNotificationCenterRepositoryForViewModel()

        val viewModel = createViewModel(checklistRepository, eventRepository, notificationRepository)
        advanceUntilIdle()

        viewModel.onAction(ScheduleAction.OnToggleChecklistItem("task-1"))
        advanceUntilIdle()

        val item = viewModel.state.value.checklist.groups.first().items.first()
        assertEquals(true, item.isDone)
        assertTrue(notificationRepository.cancelledReminderIds.contains("checklist-reminder-task-1"))
    }

    @Test
    fun `update schedule event cancels existing reminder`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val checklistRepository = FakeChecklistRepositoryForViewModel(initialTasks = emptyList())
        val eventRepository = FakeScheduleEventRepositoryForViewModel(
            initialEvents = listOf(
                ScheduleEvent(
                    id = "event-1",
                    title = "웨딩홀 미팅",
                    eventDate = today.plus(2, DateTimeUnit.DAY),
                    startTime = "11:00",
                    location = "잠실",
                    memo = null,
                    repeatRule = EventRepeatRule.None,
                )
            )
        )
        val notificationRepository = FakeNotificationCenterRepositoryForViewModel()

        val viewModel = createViewModel(checklistRepository, eventRepository, notificationRepository)
        advanceUntilIdle()

        viewModel.onAction(
            ScheduleAction.OnUpdateScheduleEvent(
                id = "event-1",
                title = "웨딩홀 미팅(수정)",
                eventDate = today.plus(3, DateTimeUnit.DAY),
                startTime = "13:30",
                location = "송파",
                memo = "계약서 확인",
                repeatRule = EventRepeatRule.Weekly,
            )
        )
        advanceUntilIdle()

        assertTrue(notificationRepository.cancelledReminderIds.contains("event-reminder-event-1"))
    }

    @Test
    fun `selecting calendar day shows day items without replacing upcoming list`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDate = today.plus(3, DateTimeUnit.DAY)
        val checklistRepository = FakeChecklistRepositoryForViewModel(
            initialTasks = listOf(
                ChecklistTask(
                    id = "task-1",
                    title = "드레스 최종 확인",
                    dTag = "D-7",
                    groupTitle = "마지막 점검",
                    isChecked = false,
                    targetDate = targetDate,
                    memo = null,
                )
            )
        )
        val eventRepository = FakeScheduleEventRepositoryForViewModel(
            initialEvents = listOf(
                ScheduleEvent(
                    id = "event-1",
                    title = "웨딩홀 미팅",
                    eventDate = targetDate,
                    startTime = "11:00",
                    location = "잠실",
                    memo = null,
                    repeatRule = EventRepeatRule.None,
                ),
                ScheduleEvent(
                    id = "event-2",
                    title = "메이크업 상담",
                    eventDate = today.plus(4, DateTimeUnit.DAY),
                    startTime = "15:00",
                    location = "청담",
                    memo = null,
                    repeatRule = EventRepeatRule.None,
                ),
            )
        )
        val notificationRepository = FakeNotificationCenterRepositoryForViewModel()

        val viewModel = createViewModel(checklistRepository, eventRepository, notificationRepository)
        advanceUntilIdle()

        viewModel.onAction(ScheduleAction.OnSelectCalendarDay(targetDate.toString()))
        advanceUntilIdle()

        val calendar = viewModel.state.value.calendar
        assertEquals(targetDate.toString(), calendar.selectedDayId)
        assertEquals(2, calendar.selectedDayItems.size)
        assertTrue(calendar.selectedDayItems.any { it.title == "드레스 최종 확인" })
        assertTrue(calendar.selectedDayItems.any { it.title == "웨딩홀 미팅" })
        assertTrue(calendar.upcoming.any { it.title == "메이크업 상담" })
    }
}

private fun createViewModel(
    checklistRepository: FakeChecklistRepositoryForViewModel,
    eventRepository: FakeScheduleEventRepositoryForViewModel,
    notificationRepository: FakeNotificationCenterRepositoryForViewModel,
): ScheduleViewModel {
    return ScheduleViewModel(
        fetchChecklistUseCase = FetchChecklistUseCase(checklistRepository),
        addChecklistUseCase = AddChecklistUseCase(checklistRepository),
        updateChecklistUseCase = UpdateChecklistUseCase(checklistRepository),
        setChecklistCheckedUseCase = SetChecklistCheckedUseCase(checklistRepository),
        deleteChecklistUseCase = DeleteChecklistUseCase(checklistRepository),
        fetchScheduleEventsUseCase = FetchScheduleEventsUseCase(eventRepository),
        addScheduleEventUseCase = AddScheduleEventUseCase(eventRepository),
        updateScheduleEventUseCase = UpdateScheduleEventUseCase(eventRepository),
        deleteScheduleEventUseCase = DeleteScheduleEventUseCase(eventRepository),
        cancelReminderUseCase = CancelReminderUseCase(notificationRepository),
    )
}

private class FakeChecklistRepositoryForViewModel(
    initialTasks: List<ChecklistTask>,
) : ChecklistRepository {
    private val tasks = linkedMapOf<String, ChecklistTask>().apply {
        initialTasks.forEach { task -> put(task.id, task) }
    }

    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> {
        return ScheduleResult.Success(tasks.values.toList())
    }

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> {
        val id = "task-${tasks.size + 1}"
        tasks[id] = ChecklistTask(
            id = id,
            title = task.title,
            dTag = task.dTag,
            groupTitle = task.groupTitle,
            isChecked = task.isChecked,
            targetDate = task.targetDate,
            memo = task.memo,
        )
        return ScheduleResult.Success(Unit)
    }

    override suspend fun updateChecklist(id: String, task: NewChecklistTask): ScheduleResult<Unit> {
        val current = tasks[id] ?: return ScheduleResult.Error("체크리스트를 찾지 못했습니다.")
        tasks[id] = current.copy(
            title = task.title,
            dTag = task.dTag,
            groupTitle = task.groupTitle,
            isChecked = task.isChecked,
            targetDate = task.targetDate,
            memo = task.memo,
        )
        return ScheduleResult.Success(Unit)
    }

    override suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit> {
        val current = tasks[id] ?: return ScheduleResult.Error("체크리스트를 찾지 못했습니다.")
        tasks[id] = current.copy(isChecked = checked)
        return ScheduleResult.Success(Unit)
    }

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> {
        tasks.remove(id)
        return ScheduleResult.Success(Unit)
    }
}

private class FakeScheduleEventRepositoryForViewModel(
    initialEvents: List<ScheduleEvent>,
) : ScheduleEventRepository {
    private val events = linkedMapOf<String, ScheduleEvent>().apply {
        initialEvents.forEach { event -> put(event.id, event) }
    }

    override suspend fun fetchEvents(): ScheduleResult<List<ScheduleEvent>> {
        return ScheduleResult.Success(events.values.toList())
    }

    override suspend fun createEvent(event: NewScheduleEvent): ScheduleResult<Unit> {
        val id = "event-${events.size + 1}"
        events[id] = ScheduleEvent(
            id = id,
            title = event.title,
            eventDate = event.eventDate,
            startTime = event.startTime,
            location = event.location,
            memo = event.memo,
            repeatRule = event.repeatRule,
        )
        return ScheduleResult.Success(Unit)
    }

    override suspend fun updateEvent(id: String, event: NewScheduleEvent): ScheduleResult<Unit> {
        val current = events[id] ?: return ScheduleResult.Error("일정을 찾지 못했습니다.")
        events[id] = current.copy(
            title = event.title,
            eventDate = event.eventDate,
            startTime = event.startTime,
            location = event.location,
            memo = event.memo,
            repeatRule = event.repeatRule,
        )
        return ScheduleResult.Success(Unit)
    }

    override suspend fun deleteEvent(id: String): ScheduleResult<Unit> {
        events.remove(id)
        return ScheduleResult.Success(Unit)
    }
}

private class FakeNotificationCenterRepositoryForViewModel : NotificationCenterRepository {
    val cancelledReminderIds = mutableListOf<String>()

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        return NotificationCenterResult.Success(emptyList())
    }

    override suspend fun scheduleReminder(plan: NotificationReminderPlan): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun markActivityItemsRead(notificationIds: List<String>): NotificationCenterResult<Unit> {
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun cancelReminder(notificationId: String): NotificationCenterResult<Unit> {
        cancelledReminderIds += notificationId
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return NotificationCenterResult.Success(emptySet())
    }
}

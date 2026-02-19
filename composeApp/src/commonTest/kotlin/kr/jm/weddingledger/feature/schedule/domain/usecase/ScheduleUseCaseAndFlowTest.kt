package kr.jm.weddingledger.feature.schedule.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class ScheduleUseCaseAndFlowTest {

    @Test
    fun `add schedule event validates title and time`() = runBlocking {
        val repository = InMemoryScheduleEventRepository()
        val useCase = AddScheduleEventUseCase(repository)

        val blankTitle = useCase(newScheduleEvent(title = " ", startTime = "10:00"))
        val invalidTime = useCase(newScheduleEvent(startTime = "25:90"))

        assertEquals("일정 제목을 입력해주세요.", assertIs<ScheduleResult.Error>(blankTitle).message)
        assertEquals("시간 형식은 HH:mm 으로 입력해주세요.", assertIs<ScheduleResult.Error>(invalidTime).message)
        assertEquals(0, repository.createCallCount)
    }

    @Test
    fun `add schedule event trims time before repository call`() = runBlocking {
        val repository = InMemoryScheduleEventRepository()
        val useCase = AddScheduleEventUseCase(repository)

        val result = useCase(newScheduleEvent(startTime = " 09:30 "))

        assertIs<ScheduleResult.Success<Unit>>(result)
        assertEquals(1, repository.createCallCount)
        assertEquals("09:30", repository.lastCreatedEvent?.startTime)
    }

    @Test
    fun `update schedule event validates id and time`() = runBlocking {
        val repository = InMemoryScheduleEventRepository()
        val useCase = UpdateScheduleEventUseCase(repository)

        val invalidId = useCase(" ", newScheduleEvent())
        val invalidTime = useCase("event-1", newScheduleEvent(startTime = "9:00"))

        assertEquals("일정 ID가 유효하지 않습니다.", assertIs<ScheduleResult.Error>(invalidId).message)
        assertEquals("시간 형식은 HH:mm 으로 입력해주세요.", assertIs<ScheduleResult.Error>(invalidTime).message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `update checklist validates dtag range`() = runBlocking {
        val repository = InMemoryChecklistRepository()
        val useCase = UpdateChecklistUseCase(repository)

        val result = useCase(
            id = "checklist-1",
            task = newChecklistTask(dTag = "D-999"),
        )

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("D-Tag는 D-0부터 D-365 사이여야 합니다.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `delete and toggle checklist use cases validate blank id`() = runBlocking {
        val checklistRepository = InMemoryChecklistRepository()
        val deleteUseCase = DeleteChecklistUseCase(checklistRepository)
        val toggleUseCase = SetChecklistCheckedUseCase(checklistRepository)

        val deleteResult = deleteUseCase(" ")
        val toggleResult = toggleUseCase(" ", checked = true)

        assertEquals("삭제할 항목을 찾지 못했습니다.", assertIs<ScheduleResult.Error>(deleteResult).message)
        assertEquals("항목을 찾지 못했습니다.", assertIs<ScheduleResult.Error>(toggleResult).message)
        assertEquals(0, checklistRepository.deleteCallCount)
        assertEquals(0, checklistRepository.setCheckedCallCount)
    }

    @Test
    fun `delete schedule event validates blank id`() = runBlocking {
        val repository = InMemoryScheduleEventRepository()
        val useCase = DeleteScheduleEventUseCase(repository)

        val result = useCase(" ")

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("일정 ID가 유효하지 않습니다.", error.message)
        assertEquals(0, repository.deleteCallCount)
    }

    @Test
    fun `integration checklist flow add toggle delete`() = runBlocking {
        val repository = InMemoryChecklistRepository()
        val addUseCase = AddChecklistUseCase(repository)
        val fetchUseCase = FetchChecklistUseCase(repository)
        val toggleUseCase = SetChecklistCheckedUseCase(repository)
        val deleteUseCase = DeleteChecklistUseCase(repository)

        addUseCase(newChecklistTask(title = "웨딩홀 투어"))

        val createdId = assertIs<ScheduleResult.Success<List<ChecklistTask>>>(fetchUseCase())
            .data
            .first()
            .id

        toggleUseCase(createdId, checked = true)

        val toggled = assertIs<ScheduleResult.Success<List<ChecklistTask>>>(fetchUseCase())
            .data
            .first()
        assertEquals(true, toggled.isChecked)

        deleteUseCase(createdId)

        val afterDelete = assertIs<ScheduleResult.Success<List<ChecklistTask>>>(fetchUseCase()).data
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun `integration schedule event flow add update delete`() = runBlocking {
        val repository = InMemoryScheduleEventRepository()
        val addUseCase = AddScheduleEventUseCase(repository)
        val fetchUseCase = FetchScheduleEventsUseCase(repository)
        val updateUseCase = UpdateScheduleEventUseCase(repository)
        val deleteUseCase = DeleteScheduleEventUseCase(repository)

        addUseCase(newScheduleEvent(title = "스튜디오 촬영", startTime = "11:00", repeatRule = EventRepeatRule.Weekly))

        val createdId = assertIs<ScheduleResult.Success<List<ScheduleEvent>>>(fetchUseCase())
            .data
            .first()
            .id

        updateUseCase(
            id = createdId,
            event = newScheduleEvent(title = "스튜디오 촬영 수정", startTime = " 12:30 "),
        )

        val updated = assertIs<ScheduleResult.Success<List<ScheduleEvent>>>(fetchUseCase())
            .data
            .first()
        assertEquals("스튜디오 촬영 수정", updated.title)
        assertEquals("12:30", updated.startTime)

        deleteUseCase(createdId)

        val afterDelete = assertIs<ScheduleResult.Success<List<ScheduleEvent>>>(fetchUseCase()).data
        assertEquals(0, afterDelete.size)
    }
}

private class InMemoryChecklistRepository : ChecklistRepository {
    private val tasks = linkedMapOf<String, ChecklistTask>()
    private var seed = 1

    var updateCallCount: Int = 0
    var setCheckedCallCount: Int = 0
    var deleteCallCount: Int = 0

    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> {
        return ScheduleResult.Success(tasks.values.toList())
    }

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> {
        val id = "checklist-${seed++}"
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
        updateCallCount += 1
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
        setCheckedCallCount += 1
        val current = tasks[id] ?: return ScheduleResult.Error("체크리스트를 찾지 못했습니다.")
        tasks[id] = current.copy(isChecked = checked)
        return ScheduleResult.Success(Unit)
    }

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> {
        deleteCallCount += 1
        tasks.remove(id)
        return ScheduleResult.Success(Unit)
    }
}

private class InMemoryScheduleEventRepository : ScheduleEventRepository {
    private val events = linkedMapOf<String, ScheduleEvent>()
    private var seed = 1

    var createCallCount: Int = 0
    var updateCallCount: Int = 0
    var deleteCallCount: Int = 0
    var lastCreatedEvent: NewScheduleEvent? = null

    override suspend fun fetchEvents(): ScheduleResult<List<ScheduleEvent>> {
        return ScheduleResult.Success(events.values.toList())
    }

    override suspend fun createEvent(event: NewScheduleEvent): ScheduleResult<Unit> {
        createCallCount += 1
        lastCreatedEvent = event
        val id = "event-${seed++}"
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
        updateCallCount += 1
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
        deleteCallCount += 1
        events.remove(id)
        return ScheduleResult.Success(Unit)
    }
}

private fun newChecklistTask(
    title: String = "스튜디오 촬영",
    dTag: String = "D-30",
): NewChecklistTask {
    return NewChecklistTask(
        title = title,
        dTag = dTag,
        groupTitle = "본식 준비",
        isChecked = false,
        targetDate = LocalDate.parse("2026-06-01"),
        memo = "체크 필요",
    )
}

private fun newScheduleEvent(
    title: String = "본식 리허설",
    startTime: String = "10:00",
    repeatRule: EventRepeatRule = EventRepeatRule.None,
): NewScheduleEvent {
    return NewScheduleEvent(
        title = title,
        eventDate = LocalDate.parse("2026-06-15"),
        startTime = startTime,
        location = "웨딩홀",
        memo = "메모",
        repeatRule = repeatRule,
    )
}

package kr.jm.weddingledger.feature.schedule.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class AddChecklistUseCaseTest {

    @Test
    fun `returns error when dtag is invalid`() = runBlocking {
        val repository = FakeChecklistRepositoryForAdd()
        val useCase = AddChecklistUseCase(repository)

        val result = useCase(sampleTask(dTag = "INVALID"))

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("D-Tag 형식이 올바르지 않습니다. (예: D-30)", error.message)
        assertEquals(0, repository.createCallCount)
    }

    @Test
    fun `returns error when memo exceeds max length`() = runBlocking {
        val repository = FakeChecklistRepositoryForAdd()
        val useCase = AddChecklistUseCase(repository)

        val result = useCase(sampleTask(memo = "a".repeat(121)))

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("메모는 120자 이하로 입력해주세요.", error.message)
        assertEquals(0, repository.createCallCount)
    }

    @Test
    fun `delegates create when task is valid`() = runBlocking {
        val repository = FakeChecklistRepositoryForAdd()
        val useCase = AddChecklistUseCase(repository)
        val task = sampleTask()

        val result = useCase(task)

        assertIs<ScheduleResult.Success<Unit>>(result)
        assertEquals(1, repository.createCallCount)
        assertEquals(task, repository.lastCreatedTask)
    }
}

private class FakeChecklistRepositoryForAdd : ChecklistRepository {
    var createCallCount: Int = 0
    var lastCreatedTask: NewChecklistTask? = null

    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> =
        ScheduleResult.Success(emptyList())

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> {
        createCallCount += 1
        lastCreatedTask = task
        return ScheduleResult.Success(Unit)
    }

    override suspend fun updateChecklist(id: String, task: NewChecklistTask): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)

    override suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)
}

private fun sampleTask(
    dTag: String = "D-30",
    memo: String = "메모",
): NewChecklistTask = NewChecklistTask(
    title = "스튜디오 예약",
    dTag = dTag,
    groupTitle = "본식 준비",
    isChecked = false,
    targetDate = LocalDate.parse("2026-06-01"),
    memo = memo,
)

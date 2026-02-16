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

class UpdateChecklistUseCaseTest {

    @Test
    fun `returns error when id is blank`() = runBlocking {
        val repository = FakeChecklistRepositoryForUpdate()
        val useCase = UpdateChecklistUseCase(repository)

        val result = useCase(id = " ", task = sampleTask(title = "드레스 피팅"))

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("수정할 항목을 찾지 못했습니다.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `returns error when title is blank`() = runBlocking {
        val repository = FakeChecklistRepositoryForUpdate()
        val useCase = UpdateChecklistUseCase(repository)

        val result = useCase(id = "task-1", task = sampleTask(title = " "))

        val error = assertIs<ScheduleResult.Error>(result)
        assertEquals("할 일 제목을 입력해주세요.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `delegates update when data is valid`() = runBlocking {
        val repository = FakeChecklistRepositoryForUpdate()
        val useCase = UpdateChecklistUseCase(repository)
        val expected = sampleTask(title = "예식장 계약서 확인")

        val result = useCase(id = "task-1", task = expected)

        assertIs<ScheduleResult.Success<Unit>>(result)
        assertEquals(1, repository.updateCallCount)
        assertEquals("task-1", repository.lastId)
        assertEquals(expected, repository.lastTask)
    }
}

private class FakeChecklistRepositoryForUpdate : ChecklistRepository {
    var updateCallCount: Int = 0
    var lastId: String? = null
    var lastTask: NewChecklistTask? = null

    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> =
        ScheduleResult.Success(emptyList())

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)

    override suspend fun updateChecklist(id: String, task: NewChecklistTask): ScheduleResult<Unit> {
        updateCallCount += 1
        lastId = id
        lastTask = task
        return ScheduleResult.Success(Unit)
    }

    override suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> =
        ScheduleResult.Success(Unit)
}

private fun sampleTask(title: String): NewChecklistTask = NewChecklistTask(
    title = title,
    dTag = "D-30",
    groupTitle = "본식 준비",
    isChecked = false,
    targetDate = LocalDate.parse("2026-06-01"),
    memo = "메모",
)

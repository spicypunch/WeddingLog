package kr.jm.weddingledger.feature.money.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPresetPreference
import kr.jm.weddingledger.feature.money.domain.repository.MoneyFilterPreferenceRepository

class ExpenseFilterPreferenceUseCaseTest {

    @Test
    fun `fetch use case returns repository state`() = runBlocking {
        val expected = sampleState()
        val repository = FakeMoneyFilterPreferenceRepository(initial = expected)
        val useCase = FetchExpenseFilterPreferenceUseCase(repository)

        val result = useCase()

        assertEquals(expected, result)
        assertEquals(1, repository.loadCallCount)
    }

    @Test
    fun `save use case delegates state to repository`() = runBlocking {
        val repository = FakeMoneyFilterPreferenceRepository(initial = null)
        val useCase = SaveExpenseFilterPreferenceUseCase(repository)
        val state = sampleState()

        useCase(state)

        assertEquals(1, repository.saveCallCount)
        assertEquals(state, repository.lastSavedState)
    }
}

private class FakeMoneyFilterPreferenceRepository(
    initial: ExpenseFilterPreferenceState?,
) : MoneyFilterPreferenceRepository {
    var state: ExpenseFilterPreferenceState? = initial
    var loadCallCount: Int = 0
    var saveCallCount: Int = 0
    var lastSavedState: ExpenseFilterPreferenceState? = null

    override suspend fun loadExpenseFilterState(): ExpenseFilterPreferenceState? {
        loadCallCount += 1
        return state
    }

    override suspend fun saveExpenseFilterState(state: ExpenseFilterPreferenceState) {
        saveCallCount += 1
        lastSavedState = state
        this.state = state
    }
}

private fun sampleState(): ExpenseFilterPreferenceState {
    return ExpenseFilterPreferenceState(
        selectedCategoryId = "wedding_hall",
        selectedPeriodFilterName = "Last3Months",
        selectedStatusFilterName = "HasMemo",
        selectedPresetId = "preset-2",
        presetSeed = 3,
        overBudgetOnly = true,
        presets = listOf(
            ExpenseFilterPresetPreference(
                id = "preset-1",
                label = "웨딩홀 · 이번 달 · 전체",
                categoryId = "wedding_hall",
                periodFilterName = "ThisMonth",
                statusFilterName = "All",
            ),
            ExpenseFilterPresetPreference(
                id = "preset-2",
                label = "전체 · 최근 3개월 · 메모 있음",
                categoryId = null,
                periodFilterName = "Last3Months",
                statusFilterName = "HasMemo",
            ),
        ),
    )
}

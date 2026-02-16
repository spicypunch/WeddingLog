package kr.jm.weddingledger.feature.money.data.repository

import kotlinx.coroutines.flow.first
import kr.jm.weddingledger.feature.money.data.local.ExpenseFilterPreferenceLocalState
import kr.jm.weddingledger.feature.money.data.local.ExpenseFilterPreferenceStorage
import kr.jm.weddingledger.feature.money.data.local.ExpenseFilterPresetLocalState
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPresetPreference
import kr.jm.weddingledger.feature.money.domain.repository.MoneyFilterPreferenceRepository

class MoneyFilterPreferenceRepositoryImpl : MoneyFilterPreferenceRepository {

    override suspend fun loadExpenseFilterState(): ExpenseFilterPreferenceState? {
        return runCatching { ExpenseFilterPreferenceStorage.state.first() }
            .getOrNull()
            ?.toDomain()
    }

    override suspend fun saveExpenseFilterState(state: ExpenseFilterPreferenceState) {
        runCatching {
            ExpenseFilterPreferenceStorage.save(state.toLocal())
        }
    }
}

private fun ExpenseFilterPreferenceLocalState.toDomain(): ExpenseFilterPreferenceState {
    return ExpenseFilterPreferenceState(
        selectedCategoryId = selectedCategoryId,
        selectedPeriodFilterName = selectedPeriodFilterName,
        selectedStatusFilterName = selectedStatusFilterName,
        selectedPresetId = selectedPresetId,
        presetSeed = presetSeed.coerceAtLeast(1),
        overBudgetOnly = overBudgetOnly,
        presets = presets.map { preset ->
            ExpenseFilterPresetPreference(
                id = preset.id,
                label = preset.label,
                categoryId = preset.categoryId,
                periodFilterName = preset.periodFilterName,
                statusFilterName = preset.statusFilterName,
            )
        },
    )
}

private fun ExpenseFilterPreferenceState.toLocal(): ExpenseFilterPreferenceLocalState {
    return ExpenseFilterPreferenceLocalState(
        selectedCategoryId = selectedCategoryId,
        selectedPeriodFilterName = selectedPeriodFilterName,
        selectedStatusFilterName = selectedStatusFilterName,
        selectedPresetId = selectedPresetId,
        presetSeed = presetSeed.coerceAtLeast(1),
        overBudgetOnly = overBudgetOnly,
        presets = presets.map { preset ->
            ExpenseFilterPresetLocalState(
                id = preset.id,
                label = preset.label,
                categoryId = preset.categoryId,
                periodFilterName = preset.periodFilterName,
                statusFilterName = preset.statusFilterName,
            )
        },
    )
}

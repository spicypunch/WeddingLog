package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState
import kr.jm.weddingledger.feature.money.domain.repository.MoneyFilterPreferenceRepository

class SaveExpenseFilterPreferenceUseCase(
    private val repository: MoneyFilterPreferenceRepository,
) {
    suspend operator fun invoke(state: ExpenseFilterPreferenceState) {
        repository.saveExpenseFilterState(state)
    }
}

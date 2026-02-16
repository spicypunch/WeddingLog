package kr.jm.weddingledger.feature.money.domain.repository

import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState

interface MoneyFilterPreferenceRepository {
    suspend fun loadExpenseFilterState(): ExpenseFilterPreferenceState?
    suspend fun saveExpenseFilterState(state: ExpenseFilterPreferenceState)
}

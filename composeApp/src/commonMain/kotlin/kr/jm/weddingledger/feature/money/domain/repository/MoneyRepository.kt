package kr.jm.weddingledger.feature.money.domain.repository

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense

interface MoneyRepository {
    suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot>
    suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit>
    suspend fun addExpense(expense: NewExpense): MoneyResult<Unit>
}


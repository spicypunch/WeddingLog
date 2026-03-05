package kr.jm.weddingledger.feature.money.domain.repository

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense

interface MoneyRepository {
    suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot>
    suspend fun addCategory(name: String): MoneyResult<Unit>
    suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit>
    suspend fun addExpense(expense: NewExpense): MoneyResult<Unit>

    suspend fun updateCategory(id: String, name: String): MoneyResult<Unit>
    suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit>
    suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit>

    suspend fun deleteBudgetItem(id: String): MoneyResult<Unit>
    suspend fun deleteExpense(id: String): MoneyResult<Unit>
}

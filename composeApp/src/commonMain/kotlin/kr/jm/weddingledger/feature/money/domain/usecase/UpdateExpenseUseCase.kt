package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class UpdateExpenseUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(id: String, expense: NewExpense): MoneyResult<Unit> =
        repository.updateExpense(id, expense)
}


package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class UpdateBudgetItemUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(id: String, item: NewBudgetItem): MoneyResult<Unit> =
        repository.updateBudgetItem(id, item)
}


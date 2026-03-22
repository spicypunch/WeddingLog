package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class DeleteCategoryUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(id: String): MoneyResult<Unit> =
        repository.deleteCategory(id)
}

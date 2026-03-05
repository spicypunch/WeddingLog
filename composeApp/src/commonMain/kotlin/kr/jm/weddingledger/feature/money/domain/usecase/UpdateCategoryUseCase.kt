package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class UpdateCategoryUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(id: String, name: String): MoneyResult<Unit> =
        repository.updateCategory(id, name)
}

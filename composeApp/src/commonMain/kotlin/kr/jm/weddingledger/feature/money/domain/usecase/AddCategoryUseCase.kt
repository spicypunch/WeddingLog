package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class AddCategoryUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(name: String): MoneyResult<Unit> = repository.addCategory(name)
}

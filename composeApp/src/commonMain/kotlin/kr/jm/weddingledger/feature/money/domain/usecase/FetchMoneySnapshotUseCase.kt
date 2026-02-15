package kr.jm.weddingledger.feature.money.domain.usecase

import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class FetchMoneySnapshotUseCase(
    private val repository: MoneyRepository,
) {
    suspend operator fun invoke(): MoneyResult<MoneySnapshot> = repository.fetchSnapshot()
}


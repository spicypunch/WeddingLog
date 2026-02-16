package kr.jm.weddingledger.feature.tools.domain.usecase

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class FetchVenuesUseCase(
    private val repository: ToolsRepository,
) {
    suspend operator fun invoke(): ToolsResult<List<Venue>> = repository.fetchVenues()
}


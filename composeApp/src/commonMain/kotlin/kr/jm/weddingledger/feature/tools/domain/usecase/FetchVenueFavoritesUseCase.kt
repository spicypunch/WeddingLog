package kr.jm.weddingledger.feature.tools.domain.usecase

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class FetchVenueFavoritesUseCase(
    private val repository: ToolsRepository,
) {
    suspend operator fun invoke(): ToolsResult<Set<String>> = repository.fetchFavoriteVenueIds()
}


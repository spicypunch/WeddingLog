package kr.jm.weddingledger.feature.tools.domain.usecase

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class SetVenueFavoriteUseCase(
    private val repository: ToolsRepository,
) {
    suspend operator fun invoke(venueId: String, favorite: Boolean): ToolsResult<Unit> {
        if (venueId.isBlank()) return ToolsResult.Error("웨딩홀 ID가 유효하지 않습니다.")
        return repository.setVenueFavorite(venueId, favorite)
    }
}


package kr.jm.weddingledger.feature.tools.domain.repository

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.model.Venue

interface ToolsRepository {
    suspend fun fetchVenues(): ToolsResult<List<Venue>>
    suspend fun fetchFavoriteVenueIds(): ToolsResult<Set<String>>
    suspend fun setVenueFavorite(venueId: String, favorite: Boolean): ToolsResult<Unit>

    suspend fun fetchSurveyAnswers(): ToolsResult<List<ValuesSurveyAnswer>>
    suspend fun saveSurveyAnswers(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit>
}


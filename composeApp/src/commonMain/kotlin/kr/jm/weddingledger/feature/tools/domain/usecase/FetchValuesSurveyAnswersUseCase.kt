package kr.jm.weddingledger.feature.tools.domain.usecase

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class FetchValuesSurveyAnswersUseCase(
    private val repository: ToolsRepository,
) {
    suspend operator fun invoke(): ToolsResult<List<ValuesSurveyAnswer>> = repository.fetchSurveyAnswers()
}


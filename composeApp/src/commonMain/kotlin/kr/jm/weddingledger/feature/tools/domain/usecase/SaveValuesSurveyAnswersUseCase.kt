package kr.jm.weddingledger.feature.tools.domain.usecase

import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class SaveValuesSurveyAnswersUseCase(
    private val repository: ToolsRepository,
) {
    suspend operator fun invoke(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit> {
        return repository.saveSurveyAnswers(answers)
    }
}


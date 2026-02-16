package kr.jm.weddingledger.feature.tools.presentation

data class ValuesSurveyOptionUi(
    val id: String,
    val label: String,
    val score: Int,
)

data class ValuesSurveyUiState(
    val isLoading: Boolean,
    val message: String?,
    val progressText: String,
    val progress: Float,
    val currentIndex: Int,
    val totalCount: Int,
    val questionTitle: String,
    val questionDescription: String,
    val options: List<ValuesSurveyOptionUi>,
    val selectedOptionId: String?,
    val canGoPrev: Boolean,
    val nextButtonText: String,
    val isCompleted: Boolean,
    val resultTitle: String,
    val resultSummary: String,
    val resultScoreText: String,
) {
    companion object {
        fun initial(): ValuesSurveyUiState = ValuesSurveyUiState(
            isLoading = true,
            message = null,
            progressText = "0 / 0",
            progress = 0f,
            currentIndex = 0,
            totalCount = 0,
            questionTitle = "",
            questionDescription = "",
            options = emptyList(),
            selectedOptionId = null,
            canGoPrev = false,
            nextButtonText = "다음",
            isCompleted = false,
            resultTitle = "",
            resultSummary = "",
            resultScoreText = "",
        )
    }
}

sealed interface ValuesSurveyAction {
    data class OnSelectOption(val optionId: String) : ValuesSurveyAction
    data object OnClickPrev : ValuesSurveyAction
    data object OnClickNext : ValuesSurveyAction
    data object OnClickRestart : ValuesSurveyAction
}

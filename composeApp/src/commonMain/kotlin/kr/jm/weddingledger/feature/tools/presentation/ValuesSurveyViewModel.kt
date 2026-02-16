package kr.jm.weddingledger.feature.tools.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchValuesSurveyAnswersUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SaveValuesSurveyAnswersUseCase
import kotlin.math.roundToInt

class ValuesSurveyViewModel(
    private val fetchValuesSurveyAnswersUseCase: FetchValuesSurveyAnswersUseCase,
    private val saveValuesSurveyAnswersUseCase: SaveValuesSurveyAnswersUseCase,
) : ViewModel() {

    private val questions = listOf(
        SurveyQuestion(
            id = "q-budget",
            title = "결혼 예산 사용 기준",
            description = "필요하면 계획보다 더 쓸 수 있다고 보나요?",
        ),
        SurveyQuestion(
            id = "q-home",
            title = "신혼집 우선순위",
            description = "입지/크기/대출 중 무엇에 더 비중을 두나요?",
        ),
        SurveyQuestion(
            id = "q-family",
            title = "가족 행사 참여도",
            description = "양가 행사에 참여하는 빈도를 어떻게 생각하나요?",
        ),
        SurveyQuestion(
            id = "q-leisure",
            title = "주말 시간 사용",
            description = "주말은 휴식 중심인가요, 활동 중심인가요?",
        ),
        SurveyQuestion(
            id = "q-saving",
            title = "저축/소비 성향",
            description = "수입의 어느 정도를 저축하고 싶은가요?",
        ),
    )

    private val options = listOf(
        ValuesSurveyOptionUi(id = "opt-1", label = "매우 신중", score = 1),
        ValuesSurveyOptionUi(id = "opt-2", label = "신중한 편", score = 2),
        ValuesSurveyOptionUi(id = "opt-3", label = "중간", score = 3),
        ValuesSurveyOptionUi(id = "opt-4", label = "유연한 편", score = 4),
        ValuesSurveyOptionUi(id = "opt-5", label = "매우 유연", score = 5),
    )

    private var currentIndex = 0
    private var completed = false
    private val selectedByQuestionId = mutableMapOf<String, String>()

    private val _state = MutableStateFlow(ValuesSurveyUiState.initial())
    val state: StateFlow<ValuesSurveyUiState> = _state.asStateFlow()

    init {
        loadSavedAnswers()
    }

    fun onAction(action: ValuesSurveyAction) {
        when (action) {
            is ValuesSurveyAction.OnSelectOption -> {
                if (completed) return
                val questionId = questions[currentIndex].id
                selectedByQuestionId[questionId] = action.optionId
                applyState()
            }

            ValuesSurveyAction.OnClickPrev -> {
                if (completed) return
                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                applyState()
            }

            ValuesSurveyAction.OnClickNext -> {
                if (completed) return
                if (currentIndex < questions.lastIndex) {
                    currentIndex += 1
                    applyState()
                } else {
                    completed = true
                    applyState()
                    persistAnswers()
                }
            }

            ValuesSurveyAction.OnClickRestart -> {
                currentIndex = 0
                completed = false
                selectedByQuestionId.clear()
                applyState()
                persistAnswers()
            }
        }
    }

    private fun loadSavedAnswers() {
        applyState(isLoading = true)
        viewModelScope.launch {
            when (val result = fetchValuesSurveyAnswersUseCase()) {
                is ToolsResult.Success -> {
                    selectedByQuestionId.clear()
                    result.data.forEach { answer ->
                        if (questions.any { it.id == answer.questionId } && options.any { it.id == answer.optionId }) {
                            selectedByQuestionId[answer.questionId] = answer.optionId
                        }
                    }
                    val allAnswered = questions.isNotEmpty() && questions.all { selectedByQuestionId.containsKey(it.id) }
                    completed = allAnswered
                    currentIndex = if (allAnswered) {
                        questions.lastIndex.coerceAtLeast(0)
                    } else {
                        firstUnansweredIndex()
                    }
                    applyState(isLoading = false)
                }

                is ToolsResult.Error -> {
                    applyState(
                        isLoading = false,
                        message = result.message,
                    )
                }
            }
        }
    }

    private fun persistAnswers() {
        val answers = questions.mapNotNull { question ->
            val optionId = selectedByQuestionId[question.id] ?: return@mapNotNull null
            val score = options.firstOrNull { it.id == optionId }?.score ?: return@mapNotNull null
            ValuesSurveyAnswer(
                questionId = question.id,
                optionId = optionId,
                score = score,
            )
        }

        viewModelScope.launch {
            when (val result = saveValuesSurveyAnswersUseCase(answers)) {
                is ToolsResult.Success -> Unit
                is ToolsResult.Error -> {
                    _state.update { it.copy(message = result.message) }
                }
            }
        }
    }

    private fun firstUnansweredIndex(): Int {
        val index = questions.indexOfFirst { question -> !selectedByQuestionId.containsKey(question.id) }
        return if (index >= 0) index else 0
    }

    private fun applyState(
        isLoading: Boolean = false,
        message: String? = null,
    ) {
        if (completed) {
            val scores = questions.map { question ->
                val selectedId = selectedByQuestionId[question.id]
                options.firstOrNull { it.id == selectedId }?.score ?: 3
            }
            val average = scores.average()
            val (title, summary) = when {
                average < 2.4 -> "신중 조율형" to "두 분 모두 안정적인 결정을 선호해요. 일정/예산표를 미리 합의하면 충돌을 줄일 수 있어요."
                average < 3.6 -> "균형 조율형" to "상황에 맞춰 유연하게 조정하는 타입이에요. 기준선(예산/시간)을 먼저 두면 더 탄탄해져요."
                else -> "추진 조율형" to "실행력이 높은 타입이에요. 빠른 결정 장점을 살리되 중요한 항목은 한 번 더 검토해보세요."
            }

            _state.update {
                it.copy(
                    isLoading = isLoading,
                    message = message,
                    progressText = "${questions.size} / ${questions.size}",
                    progress = 1f,
                    currentIndex = questions.lastIndex.coerceAtLeast(0),
                    totalCount = questions.size,
                    questionTitle = "",
                    questionDescription = "",
                    options = options,
                    selectedOptionId = null,
                    canGoPrev = false,
                    nextButtonText = "",
                    isCompleted = true,
                    resultTitle = title,
                    resultSummary = summary,
                    resultScoreText = "평균 ${formatOneDecimal(average)}점",
                )
            }
            return
        }

        val safeIndex = currentIndex.coerceIn(0, questions.lastIndex.coerceAtLeast(0))
        currentIndex = safeIndex
        val question = questions.getOrNull(safeIndex)
        val selected = question?.let { selectedByQuestionId[it.id] }
        val solvedCount = selectedByQuestionId.size
        val isLast = safeIndex == questions.lastIndex

        _state.update {
            it.copy(
                isLoading = isLoading,
                message = message,
                progressText = "${solvedCount} / ${questions.size}",
                progress = if (questions.isEmpty()) 0f else solvedCount.toFloat() / questions.size.toFloat(),
                currentIndex = safeIndex,
                totalCount = questions.size,
                questionTitle = question?.title.orEmpty(),
                questionDescription = question?.description.orEmpty(),
                options = options,
                selectedOptionId = selected,
                canGoPrev = safeIndex > 0,
                nextButtonText = if (isLast) "결과 보기" else "다음",
                isCompleted = false,
                resultTitle = "",
                resultSummary = "",
                resultScoreText = "",
            )
        }
    }
}

private data class SurveyQuestion(
    val id: String,
    val title: String,
    val description: String,
)

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    val integerPart = rounded.toInt()
    return if (rounded == integerPart.toDouble()) "$integerPart.0" else rounded.toString()
}

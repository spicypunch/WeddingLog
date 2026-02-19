package kr.jm.weddingledger.feature.tools.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchValuesSurveyAnswersUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SaveValuesSurveyAnswersUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class ValuesSurveyViewModelTest {

    @Test
    fun `load saved complete answers enters completed state`() = runMainDispatcherTest {
        val repository = FakeToolsRepositoryForSurvey(
            initialAnswers = listOf(
                ValuesSurveyAnswer("q-budget", "opt-1", 1),
                ValuesSurveyAnswer("q-home", "opt-2", 2),
                ValuesSurveyAnswer("q-family", "opt-3", 3),
                ValuesSurveyAnswer("q-leisure", "opt-4", 4),
                ValuesSurveyAnswer("q-saving", "opt-5", 5),
            )
        )

        val viewModel = ValuesSurveyViewModel(
            fetchValuesSurveyAnswersUseCase = FetchValuesSurveyAnswersUseCase(repository),
            saveValuesSurveyAnswersUseCase = SaveValuesSurveyAnswersUseCase(repository),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(true, state.isCompleted)
        assertTrue(state.resultTitle.isNotBlank())
        assertTrue(state.resultScoreText.contains("평균"))
    }

    @Test
    fun `completing survey saves all answers`() = runMainDispatcherTest {
        val repository = FakeToolsRepositoryForSurvey(initialAnswers = emptyList())

        val viewModel = ValuesSurveyViewModel(
            fetchValuesSurveyAnswersUseCase = FetchValuesSurveyAnswersUseCase(repository),
            saveValuesSurveyAnswersUseCase = SaveValuesSurveyAnswersUseCase(repository),
        )
        advanceUntilIdle()

        repeat(5) {
            viewModel.onAction(ValuesSurveyAction.OnSelectOption("opt-3"))
            viewModel.onAction(ValuesSurveyAction.OnClickNext)
            advanceUntilIdle()
        }

        val state = viewModel.state.value
        assertEquals(true, state.isCompleted)
        assertEquals(1, repository.saveCallCount)
        assertEquals(5, repository.savedAnswers.last().size)
    }
}

private class FakeToolsRepositoryForSurvey(
    initialAnswers: List<ValuesSurveyAnswer>,
) : ToolsRepository {
    private var answers: List<ValuesSurveyAnswer> = initialAnswers
    val savedAnswers = mutableListOf<List<ValuesSurveyAnswer>>()
    var saveCallCount: Int = 0

    override suspend fun fetchVenues(): ToolsResult<List<Venue>> = ToolsResult.Success(emptyList())

    override suspend fun fetchFavoriteVenueIds(): ToolsResult<Set<String>> = ToolsResult.Success(emptySet())

    override suspend fun setVenueFavorite(venueId: String, favorite: Boolean): ToolsResult<Unit> = ToolsResult.Success(Unit)

    override suspend fun fetchSurveyAnswers(): ToolsResult<List<ValuesSurveyAnswer>> {
        return ToolsResult.Success(answers)
    }

    override suspend fun saveSurveyAnswers(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit> {
        saveCallCount += 1
        this.answers = answers
        savedAnswers += answers
        return ToolsResult.Success(Unit)
    }
}

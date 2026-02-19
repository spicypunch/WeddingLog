package kr.jm.weddingledger.feature.tools.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class ToolsUseCaseAndFlowTest {

    @Test
    fun `set venue favorite validates blank id`() = runBlocking {
        val repository = InMemoryToolsRepository()
        val useCase = SetVenueFavoriteUseCase(repository)

        val result = useCase(" ", favorite = true)

        val error = assertIs<ToolsResult.Error>(result)
        assertEquals("웨딩홀 ID가 유효하지 않습니다.", error.message)
        assertEquals(0, repository.setFavoriteCallCount)
    }

    @Test
    fun `fetch usecases return repository data`() = runBlocking {
        val repository = InMemoryToolsRepository()

        val venues = FetchVenuesUseCase(repository)()
        val favorites = FetchVenueFavoritesUseCase(repository)()

        assertEquals(2, assertIs<ToolsResult.Success<List<Venue>>>(venues).data.size)
        assertEquals(setOf("venue-1"), assertIs<ToolsResult.Success<Set<String>>>(favorites).data)
    }

    @Test
    fun `save and fetch survey answers round trip`() = runBlocking {
        val repository = InMemoryToolsRepository()
        val saveUseCase = SaveValuesSurveyAnswersUseCase(repository)
        val fetchUseCase = FetchValuesSurveyAnswersUseCase(repository)

        val answers = listOf(
            ValuesSurveyAnswer(questionId = "q-budget", optionId = "opt-3", score = 3),
            ValuesSurveyAnswer(questionId = "q-home", optionId = "opt-4", score = 4),
        )

        saveUseCase(answers)
        val fetched = fetchUseCase()

        assertEquals(answers, assertIs<ToolsResult.Success<List<ValuesSurveyAnswer>>>(fetched).data)
        assertEquals(1, repository.saveSurveyCallCount)
    }

    @Test
    fun `integration flow toggles favorite and persists survey answers`() = runBlocking {
        val repository = InMemoryToolsRepository()
        val setFavoriteUseCase = SetVenueFavoriteUseCase(repository)
        val fetchFavoritesUseCase = FetchVenueFavoritesUseCase(repository)
        val saveSurveyUseCase = SaveValuesSurveyAnswersUseCase(repository)
        val fetchSurveyUseCase = FetchValuesSurveyAnswersUseCase(repository)

        setFavoriteUseCase("venue-2", favorite = true)
        setFavoriteUseCase("venue-1", favorite = false)

        val favorites = assertIs<ToolsResult.Success<Set<String>>>(fetchFavoritesUseCase()).data
        assertEquals(setOf("venue-2"), favorites)

        val answers = listOf(
            ValuesSurveyAnswer(questionId = "q-family", optionId = "opt-2", score = 2),
            ValuesSurveyAnswer(questionId = "q-saving", optionId = "opt-5", score = 5),
        )
        saveSurveyUseCase(answers)

        val restoredAnswers = assertIs<ToolsResult.Success<List<ValuesSurveyAnswer>>>(fetchSurveyUseCase()).data
        assertEquals(answers, restoredAnswers)
    }
}

private class InMemoryToolsRepository : ToolsRepository {
    private val venues = listOf(
        Venue(
            id = "venue-1",
            name = "더채플",
            location = "강남",
            pricePerGuest = 95_000,
            rating = 4.7,
            capacity = 220,
            tags = listOf("호텔", "교통편리"),
            accentHex = 0xFFEF3948,
        ),
        Venue(
            id = "venue-2",
            name = "루나홀",
            location = "송파",
            pricePerGuest = 82_000,
            rating = 4.5,
            capacity = 180,
            tags = listOf("가성비", "주차"),
            accentHex = 0xFF3B82F6,
        ),
    )

    private val favorites = linkedSetOf("venue-1")
    private var answers: List<ValuesSurveyAnswer> = emptyList()

    var setFavoriteCallCount: Int = 0
    var saveSurveyCallCount: Int = 0

    override suspend fun fetchVenues(): ToolsResult<List<Venue>> = ToolsResult.Success(venues)

    override suspend fun fetchFavoriteVenueIds(): ToolsResult<Set<String>> = ToolsResult.Success(favorites.toSet())

    override suspend fun setVenueFavorite(venueId: String, favorite: Boolean): ToolsResult<Unit> {
        setFavoriteCallCount += 1
        if (favorite) favorites.add(venueId) else favorites.remove(venueId)
        return ToolsResult.Success(Unit)
    }

    override suspend fun fetchSurveyAnswers(): ToolsResult<List<ValuesSurveyAnswer>> = ToolsResult.Success(answers)

    override suspend fun saveSurveyAnswers(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit> {
        saveSurveyCallCount += 1
        this.answers = answers
        return ToolsResult.Success(Unit)
    }
}

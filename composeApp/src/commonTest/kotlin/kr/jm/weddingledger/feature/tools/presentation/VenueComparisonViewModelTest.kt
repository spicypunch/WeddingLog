package kr.jm.weddingledger.feature.tools.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenueFavoritesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenuesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SetVenueFavoriteUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class VenueComparisonViewModelTest {

    @Test
    fun `recommended sort prioritizes favorite venue`() = runMainDispatcherTest {
        val repository = FakeToolsRepositoryForVenue(
            venues = listOf(
                venue(id = "v1", name = "A홀", rating = 4.6, price = 90_000),
                venue(id = "v2", name = "B홀", rating = 4.6, price = 90_000),
            ),
            favoriteIds = mutableSetOf("v2"),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val firstId = viewModel.state.value.items.first().id
        assertEquals("v2", firstId)
    }

    @Test
    fun `toggle favorite rolls back when persistence fails`() = runMainDispatcherTest {
        val repository = FakeToolsRepositoryForVenue(
            venues = listOf(venue(id = "v1", name = "A홀", rating = 4.5, price = 80_000)),
            favoriteIds = mutableSetOf(),
        ).apply {
            setFavoriteResults += ToolsResult.Error("즐겨찾기 저장 실패")
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(VenueComparisonAction.OnToggleFavorite("v1"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.items.first().isFavorite)
        assertEquals("즐겨찾기 저장 실패", state.message)
    }

    @Test
    fun `query and clear filter update state`() = runMainDispatcherTest {
        val repository = FakeToolsRepositoryForVenue(
            venues = listOf(
                venue(id = "v1", name = "강남 웨딩홀", rating = 4.5, price = 90_000),
                venue(id = "v2", name = "송파 웨딩홀", rating = 4.4, price = 85_000),
            ),
            favoriteIds = mutableSetOf(),
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(VenueComparisonAction.OnQueryChange("강남"))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals("v1", state.items.first().id)

        viewModel.onAction(VenueComparisonAction.OnClearFilter)
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals("", state.query)
        assertEquals(VenueSortType.Recommended, state.sortType)
        assertTrue(state.items.size >= 2)
    }
}

private fun createViewModel(repository: FakeToolsRepositoryForVenue): VenueComparisonViewModel {
    return VenueComparisonViewModel(
        fetchVenuesUseCase = FetchVenuesUseCase(repository),
        fetchVenueFavoritesUseCase = FetchVenueFavoritesUseCase(repository),
        setVenueFavoriteUseCase = SetVenueFavoriteUseCase(repository),
    )
}

private class FakeToolsRepositoryForVenue(
    private val venues: List<Venue>,
    private val favoriteIds: MutableSet<String>,
) : ToolsRepository {
    val setFavoriteResults = mutableListOf<ToolsResult<Unit>>()

    override suspend fun fetchVenues(): ToolsResult<List<Venue>> = ToolsResult.Success(venues)

    override suspend fun fetchFavoriteVenueIds(): ToolsResult<Set<String>> = ToolsResult.Success(favoriteIds)

    override suspend fun setVenueFavorite(venueId: String, favorite: Boolean): ToolsResult<Unit> {
        val result = setFavoriteResults.removeFirstOrNull() ?: ToolsResult.Success(Unit)
        return when (result) {
            is ToolsResult.Success -> {
                if (favorite) favoriteIds += venueId else favoriteIds -= venueId
                ToolsResult.Success(Unit)
            }
            is ToolsResult.Error -> result
        }
    }

    override suspend fun fetchSurveyAnswers(): ToolsResult<List<ValuesSurveyAnswer>> = ToolsResult.Success(emptyList())

    override suspend fun saveSurveyAnswers(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit> = ToolsResult.Success(Unit)
}

private fun venue(
    id: String,
    name: String,
    rating: Double,
    price: Int,
): Venue {
    return Venue(
        id = id,
        name = name,
        location = "서울",
        pricePerGuest = price,
        rating = rating,
        capacity = 200,
        tags = listOf("예식장"),
        accentHex = 0xFFEF3948,
    )
}

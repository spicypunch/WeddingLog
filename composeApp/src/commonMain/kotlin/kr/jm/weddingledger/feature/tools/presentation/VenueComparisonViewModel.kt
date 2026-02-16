package kr.jm.weddingledger.feature.tools.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenueFavoritesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.FetchVenuesUseCase
import kr.jm.weddingledger.feature.tools.domain.usecase.SetVenueFavoriteUseCase
import kotlin.math.roundToInt

class VenueComparisonViewModel(
    private val fetchVenuesUseCase: FetchVenuesUseCase,
    private val fetchVenueFavoritesUseCase: FetchVenueFavoritesUseCase,
    private val setVenueFavoriteUseCase: SetVenueFavoriteUseCase,
) : ViewModel() {

    private var venues: List<Venue> = emptyList()
    private var favorites = linkedSetOf<String>()
    private var query: String = ""
    private var sortType: VenueSortType = VenueSortType.Recommended

    private val _state = MutableStateFlow(VenueComparisonUiState.initial())
    val state: StateFlow<VenueComparisonUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: VenueComparisonAction) {
        when (action) {
            is VenueComparisonAction.OnQueryChange -> {
                query = action.value
                applyState()
            }

            is VenueComparisonAction.OnSelectSort -> {
                sortType = action.value
                applyState()
            }

            is VenueComparisonAction.OnToggleFavorite -> {
                val favorite = !favorites.contains(action.id)
                if (favorite) favorites.add(action.id) else favorites.remove(action.id)
                applyState()
                persistFavorite(id = action.id, favorite = favorite)
            }

            VenueComparisonAction.OnShareTopResults -> Unit // handled in Route
            VenueComparisonAction.OnClearFilter -> {
                query = ""
                sortType = VenueSortType.Recommended
                applyState()
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            var message: String? = null

            when (val result = fetchVenuesUseCase()) {
                is ToolsResult.Success -> venues = result.data
                is ToolsResult.Error -> {
                    venues = emptyList()
                    message = result.message
                }
            }

            when (val result = fetchVenueFavoritesUseCase()) {
                is ToolsResult.Success -> {
                    favorites = linkedSetOf<String>().apply { addAll(result.data) }
                }

                is ToolsResult.Error -> {
                    if (message == null) message = result.message
                }
            }

            applyState(isLoading = false, message = message)
        }
    }

    private fun persistFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch {
            when (val result = setVenueFavoriteUseCase(id, favorite)) {
                is ToolsResult.Success -> Unit
                is ToolsResult.Error -> {
                    if (favorite) favorites.remove(id) else favorites.add(id)
                    applyState(message = result.message)
                }
            }
        }
    }

    private fun applyState(
        isLoading: Boolean = false,
        message: String? = null,
    ) {
        val filtered = venues
            .asSequence()
            .filter { venue ->
                val q = query.trim()
                if (q.isBlank()) return@filter true
                val haystack = buildString {
                    append(venue.name)
                    append(' ')
                    append(venue.location)
                    append(' ')
                    append(venue.tags.joinToString(" "))
                }
                haystack.contains(q, ignoreCase = true)
            }
            .toList()

        val sorted = when (sortType) {
            VenueSortType.Recommended -> filtered.sortedByDescending { venue ->
                recommendationScore(venue, favorites.contains(venue.id), query)
            }

            VenueSortType.FavoriteFirst -> filtered.sortedWith(
                compareByDescending<Venue> { favorites.contains(it.id) }
                    .thenByDescending { recommendationScore(it, favorites.contains(it.id), query) }
            )

            VenueSortType.PriceAsc -> filtered.sortedBy { it.pricePerGuest }
            VenueSortType.RatingDesc -> filtered.sortedByDescending { it.rating }
        }

        _state.update {
            it.copy(
                query = query,
                sortType = sortType,
                resultCountText = "${sorted.size}곳",
                items = sorted.map { venue ->
                    VenueUiItem(
                        id = venue.id,
                        name = venue.name,
                        location = venue.location,
                        priceText = "1인 ${formatKrw(venue.pricePerGuest)}",
                        ratingText = "평점 ${formatOneDecimal(venue.rating)}",
                        capacityText = "수용 ${venue.capacity}명",
                        tags = venue.tags,
                        isFavorite = favorites.contains(venue.id),
                        accent = Color(venue.accentHex),
                        recommendationNote = recommendationNote(venue, favorites.contains(venue.id)),
                    )
                },
                isEmpty = sorted.isEmpty(),
                isLoading = isLoading,
                message = message,
            )
        }
    }
}

private fun formatKrw(amount: Int): String {
    val digits = amount.toString().reversed().chunked(3).joinToString(",").reversed()
    return "₩$digits"
}

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    val integerPart = rounded.toInt()
    return if (rounded == integerPart.toDouble()) "$integerPart.0" else rounded.toString()
}

private fun recommendationScore(
    venue: Venue,
    isFavorite: Boolean,
    query: String,
): Double {
    val favoriteBonus = if (isFavorite) 350.0 else 0.0
    val qualityScore = venue.rating * 120.0
    val pricePenalty = venue.pricePerGuest / 2500.0
    val capacityScore = venue.capacity / 30.0
    val queryBonus = if (
        query.isNotBlank() &&
        (venue.name.contains(query, ignoreCase = true) || venue.location.contains(query, ignoreCase = true))
    ) {
        35.0
    } else {
        0.0
    }
    return qualityScore + capacityScore + favoriteBonus + queryBonus - pricePenalty
}

private fun recommendationNote(
    venue: Venue,
    isFavorite: Boolean,
): String {
    return when {
        isFavorite -> "찜한 웨딩홀 우선 추천"
        venue.rating >= 4.8 -> "평점이 높은 웨딩홀"
        venue.pricePerGuest <= 85_000 -> "예산 친화적인 선택"
        else -> "평점/가격/수용 인원을 균형 반영"
    }
}

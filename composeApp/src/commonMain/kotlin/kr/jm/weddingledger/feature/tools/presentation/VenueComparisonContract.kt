package kr.jm.weddingledger.feature.tools.presentation

import androidx.compose.ui.graphics.Color

enum class VenueSortType(val label: String) {
    Recommended("추천순"),
    FavoriteFirst("찜 우선"),
    PriceAsc("예산 낮은순"),
    RatingDesc("평점 높은순"),
}

data class VenueUiItem(
    val id: String,
    val name: String,
    val location: String,
    val priceText: String,
    val ratingText: String,
    val capacityText: String,
    val tags: List<String>,
    val isFavorite: Boolean,
    val accent: Color,
    val recommendationNote: String?,
)

data class VenueComparisonUiState(
    val query: String,
    val sortType: VenueSortType,
    val sortOptions: List<VenueSortType>,
    val resultCountText: String,
    val items: List<VenueUiItem>,
    val isEmpty: Boolean,
    val isLoading: Boolean,
    val message: String?,
) {
    companion object {
        fun initial(): VenueComparisonUiState = VenueComparisonUiState(
            query = "",
            sortType = VenueSortType.Recommended,
            sortOptions = VenueSortType.entries,
            resultCountText = "0곳",
            items = emptyList(),
            isEmpty = false,
            isLoading = true,
            message = null,
        )
    }
}

sealed interface VenueComparisonAction {
    data class OnQueryChange(val value: String) : VenueComparisonAction
    data class OnSelectSort(val value: VenueSortType) : VenueComparisonAction
    data class OnToggleFavorite(val id: String) : VenueComparisonAction
    data object OnShareTopResults : VenueComparisonAction
    data object OnClearFilter : VenueComparisonAction
}

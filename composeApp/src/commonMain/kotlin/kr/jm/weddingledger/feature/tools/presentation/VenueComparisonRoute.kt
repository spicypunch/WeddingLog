package kr.jm.weddingledger.feature.tools.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kr.jm.weddingledger.core.platform.shareText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VenueComparisonRoute(
    onBack: () -> Unit,
) {
    val viewModel: VenueComparisonViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    VenueComparisonScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                VenueComparisonAction.OnShareTopResults -> {
                    shareText(buildVenueShareText(state))
                }

                else -> viewModel.onAction(action)
            }
        },
    )
}

private fun buildVenueShareText(state: VenueComparisonUiState): String {
    if (state.items.isEmpty()) {
        return "WeddingLog 웨딩홀 비교\n조건에 맞는 결과가 없어 다시 검색이 필요합니다."
    }

    val top = state.items.take(3)
    val lines = buildList {
        add("WeddingLog 웨딩홀 비교 결과")
        if (state.query.isNotBlank()) add("검색어: ${state.query}")
        add("정렬: ${state.sortType.label}")
        add("")
        top.forEachIndexed { index, item ->
            add("${index + 1}. ${item.name}")
            add("   - ${item.location}")
            add("   - ${item.priceText} / ${item.ratingText} / ${item.capacityText}")
            item.recommendationNote?.takeIf { it.isNotBlank() }?.let { add("   - $it") }
        }
    }
    return lines.joinToString("\n")
}

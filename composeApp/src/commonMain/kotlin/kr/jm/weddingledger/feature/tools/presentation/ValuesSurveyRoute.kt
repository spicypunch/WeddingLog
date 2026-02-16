package kr.jm.weddingledger.feature.tools.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ValuesSurveyRoute(
    onBack: () -> Unit,
) {
    val viewModel: ValuesSurveyViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    ValuesSurveyScreen(
        state = state,
        onBack = onBack,
        onAction = viewModel::onAction,
    )
}

package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.runtime.Composable

@Composable
fun HomeRoute() {
    HomeScreen(
        state = HomeUiState.sample(),
        onAction = { /* TODO */ }
    )
}


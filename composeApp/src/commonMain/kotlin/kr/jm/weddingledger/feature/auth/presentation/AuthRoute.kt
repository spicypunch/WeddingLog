package kr.jm.weddingledger.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun AuthRoute(
    initialMessage: String? = null,
) {
    val viewModel: AuthViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrBlank()) {
            viewModel.onAction(AuthAction.OnShowMessage(AuthMessage.Error(initialMessage)))
        }
    }

    AuthScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}


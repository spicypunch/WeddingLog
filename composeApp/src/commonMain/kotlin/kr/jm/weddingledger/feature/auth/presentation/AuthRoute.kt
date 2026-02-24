package kr.jm.weddingledger.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun AuthRoute(
    initialMessage: AuthMessage? = null,
) {
    val viewModel: AuthViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(initialMessage) {
        if (initialMessage != null) {
            viewModel.onAction(AuthAction.OnShowMessage(initialMessage))
        }
    }

    AuthScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

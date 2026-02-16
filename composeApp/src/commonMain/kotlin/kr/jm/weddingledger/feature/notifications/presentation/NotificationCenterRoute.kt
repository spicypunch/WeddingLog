package kr.jm.weddingledger.feature.notifications.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kr.jm.weddingledger.core.platform.requestNotificationPermission
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationCenterRoute(
    onBack: () -> Unit,
) {
    val viewModel: NotificationCenterViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    NotificationCenterScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                NotificationCenterAction.OnClickRequestPermission -> {
                    scope.launch {
                        val result = requestNotificationPermission()
                        viewModel.onNotificationPermissionResult(result)
                    }
                }

                else -> viewModel.onAction(action)
            }
        },
    )
}

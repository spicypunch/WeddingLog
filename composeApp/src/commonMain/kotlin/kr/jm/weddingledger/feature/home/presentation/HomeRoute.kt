package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kr.jm.weddingledger.core.platform.requestNotificationPermission
import kr.jm.weddingledger.core.settings.data.NotificationPermissionPromptStorage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenMoney: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (!NotificationPermissionPromptStorage.shouldPromptOnHomeEntry()) return@LaunchedEffect
        NotificationPermissionPromptStorage.markHomeEntryPrompted()
        requestNotificationPermission()
    }

    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                HomeAction.OnClickAddExpense,
                HomeAction.OnClickViewAllCategories,
                is HomeAction.OnClickCategory,
                is HomeAction.OnClickRecentExpense,
                -> onOpenMoney()

                HomeAction.OnClickNotifications -> onOpenNotifications()
                HomeAction.OnRefresh -> viewModel.onAction(action)
            }
        }
    )
}

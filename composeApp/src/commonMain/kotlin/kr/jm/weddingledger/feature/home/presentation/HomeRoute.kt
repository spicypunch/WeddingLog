package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenMoney: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

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
            }
        }
    )
}

package kr.jm.weddingledger.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.platform.openAppNotificationSettings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onOpenProfileEdit: () -> Unit,
    onOpenAccountSettings: (email: String) -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(themeStyle) {
        viewModel.onThemeStyleChanged(themeStyle)
    }

    SettingsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SettingsAction.OnClickBack -> onBack()
                is SettingsAction.OnSelectTheme -> {
                    onThemeStyleChange(action.style)
                    viewModel.onThemeStyleChanged(action.style)
                }
                SettingsAction.OnClickLogout -> onLogout()
                SettingsAction.OnClickNotificationSettings -> {
                    val opened = openAppNotificationSettings()
                    if (!opened) onOpenNotificationCenter()
                }
                SettingsAction.OnClickProfileEdit -> onOpenProfileEdit()
                SettingsAction.OnClickAccountSettings -> onOpenAccountSettings(state.email)
                SettingsAction.OnClickAppInfo -> onOpenAppInfo()
                else -> viewModel.onAction(action)
            }
        }
    )
}

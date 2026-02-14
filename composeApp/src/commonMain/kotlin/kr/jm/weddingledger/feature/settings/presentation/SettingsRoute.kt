package kr.jm.weddingledger.feature.settings.presentation

import androidx.compose.runtime.Composable
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle

@Composable
fun SettingsRoute(
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(
        state = SettingsUiState.sample(themeStyle),
        onAction = { action ->
            when (action) {
                SettingsAction.OnClickBack -> onBack()
                is SettingsAction.OnSelectTheme -> onThemeStyleChange(action.style)
                SettingsAction.OnClickCopyInviteCode,
                SettingsAction.OnClickLogout,
                SettingsAction.OnClickProfileEdit,
                SettingsAction.OnClickNotificationSettings,
                SettingsAction.OnClickAccountSettings,
                SettingsAction.OnClickAppInfo -> Unit // TODO
            }
        }
    )
}


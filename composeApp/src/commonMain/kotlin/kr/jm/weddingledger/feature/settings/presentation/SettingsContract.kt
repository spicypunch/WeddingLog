package kr.jm.weddingledger.feature.settings.presentation

import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle

data class SettingsUiState(
    val title: String,
    val coupleName: String,
    val email: String,
    val inviteCode: String,
    val selectedTheme: WeddingLogThemeStyle,
    val isLoading: Boolean = false,
    val message: String? = null,
)

sealed interface SettingsAction {
    data object OnClickBack : SettingsAction
    data object OnClickProfileEdit : SettingsAction
    data object OnClickCopyInviteCode : SettingsAction
    data class OnSelectTheme(val style: WeddingLogThemeStyle) : SettingsAction

    data object OnClickNotificationSettings : SettingsAction
    data object OnClickAccountSettings : SettingsAction
    data object OnClickAppInfo : SettingsAction

    data object OnClickLogout : SettingsAction
    data object OnDismissMessage : SettingsAction
}

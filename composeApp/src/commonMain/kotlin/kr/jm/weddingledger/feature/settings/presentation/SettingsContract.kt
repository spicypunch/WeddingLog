package kr.jm.weddingledger.feature.settings.presentation

import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle

data class SettingsUiState(
    val title: String,
    val coupleName: String,
    val inviteCode: String,
    val selectedTheme: WeddingLogThemeStyle,
) {
    companion object {
        fun sample(selectedTheme: WeddingLogThemeStyle): SettingsUiState = SettingsUiState(
            title = "설정",
            coupleName = "신랑 김민수 & 신부 이나래",
            inviteCode = "WDLG-1234",
            selectedTheme = selectedTheme
        )
    }
}

sealed interface SettingsAction {
    data object OnClickBack : SettingsAction
    data object OnClickProfileEdit : SettingsAction
    data object OnClickCopyInviteCode : SettingsAction
    data class OnSelectTheme(val style: WeddingLogThemeStyle) : SettingsAction

    data object OnClickNotificationSettings : SettingsAction
    data object OnClickAccountSettings : SettingsAction
    data object OnClickAppInfo : SettingsAction

    data object OnClickLogout : SettingsAction
}


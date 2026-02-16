package kr.jm.weddingledger.feature.settings.presentation

data class AccountSettingsUiState(
    val email: String,
    val isLoading: Boolean,
    val message: String?,
) {
    val canSendEmailActions: Boolean
        get() = !isLoading && email.contains("@")

    companion object {
        fun initial(email: String): AccountSettingsUiState = AccountSettingsUiState(
            email = email,
            isLoading = false,
            message = null,
        )
    }
}

sealed interface AccountSettingsAction {
    data object OnClickSendPasswordReset : AccountSettingsAction
    data object OnClickResendVerificationEmail : AccountSettingsAction
    data object OnDismissMessage : AccountSettingsAction
}

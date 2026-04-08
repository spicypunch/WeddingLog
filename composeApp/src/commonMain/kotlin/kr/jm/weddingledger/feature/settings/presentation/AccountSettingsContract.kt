package kr.jm.weddingledger.feature.settings.presentation

import kr.jm.weddingledger.feature.auth.domain.model.AuthProvider

data class AccountSettingsUiState(
    val email: String,
    val isLoading: Boolean,
    val isDeleting: Boolean,
    val authProvider: AuthProvider,
    val showDeleteConfirm: Boolean,
    val message: String?,
) {
    val canSendEmailActions: Boolean
        get() = !isLoading && !isDeleting && email.contains("@")

    val canDeleteAccount: Boolean
        get() = !isLoading && !isDeleting

    companion object {
        fun initial(email: String): AccountSettingsUiState = AccountSettingsUiState(
            email = email,
            isLoading = false,
            isDeleting = false,
            authProvider = AuthProvider.Unknown,
            showDeleteConfirm = false,
            message = null,
        )
    }
}

sealed interface AccountSettingsAction {
    data object OnClickSendPasswordReset : AccountSettingsAction
    data object OnClickResendVerificationEmail : AccountSettingsAction
    data object OnClickDeleteAccount : AccountSettingsAction
    data object OnConfirmDeleteAccount : AccountSettingsAction
    data object OnDismissDeleteConfirm : AccountSettingsAction
    data object OnDismissMessage : AccountSettingsAction
}

sealed interface AccountSettingsEffect {
    data object AccountDeleted : AccountSettingsEffect
}

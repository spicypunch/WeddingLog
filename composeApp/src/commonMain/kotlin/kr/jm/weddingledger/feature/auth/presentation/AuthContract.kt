package kr.jm.weddingledger.feature.auth.presentation

enum class AuthMode {
    SignIn,
    SignUp,
}

sealed interface AuthMessage {
    data class Error(val text: String) : AuthMessage
    data class Info(val text: String) : AuthMessage
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val message: AuthMessage? = null,
) {
    val submitLabel: String
        get() = when (mode) {
            AuthMode.SignIn -> "로그인"
            AuthMode.SignUp -> "회원가입"
        }

    val toggleModeLabel: String
        get() = when (mode) {
            AuthMode.SignIn -> "계정이 없나요? 회원가입"
            AuthMode.SignUp -> "이미 계정이 있나요? 로그인"
        }

    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

sealed interface AuthAction {
    data class OnEmailChange(val value: String) : AuthAction
    data class OnPasswordChange(val value: String) : AuthAction
    data object OnClickToggleMode : AuthAction
    data object OnClickSubmit : AuthAction
    data object OnClickResetPassword : AuthAction
    data object OnDismissMessage : AuthAction

    data class OnShowMessage(val message: AuthMessage) : AuthAction
}


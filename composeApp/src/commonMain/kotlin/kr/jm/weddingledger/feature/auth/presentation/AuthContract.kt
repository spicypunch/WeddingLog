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
    val passwordConfirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val isResendingVerificationEmail: Boolean = false,
    val isEmailVerificationRequired: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val resendAttemptCount: Int = 0,
    val resendAttemptLimit: Int = 5,
    val message: AuthMessage? = null,
) {
    val submitLabel: String
        get() = when (mode) {
            AuthMode.SignIn -> "로그인"
            AuthMode.SignUp -> "가입하기"
        }

    val toggleModeLabel: String
        get() = when (mode) {
            AuthMode.SignIn -> "계정이 없나요? 회원가입"
            AuthMode.SignUp -> "이미 계정이 있나요? 로그인"
        }

    val isSubmitEnabled: Boolean
        get() = when (mode) {
            AuthMode.SignIn -> email.isNotBlank() &&
                password.isNotBlank() &&
                !isLoading &&
                !isResendingVerificationEmail
            AuthMode.SignUp -> {
                email.isNotBlank() &&
                    password.isNotBlank() &&
                    passwordConfirm.isNotBlank() &&
                    password == passwordConfirm &&
                    isTermsAccepted &&
                    !isLoading &&
                    !isResendingVerificationEmail
            }
        }

    val isResendVerificationEnabled: Boolean
        get() = mode == AuthMode.SignIn &&
            isEmailVerificationRequired &&
            email.isNotBlank() &&
            !isLoading &&
            !isResendingVerificationEmail &&
            resendCooldownSeconds <= 0 &&
            resendAttemptCount < resendAttemptLimit

    val shouldShowVerificationGuide: Boolean
        get() = mode == AuthMode.SignIn && isEmailVerificationRequired

    val remainingResendAttempts: Int
        get() = (resendAttemptLimit - resendAttemptCount).coerceAtLeast(0)

    val resendButtonLabel: String
        get() = if (resendCooldownSeconds > 0) {
            "인증 메일 재전송 (${resendCooldownSeconds}초)"
        } else {
            "인증 메일 재전송"
        }
}

sealed interface AuthAction {
    data class OnEmailChange(val value: String) : AuthAction
    data class OnPasswordChange(val value: String) : AuthAction
    data class OnPasswordConfirmChange(val value: String) : AuthAction
    data object OnClickTogglePasswordVisibility : AuthAction
    data class OnTermsAcceptedChange(val accepted: Boolean) : AuthAction
    data object OnClickToggleMode : AuthAction
    data object OnClickSubmit : AuthAction
    data object OnClickResetPassword : AuthAction
    data object OnClickResendVerificationEmail : AuthAction
    data object OnDismissMessage : AuthAction

    data class OnShowMessage(val message: AuthMessage) : AuthAction
}

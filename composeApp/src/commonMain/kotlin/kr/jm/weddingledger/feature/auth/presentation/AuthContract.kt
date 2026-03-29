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
    val otpCode: String = "",
    val isPasswordVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isKakaoLoginAvailable: Boolean = true,
    val isGoogleLoginAvailable: Boolean = true,
    val isAppleLoginAvailable: Boolean = true,
    val isLoading: Boolean = false,
    val isSendingOtp: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val isOtpCodeSent: Boolean = false,
    val otpCooldownSeconds: Int = 0,
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
                !isSendingOtp &&
                !isVerifyingOtp &&
                !isResendingVerificationEmail
            AuthMode.SignUp -> {
                email.isNotBlank() &&
                    password.isNotBlank() &&
                    passwordConfirm.isNotBlank() &&
                    password == passwordConfirm &&
                    isTermsAccepted &&
                    !isLoading &&
                    !isSendingOtp &&
                    !isVerifyingOtp &&
                    !isResendingVerificationEmail
            }
        }

    val isSocialSignInEnabled: Boolean
        get() = !isLoading &&
            !isSendingOtp &&
            !isVerifyingOtp &&
            !isResendingVerificationEmail

    val hasAnySocialLoginAvailable: Boolean
        get() = isKakaoLoginAvailable || isGoogleLoginAvailable || isAppleLoginAvailable

    val isSendOtpEnabled: Boolean
        get() = email.isNotBlank() &&
            !isLoading &&
            !isSendingOtp &&
            !isVerifyingOtp &&
            otpCooldownSeconds <= 0

    val isVerifyOtpEnabled: Boolean
        get() = email.isNotBlank() &&
            otpCode.length == 6 &&
            !isLoading &&
            !isSendingOtp &&
            !isVerifyingOtp

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

    val otpSendButtonLabel: String
        get() = if (otpCooldownSeconds > 0) {
            "OTP 재전송 (${otpCooldownSeconds}초)"
        } else if (isOtpCodeSent) {
            "OTP 재전송"
        } else {
            "OTP 받기"
        }
}

sealed interface AuthAction {
    data class OnEmailChange(val value: String) : AuthAction
    data class OnPasswordChange(val value: String) : AuthAction
    data class OnPasswordConfirmChange(val value: String) : AuthAction
    data class OnOtpCodeChange(val value: String) : AuthAction
    data object OnClickTogglePasswordVisibility : AuthAction
    data class OnTermsAcceptedChange(val accepted: Boolean) : AuthAction
    data object OnClickToggleMode : AuthAction
    data object OnClickSubmit : AuthAction
    data object OnClickSignInWithKakao : AuthAction
    data object OnClickSignInWithGoogle : AuthAction
    data object OnClickSignInWithApple : AuthAction
    data object OnClickTermsOfService : AuthAction
    data object OnClickPrivacyPolicy : AuthAction
    data object OnClickSendOtp : AuthAction
    data object OnClickVerifyOtp : AuthAction
    data object OnClickResetPassword : AuthAction
    data object OnClickResendVerificationEmail : AuthAction
    data object OnDismissMessage : AuthAction

    data class OnShowMessage(val message: AuthMessage) : AuthAction
}

sealed interface AuthEffect {
    data class OpenExternalUrl(val url: String) : AuthEffect
}

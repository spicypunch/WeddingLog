package kr.jm.weddingledger.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.usecase.SendEmailOtpUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithAppleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithGoogleUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInWithKakaoUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.VerifyEmailOtpUseCase

class AuthViewModel(
    private val signInWithKakaoUseCase: SignInWithKakaoUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
    private val sendEmailOtpUseCase: SendEmailOtpUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resendSignUpVerificationEmailUseCase: ResendSignUpVerificationEmailUseCase,
    private val termsOfServiceUrl: String = BuildConfig.TERMS_OF_SERVICE_URL,
    private val privacyPolicyUrl: String = BuildConfig.PRIVACY_POLICY_URL,
) : ViewModel() {
    private val resendCooldownTotalSeconds = 60
    private val otpCooldownTotalSeconds = 60
    private var resendCooldownJob: Job? = null
    private var otpCooldownJob: Job? = null

    private val _state = MutableStateFlow(
        AuthUiState(
            isKakaoLoginAvailable = BuildConfig.ENABLE_KAKAO_LOGIN,
            isGoogleLoginAvailable = BuildConfig.ENABLE_GOOGLE_LOGIN,
            isAppleLoginAvailable = BuildConfig.ENABLE_APPLE_LOGIN,
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private val _effect = Channel<AuthEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.OnEmailChange -> {
                val nextEmail = action.value
                _state.update { current ->
                    if (current.email.trim() == nextEmail.trim()) {
                        current.copy(email = nextEmail)
                    } else {
                        current.copy(email = nextEmail).clearVerificationGuide().clearOtpState()
                    }
                }
                if (_state.value.resendCooldownSeconds == 0) {
                    resendCooldownJob?.cancel()
                }
                if (_state.value.otpCooldownSeconds == 0) {
                    otpCooldownJob?.cancel()
                }
            }
            is AuthAction.OnPasswordChange -> _state.update { it.copy(password = action.value) }
            is AuthAction.OnPasswordConfirmChange -> _state.update { it.copy(passwordConfirm = action.value) }
            is AuthAction.OnOtpCodeChange -> _state.update { it.copy(otpCode = action.value.filter { ch -> ch.isDigit() }.take(6)) }
            AuthAction.OnClickTogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is AuthAction.OnTermsAcceptedChange -> _state.update { it.copy(isTermsAccepted = action.accepted) }
            AuthAction.OnClickToggleMode -> toggleMode()
            AuthAction.OnClickSubmit -> submit()
            AuthAction.OnClickSignInWithKakao -> signInWithKakao()
            AuthAction.OnClickSignInWithGoogle -> signInWithGoogle()
            AuthAction.OnClickSignInWithApple -> signInWithApple()
            AuthAction.OnClickTermsOfService -> openLegalDocument(
                url = termsOfServiceUrl,
                documentName = "이용약관",
            )
            AuthAction.OnClickPrivacyPolicy -> openLegalDocument(
                url = privacyPolicyUrl,
                documentName = "개인정보 처리방침",
            )
            AuthAction.OnClickSendOtp -> sendOtp()
            AuthAction.OnClickVerifyOtp -> verifyOtp()
            AuthAction.OnClickResetPassword -> resetPassword()
            AuthAction.OnClickResendVerificationEmail -> resendVerificationEmail()
            AuthAction.OnDismissMessage -> _state.update { it.copy(message = null) }
            is AuthAction.OnShowMessage -> _state.update { it.copy(message = action.message) }
        }
    }

    private fun toggleMode() {
        _state.update {
            it.copy(
                mode = AuthMode.SignIn,
                message = AuthMessage.Info("회원가입은 현재 비활성화되어 있습니다. 소셜 로그인을 이용해주세요."),
            )
        }
        resendCooldownJob?.cancel()
    }

    private fun submit() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail || snapshot.isSendingOtp || snapshot.isVerifyingOtp) return

        if (snapshot.mode == AuthMode.SignUp) {
            _state.update {
                it.copy(
                    mode = AuthMode.SignIn,
                    isLoading = false,
                    message = AuthMessage.Info("회원가입은 현재 비활성화되어 있습니다. 소셜 로그인을 이용해주세요."),
                )
            }
            return
        }
        if (!ensureLoginConsent(snapshot)) return

        _state.update {
            it.copy(
                isLoading = false,
                message = AuthMessage.Info("이메일 로그인은 현재 지원하지 않습니다. 소셜 로그인을 이용해주세요."),
            )
        }
    }

    private fun signInWithGoogle() {
        val snapshot = _state.value
        if (!snapshot.isGoogleLoginAvailable) {
            _state.update { it.copy(message = AuthMessage.Info("Google 로그인은 현재 비활성화되어 있습니다.")) }
            return
        }
        if (snapshot.isLoading || snapshot.isSendingOtp || snapshot.isVerifyingOtp || snapshot.isResendingVerificationEmail) return
        if (!ensureLoginConsent(snapshot)) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = try {
                signInWithGoogleUseCase()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = AuthMessage.Info("브라우저에서 Google 로그인 절차를 완료해주세요."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = AuthMessage.Error(result.message)) }
                }
            }
        }
    }

    private fun signInWithKakao() {
        val snapshot = _state.value
        if (!snapshot.isKakaoLoginAvailable) {
            _state.update { it.copy(message = AuthMessage.Info("Kakao 로그인은 현재 비활성화되어 있습니다.")) }
            return
        }
        if (snapshot.isLoading || snapshot.isSendingOtp || snapshot.isVerifyingOtp || snapshot.isResendingVerificationEmail) return
        if (!ensureLoginConsent(snapshot)) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = try {
                signInWithKakaoUseCase()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = AuthMessage.Info("브라우저에서 Kakao 로그인 절차를 완료해주세요."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = AuthMessage.Error(result.message)) }
                }
            }
        }
    }

    private fun signInWithApple() {
        val snapshot = _state.value
        if (!snapshot.isAppleLoginAvailable) {
            _state.update { it.copy(message = AuthMessage.Info("Apple 로그인은 현재 비활성화되어 있습니다.")) }
            return
        }
        if (snapshot.isLoading || snapshot.isSendingOtp || snapshot.isVerifyingOtp || snapshot.isResendingVerificationEmail) return
        if (!ensureLoginConsent(snapshot)) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = try {
                signInWithAppleUseCase()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = AuthMessage.Info("브라우저에서 Apple 로그인 절차를 완료해주세요."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = AuthMessage.Error(result.message)) }
                }
            }
        }
    }

    private fun sendOtp() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSendingOtp || snapshot.isVerifyingOtp) return
        if (snapshot.otpCooldownSeconds > 0) return
        if (!ensureLoginConsent(snapshot)) return

        _state.update { it.copy(isSendingOtp = true, message = null) }
        viewModelScope.launch {
            val result = try {
                sendEmailOtpUseCase(snapshot.email.trim())
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    startOtpCooldown()
                    _state.update {
                        it.copy(
                            isSendingOtp = false,
                            isOtpCodeSent = true,
                            message = AuthMessage.Info("이메일로 OTP를 보냈습니다. 받은 코드를 입력해주세요."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isSendingOtp = false,
                            message = AuthMessage.Error(result.message),
                        )
                    }
                }
            }
        }
    }

    private fun ensureLoginConsent(snapshot: AuthUiState): Boolean {
        if (snapshot.isTermsAccepted) return true
        _state.update {
            it.copy(
                message = AuthMessage.Info(
                    "로그인 전에 이용약관과 개인정보 처리방침에 동의해주세요."
                ),
            )
        }
        return false
    }

    private fun openLegalDocument(
        url: String,
        documentName: String,
    ) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            _state.update {
                it.copy(
                    message = AuthMessage.Info(
                        "$documentName 문서가 아직 연결되지 않았습니다. 설정에서 URL을 추가해주세요."
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _effect.send(AuthEffect.OpenExternalUrl(normalizedUrl))
        }
    }

    private fun verifyOtp() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSendingOtp || snapshot.isVerifyingOtp) return
        if (!ensureLoginConsent(snapshot)) return

        _state.update { it.copy(isVerifyingOtp = true, message = null) }
        viewModelScope.launch {
            val result = try {
                verifyEmailOtpUseCase(
                    email = snapshot.email.trim(),
                    token = snapshot.otpCode,
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isVerifyingOtp = false,
                            otpCode = "",
                            message = AuthMessage.Info("OTP 인증이 완료되었습니다."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isVerifyingOtp = false,
                            message = AuthMessage.Error(result.message),
                        )
                    }
                }
            }
        }
    }

    private fun resetPassword() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail || snapshot.isSendingOtp || snapshot.isVerifyingOtp) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = try {
                resetPasswordUseCase(snapshot.email.trim())
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }
            when (result) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = AuthMessage.Info("비밀번호 재설정 이메일을 보냈습니다."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = AuthMessage.Error(result.message),
                        )
                    }
                }
            }
        }
    }

    private fun resendVerificationEmail() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail || snapshot.isSendingOtp || snapshot.isVerifyingOtp) return
        if (snapshot.resendCooldownSeconds > 0) return
        if (snapshot.resendAttemptCount >= snapshot.resendAttemptLimit) {
            _state.update {
                it.copy(
                    message = AuthMessage.Error("인증 메일 재전송은 최대 ${snapshot.resendAttemptLimit}회까지 가능합니다. 잠시 후 다시 시도해주세요."),
                )
            }
            return
        }

        _state.update { it.copy(isResendingVerificationEmail = true, message = null) }
        viewModelScope.launch {
            val result = try {
                resendSignUpVerificationEmailUseCase(snapshot.email.trim())
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    startResendCooldown()
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                            isEmailVerificationRequired = true,
                            resendAttemptCount = (it.resendAttemptCount + 1).coerceAtMost(it.resendAttemptLimit),
                            message = AuthMessage.Info("인증 메일을 다시 보냈습니다. 메일함을 확인해주세요."),
                        )
                    }
                }
                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isResendingVerificationEmail = false,
                            message = AuthMessage.Error(result.message),
                        )
                    }
                }
            }
        }
    }

    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            var remaining = resendCooldownTotalSeconds
            _state.update { it.copy(resendCooldownSeconds = remaining) }
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                _state.update { current ->
                    if (current.isEmailVerificationRequired) {
                        current.copy(resendCooldownSeconds = remaining)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun startOtpCooldown() {
        otpCooldownJob?.cancel()
        otpCooldownJob = viewModelScope.launch {
            var remaining = otpCooldownTotalSeconds
            _state.update { it.copy(otpCooldownSeconds = remaining) }
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1
                _state.update { current ->
                    if (current.isOtpCodeSent) {
                        current.copy(otpCooldownSeconds = remaining)
                    } else {
                        current
                    }
                }
            }
        }
    }

    override fun onCleared() {
        resendCooldownJob?.cancel()
        otpCooldownJob?.cancel()
        super.onCleared()
    }
}

private fun AuthUiState.clearVerificationGuide(): AuthUiState {
    return copy(
        isEmailVerificationRequired = false,
        resendCooldownSeconds = 0,
        resendAttemptCount = 0,
    )
}

private fun AuthUiState.clearOtpState(): AuthUiState {
    return copy(
        otpCode = "",
        isOtpCodeSent = false,
        otpCooldownSeconds = 0,
    )
}

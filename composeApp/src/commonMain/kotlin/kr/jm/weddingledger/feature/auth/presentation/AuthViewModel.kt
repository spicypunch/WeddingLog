package kr.jm.weddingledger.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignUpUseCase

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resendSignUpVerificationEmailUseCase: ResendSignUpVerificationEmailUseCase,
) : ViewModel() {
    private val resendCooldownTotalSeconds = 60
    private var resendCooldownJob: Job? = null

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.OnEmailChange -> {
                val nextEmail = action.value
                _state.update { current ->
                    if (current.email.trim() == nextEmail.trim()) {
                        current.copy(email = nextEmail)
                    } else {
                        current.copy(email = nextEmail).clearVerificationGuide()
                    }
                }
                if (_state.value.resendCooldownSeconds == 0) {
                    resendCooldownJob?.cancel()
                }
            }
            is AuthAction.OnPasswordChange -> _state.update { it.copy(password = action.value) }
            is AuthAction.OnPasswordConfirmChange -> _state.update { it.copy(passwordConfirm = action.value) }
            AuthAction.OnClickTogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is AuthAction.OnTermsAcceptedChange -> _state.update { it.copy(isTermsAccepted = action.accepted) }
            AuthAction.OnClickToggleMode -> toggleMode()
            AuthAction.OnClickSubmit -> submit()
            AuthAction.OnClickResetPassword -> resetPassword()
            AuthAction.OnClickResendVerificationEmail -> resendVerificationEmail()
            AuthAction.OnDismissMessage -> _state.update { it.copy(message = null) }
            is AuthAction.OnShowMessage -> _state.update { it.copy(message = action.message) }
        }
    }

    private fun toggleMode() {
        _state.update { current ->
            current.copy(
                mode = when (current.mode) {
                    AuthMode.SignIn -> AuthMode.SignUp
                    AuthMode.SignUp -> AuthMode.SignIn
                },
                password = "",
                passwordConfirm = "",
                isPasswordVisible = false,
                isTermsAccepted = false,
                isResendingVerificationEmail = false,
                message = null
            ).clearVerificationGuide()
        }
        resendCooldownJob?.cancel()
    }

    private fun submit() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail) return

        if (snapshot.mode == AuthMode.SignUp) {
            if (snapshot.password != snapshot.passwordConfirm) {
                _state.update { it.copy(message = AuthMessage.Error("비밀번호가 일치하지 않습니다.")) }
                return
            }
            if (!snapshot.isTermsAccepted) {
                _state.update { it.copy(message = AuthMessage.Error("이용약관 및 개인정보 처리방침에 동의해주세요.")) }
                return
            }
        }

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = try {
                when (snapshot.mode) {
                    AuthMode.SignIn -> signInUseCase(snapshot.email.trim(), snapshot.password)
                    AuthMode.SignUp -> signUpUseCase(snapshot.email.trim(), snapshot.password)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                AuthResult.Error("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }

            when (result) {
                is AuthResult.Success -> {
                    when (snapshot.mode) {
                        AuthMode.SignIn -> {
                            _state.update { it.copy(isLoading = false).clearVerificationGuide() }
                        }
                        AuthMode.SignUp -> {
                            // Supabase 설정에 따라 회원가입 직후 로그인되지 않을 수 있습니다(이메일 인증 등).
                            _state.update {
                                it.copy(
                                    mode = AuthMode.SignIn,
                                    password = "",
                                    passwordConfirm = "",
                                    isPasswordVisible = false,
                                    isTermsAccepted = false,
                                    isLoading = false,
                                    message = AuthMessage.Info("인증 메일을 보냈습니다. 메일함에서 인증한 뒤 로그인해주세요."),
                                ).clearVerificationGuide()
                            }
                        }
                    }
                }
                is AuthResult.Error -> {
                    if (snapshot.mode == AuthMode.SignIn && result.message.isEmailVerificationRelated()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isEmailVerificationRequired = true,
                                message = AuthMessage.Info(
                                    "이메일 인증이 아직 완료되지 않았습니다. 메일함의 인증 링크를 눌러 완료해주세요."
                                ),
                            )
                        }
                    } else {
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
    }

    private fun resetPassword() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail) return

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
        if (snapshot.isLoading || snapshot.isResendingVerificationEmail) return
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

    override fun onCleared() {
        resendCooldownJob?.cancel()
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

private fun String.isEmailVerificationRelated(): Boolean {
    val normalized = trim()
    return normalized.contains("이메일 인증") || (
        normalized.contains("email", ignoreCase = true) &&
            normalized.contains("confirm", ignoreCase = true)
        )
}

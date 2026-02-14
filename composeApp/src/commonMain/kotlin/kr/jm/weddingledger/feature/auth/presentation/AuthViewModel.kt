package kr.jm.weddingledger.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignInUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignUpUseCase

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.OnEmailChange -> _state.update { it.copy(email = action.value) }
            is AuthAction.OnPasswordChange -> _state.update { it.copy(password = action.value) }
            AuthAction.OnClickToggleMode -> toggleMode()
            AuthAction.OnClickSubmit -> submit()
            AuthAction.OnClickResetPassword -> resetPassword()
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
                message = null,
            )
        }
    }

    private fun submit() {
        val snapshot = _state.value
        if (snapshot.isLoading) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = when (snapshot.mode) {
                AuthMode.SignIn -> signInUseCase(snapshot.email.trim(), snapshot.password)
                AuthMode.SignUp -> signUpUseCase(snapshot.email.trim(), snapshot.password)
            }

            when (result) {
                is AuthResult.Success -> {
                    when (snapshot.mode) {
                        AuthMode.SignIn -> {
                            _state.update { it.copy(isLoading = false) }
                        }
                        AuthMode.SignUp -> {
                            // Supabase 설정에 따라 회원가입 직후 로그인되지 않을 수 있습니다(이메일 인증 등).
                            _state.update {
                                it.copy(
                                    mode = AuthMode.SignIn,
                                    password = "",
                                    isLoading = false,
                                    message = AuthMessage.Info("회원가입이 완료되었습니다. 로그인해주세요."),
                                )
                            }
                        }
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

    private fun resetPassword() {
        val snapshot = _state.value
        if (snapshot.isLoading) return

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = resetPasswordUseCase(snapshot.email.trim())
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
}


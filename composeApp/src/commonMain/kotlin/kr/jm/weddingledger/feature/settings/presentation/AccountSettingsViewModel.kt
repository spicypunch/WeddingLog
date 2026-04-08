package kr.jm.weddingledger.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.DeleteAccountRequest
import kr.jm.weddingledger.feature.auth.domain.usecase.DeleteAccountUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.GetCurrentAuthProviderUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResendSignUpVerificationEmailUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ResetPasswordUseCase

class AccountSettingsViewModel(
    email: String,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resendSignUpVerificationEmailUseCase: ResendSignUpVerificationEmailUseCase,
    private val getCurrentAuthProviderUseCase: GetCurrentAuthProviderUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsUiState.initial(email = email.trim()))
    val state: StateFlow<AccountSettingsUiState> = _state.asStateFlow()
    private val _effect = Channel<AccountSettingsEffect>(capacity = Channel.BUFFERED)
    val effect: Flow<AccountSettingsEffect> = _effect.receiveAsFlow()

    init {
        _state.update {
            it.copy(authProvider = getCurrentAuthProviderUseCase() ?: it.authProvider)
        }
    }

    fun onAction(action: AccountSettingsAction) {
        when (action) {
            AccountSettingsAction.OnClickSendPasswordReset -> sendPasswordResetEmail()
            AccountSettingsAction.OnClickResendVerificationEmail -> resendVerificationEmail()
            AccountSettingsAction.OnClickDeleteAccount -> {
                _state.update { it.copy(showDeleteConfirm = true, message = null) }
            }
            AccountSettingsAction.OnConfirmDeleteAccount -> deleteAccount()
            AccountSettingsAction.OnDismissDeleteConfirm -> {
                _state.update { it.copy(showDeleteConfirm = false) }
            }
            AccountSettingsAction.OnDismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun sendPasswordResetEmail() {
        val email = _state.value.email
        if (email.isBlank()) {
            _state.update { it.copy(message = "이메일 정보를 찾을 수 없습니다.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = resetPasswordUseCase(email)) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = "비밀번호 재설정 메일을 보냈습니다. 메일함을 확인해주세요.",
                        )
                    }
                }

                is AuthResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = result.message) }
                }
            }
        }
    }

    private fun resendVerificationEmail() {
        val email = _state.value.email
        if (email.isBlank()) {
            _state.update { it.copy(message = "이메일 정보를 찾을 수 없습니다.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = resendSignUpVerificationEmailUseCase(email)) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = "인증 메일을 다시 보냈습니다. 메일함을 확인해주세요.",
                        )
                    }
                }

                is AuthResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = result.message) }
                }
            }
        }
    }

    private fun deleteAccount() {
        val provider = _state.value.authProvider
        _state.update {
            it.copy(
                isDeleting = true,
                showDeleteConfirm = false,
                message = null,
            )
        }
        viewModelScope.launch {
            when (
                val result = deleteAccountUseCase(
                    DeleteAccountRequest(
                        provider = provider,
                    )
                )
            ) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isDeleting = false, message = null) }
                    _effect.send(AccountSettingsEffect.AccountDeleted)
                }

                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }
}

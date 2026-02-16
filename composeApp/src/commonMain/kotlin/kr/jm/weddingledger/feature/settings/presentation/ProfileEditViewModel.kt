package kr.jm.weddingledger.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase

class ProfileEditViewModel(
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditUiState.initial())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEditEffect>()
    val effect: SharedFlow<ProfileEditEffect> = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun onAction(action: ProfileEditAction) {
        when (action) {
            is ProfileEditAction.OnNicknameChange -> _state.update { it.copy(nickname = action.value) }
            is ProfileEditAction.OnSelectRole -> _state.update { it.copy(selectedRole = action.value) }
            ProfileEditAction.OnClickWeddingDate -> Unit // handled in route
            is ProfileEditAction.OnSelectWeddingDate -> _state.update {
                it.copy(
                    weddingDate = action.value,
                    isDateUndecided = false,
                )
            }

            is ProfileEditAction.OnToggleDateUndecided -> _state.update {
                it.copy(
                    isDateUndecided = action.checked,
                    weddingDate = if (action.checked) null else it.weddingDate,
                )
            }

            ProfileEditAction.OnClickSave -> saveProfile()
            ProfileEditAction.OnDismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun loadProfile() {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val result = fetchMyProfileUseCase()) {
                is OnboardingResult.Success -> {
                    val profile = result.data
                    _state.update {
                        it.copy(
                            nickname = profile.nickname.orEmpty(),
                            selectedRole = profile.role,
                            weddingDate = profile.weddingDate,
                            isDateUndecided = profile.isWeddingDateUndecided,
                            isLoading = false,
                            message = null,
                        )
                    }
                }

                is OnboardingResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val current = _state.value
        val nickname = current.nickname.trim()
        val role = current.selectedRole

        if (nickname.isBlank()) {
            _state.update { it.copy(message = "닉네임을 입력해주세요.") }
            return
        }
        if (role == null) {
            _state.update { it.copy(message = "역할을 선택해주세요.") }
            return
        }
        if (!current.isDateUndecided && current.weddingDate == null) {
            _state.update { it.copy(message = "예식일을 선택해주세요.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (
                val result = updateMyProfileUseCase(
                    nickname = nickname,
                    role = role,
                    weddingDate = current.weddingDate,
                    isWeddingDateUndecided = current.isDateUndecided,
                )
            ) {
                is OnboardingResult.Success -> {
                    _state.update { it.copy(isLoading = false, message = "프로필을 저장했습니다.") }
                    _effect.emit(ProfileEditEffect.Saved)
                }

                is OnboardingResult.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }
}

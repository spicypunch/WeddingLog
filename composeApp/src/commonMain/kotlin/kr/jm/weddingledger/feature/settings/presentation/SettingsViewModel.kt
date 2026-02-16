package kr.jm.weddingledger.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.platform.copyText
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.GetOrCreateInviteCodeUseCase

class SettingsViewModel(
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
    private val getOrCreateInviteCodeUseCase: GetOrCreateInviteCodeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            title = "설정",
            coupleName = "프로필을 불러오는 중...",
            email = "",
            inviteCode = "-",
            selectedTheme = WeddingLogThemeStyle.Minimal,
            isLoading = true,
            message = null,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onThemeStyleChanged(style: WeddingLogThemeStyle) {
        _state.update { it.copy(selectedTheme = style) }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.OnClickCopyInviteCode -> copyInviteCode()
            SettingsAction.OnDismissMessage -> _state.update { it.copy(message = null) }
            SettingsAction.OnClickBack,
            SettingsAction.OnClickLogout,
            SettingsAction.OnClickProfileEdit,
            SettingsAction.OnClickNotificationSettings,
            SettingsAction.OnClickAccountSettings,
            SettingsAction.OnClickAppInfo,
            is SettingsAction.OnSelectTheme,
            -> Unit
        }
    }

    private fun refresh() {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val result = fetchMyProfileUseCase()) {
                is OnboardingResult.Success -> {
                    val profile = result.data
                    val inviteCode = resolveInviteCode(profile.inviteCode)
                    _state.update {
                        it.copy(
                            coupleName = profile.toSettingsCoupleName(),
                            email = profile.email.orEmpty(),
                            inviteCode = inviteCode,
                            isLoading = false,
                        )
                    }
                }

                is OnboardingResult.Error -> {
                    _state.update {
                        it.copy(
                            coupleName = "프로필 설정 필요",
                            email = "",
                            inviteCode = "-",
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    private suspend fun resolveInviteCode(profileInviteCode: String?): String {
        val fromProfile = profileInviteCode?.trim()?.takeIf { it.isNotEmpty() }
        if (fromProfile != null) return fromProfile

        return when (val result = getOrCreateInviteCodeUseCase()) {
            is OnboardingResult.Success -> result.data
            is OnboardingResult.Error -> "-"
        }
    }

    private fun copyInviteCode() {
        val code = _state.value.inviteCode.trim()
        if (code.isBlank() || code == "-") {
            _state.update { it.copy(message = "복사할 초대 코드가 없습니다.") }
            return
        }

        val success = copyText(code)
        _state.update {
            it.copy(
                message = if (success) "초대 코드를 복사했습니다." else "복사에 실패했습니다.",
            )
        }
    }
}

private fun MyProfile.toSettingsCoupleName(): String {
    val nickname = nickname?.trim()?.takeIf { it.isNotEmpty() } ?: return "프로필 설정 필요"
    val roleText = role?.label
    return if (roleText.isNullOrBlank()) nickname else "$roleText $nickname"
}

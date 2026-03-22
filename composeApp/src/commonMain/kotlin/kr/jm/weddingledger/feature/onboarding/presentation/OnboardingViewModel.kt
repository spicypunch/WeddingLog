package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.core.network.logSupabaseInfo
import kr.jm.weddingledger.core.network.logSupabaseWarning
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingErrorType
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import kr.jm.weddingledger.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.ConnectPartnerUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateNicknameUseCase

class OnboardingViewModel(
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase,
    private val connectPartnerUseCase: ConnectPartnerUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState.initial())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.OnClickStartOnboarding -> startPath()
            OnboardingAction.OnClickJoinWithSharedCode -> joinPath()
            OnboardingAction.OnClickBackToEntry -> backToEntry()

            is OnboardingAction.OnNicknameChange -> _state.update {
                it.copy(profile = it.profile.copy(nickname = action.value))
            }
            is OnboardingAction.OnSelectRole -> _state.update {
                it.copy(profile = it.profile.copy(selectedRole = action.role))
            }
            OnboardingAction.OnClickWeddingDate -> Unit // handled in Route (dialog)
            is OnboardingAction.OnWeddingDateSelected -> _state.update {
                it.copy(profile = it.profile.copy(weddingDate = action.date, isDateUndecided = false))
            }
            is OnboardingAction.OnToggleDateUndecided -> _state.update {
                it.copy(
                    profile = it.profile.copy(
                        isDateUndecided = action.checked,
                        weddingDate = if (action.checked) null else it.profile.weddingDate,
                    )
                )
            }
            OnboardingAction.OnClickProfileNext -> submitProfile()

            is OnboardingAction.OnJoinNicknameChange -> _state.update {
                it.copy(inviteJoin = it.inviteJoin.copy(nickname = action.value))
            }
            is OnboardingAction.OnPartnerCodeInputChange -> _state.update {
                it.copy(inviteJoin = it.inviteJoin.copy(partnerCodeInput = action.value))
            }
            OnboardingAction.OnClickInviteShare -> {
                _state.update { it.copy(message = "초대 코드 공유 창을 열었습니다.") }
            }
            OnboardingAction.OnClickConnectPartner -> connectPartnerFromJoin()
            OnboardingAction.OnClickSkipPartner -> completeOnboarding()

            OnboardingAction.OnDismissMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun loadInitial() {
        _state.update { it.copy(step = OnboardingStep.Loading, isLoading = true, message = null) }

        viewModelScope.launch {
            when (val result = fetchMyProfileUseCase()) {
                is OnboardingResult.Success -> {
                    val p = result.data
                    val nextStep = decideStep(
                        p.onboardingCompleted,
                        p.nickname,
                        p.role,
                        p.weddingDate,
                        p.isWeddingDateUndecided,
                    )

                    _state.update {
                        it.copy(
                            step = nextStep,
                            isLoading = false,
                            message = null,
                            profile = it.profile.copy(
                                nickname = p.nickname.orEmpty(),
                                selectedRole = p.role,
                                weddingDate = p.weddingDate,
                                isDateUndecided = p.isWeddingDateUndecided,
                            ),
                            inviteJoin = it.inviteJoin.copy(nickname = p.nickname.orEmpty()),
                        )
                    }
                }
                is OnboardingResult.Error -> {
                    if (result.type == OnboardingErrorType.ReauthenticationRequired) {
                        _state.update { it.copy(step = OnboardingStep.Loading, isLoading = true, message = null) }
                        when (signOutUseCase()) {
                            is AuthResult.Success -> Unit
                            is AuthResult.Error -> {
                                _state.update {
                                    it.copy(
                                        step = OnboardingStep.EntryChoice,
                                        isLoading = false,
                                        message = "로그인 상태를 정리하지 못했습니다. 앱을 다시 실행한 뒤 다시 로그인해주세요.",
                                    )
                                }
                            }
                        }
                        return@launch
                    }

                    _state.update {
                        it.copy(
                            step = OnboardingStep.EntryChoice,
                            isLoading = false,
                            message = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun decideStep(
        onboardingCompleted: Boolean,
        _nickname: String?,
        _role: kr.jm.weddingledger.feature.onboarding.domain.model.UserRole?,
        _weddingDate: kotlinx.datetime.LocalDate?,
        _isUndecided: Boolean,
    ): OnboardingStep {
        if (onboardingCompleted) return OnboardingStep.Done
        return OnboardingStep.EntryChoice
    }

    private fun startPath() {
        _state.update {
            val mergedNickname = it.profile.nickname.ifBlank { it.inviteJoin.nickname }
            it.copy(
                step = OnboardingStep.ProfileSetup,
                message = null,
                profile = it.profile.copy(nickname = mergedNickname),
            )
        }
    }

    private fun joinPath() {
        _state.update {
            val mergedNickname = it.inviteJoin.nickname.ifBlank { it.profile.nickname }
            it.copy(
                step = OnboardingStep.InviteJoin,
                message = null,
                inviteJoin = it.inviteJoin.copy(nickname = mergedNickname),
            )
        }
    }

    private fun backToEntry() {
        _state.update {
            it.copy(
                step = OnboardingStep.EntryChoice,
                message = null,
            )
        }
    }

    private fun submitProfile() {
        val profile = _state.value.profile
        val nickname = profile.nickname.trim()
        val role = profile.selectedRole ?: return
        val date = profile.weddingDate
        val undecided = profile.isDateUndecided

        if (nickname.isBlank()) return
        if (!undecided && date == null) return

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val result = updateMyProfileUseCase(nickname, role, date, undecided)) {
                is OnboardingResult.Success -> {
                    _state.update {
                        it.copy(
                            profile = it.profile.copy(nickname = nickname),
                            inviteJoin = it.inviteJoin.copy(nickname = nickname),
                        )
                    }
                    finishOnboarding()
                }
                is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun connectPartnerFromJoin() {
        val nickname = _state.value.inviteJoin.nickname.trim()
        if (nickname.isBlank()) {
            _state.update { it.copy(message = "닉네임을 입력해주세요.") }
            return
        }

        val rawCode = _state.value.inviteJoin.partnerCodeInput
        val code = normalizeInviteCode(rawCode)
        if (code.trim().isBlank()) {
            _state.update { it.copy(message = "초대 코드를 입력해주세요.") }
            return
        }
        if (!isValidInviteCode(code)) {
            _state.update { it.copy(message = "초대 코드는 6자리 숫자로 입력해주세요.") }
            return
        }

        if (code != rawCode) {
            _state.update { it.copy(inviteJoin = it.inviteJoin.copy(partnerCodeInput = code)) }
        }

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val updateNickname = syncNicknameIfNeeded(nickname)) {
                is OnboardingResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = updateNickname.message) }
                    return@launch
                }
                is OnboardingResult.Success -> Unit
            }

            when (val result = connectPartnerUseCase(code)) {
                is OnboardingResult.Success -> {
                    awaitSharedProfileRefresh()
                    finishOnboarding()
                }
                is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private suspend fun syncNicknameIfNeeded(nickname: String): OnboardingResult<Unit> {
        val currentNickname = _state.value.profile.nickname.trim()
        if (currentNickname == nickname) return OnboardingResult.Success(Unit)

        return when (val result = updateNicknameUseCase(nickname)) {
            is OnboardingResult.Success -> {
                _state.update {
                    it.copy(
                        profile = it.profile.copy(nickname = nickname),
                        inviteJoin = it.inviteJoin.copy(nickname = nickname),
                    )
                }
                OnboardingResult.Success(Unit)
            }
            is OnboardingResult.Error -> result
        }
    }

    private fun completeOnboarding() {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            finishOnboarding()
        }
    }

    private suspend fun finishOnboarding() {
        when (val complete = completeOnboardingUseCase()) {
            is OnboardingResult.Success -> _state.update { it.copy(step = OnboardingStep.Done, isLoading = false) }
            is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = complete.message) }
        }
    }

    private suspend fun awaitSharedProfileRefresh() {
        repeat(4) { attempt ->
            when (val result = fetchMyProfileUseCase()) {
                is OnboardingResult.Success -> {
                    val profile = result.data
                    logSupabaseInfo(
                        featureName = FEATURE_NAME,
                        message = "join_refresh attempt=${attempt + 1} " +
                            "weddingId=${profile.weddingId} weddingDate=${profile.weddingDate} " +
                            "undecided=${profile.isWeddingDateUndecided}",
                    )
                    _state.update {
                        it.copy(
                            profile = it.profile.copy(
                                weddingDate = profile.weddingDate,
                                isDateUndecided = profile.isWeddingDateUndecided,
                            )
                        )
                    }
                    if (profile.weddingDate != null || profile.isWeddingDateUndecided) return
                }

                is OnboardingResult.Error -> {
                    logSupabaseWarning(
                        featureName = FEATURE_NAME,
                        message = "join_refresh attempt=${attempt + 1} error=${result.message}",
                    )
                }
            }

            delay(350)
        }
    }
}

private val inviteCodeRegex = Regex("^\\d{6}$")
private const val FEATURE_NAME: String = "Onboarding"

private fun normalizeInviteCode(value: String): String {
    return value.trim().filter { it in '0'..'9' }
}

private fun isValidInviteCode(value: String): Boolean = inviteCodeRegex.matches(value)

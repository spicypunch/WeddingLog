package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import kr.jm.weddingledger.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.ConnectPartnerUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.GetOrCreateInviteCodeUseCase
import kr.jm.weddingledger.feature.onboarding.domain.usecase.UpdateMyProfileUseCase

class OnboardingViewModel(
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase,
    private val getOrCreateInviteCodeUseCase: GetOrCreateInviteCodeUseCase,
    private val connectPartnerUseCase: ConnectPartnerUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState.initial())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
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

            is OnboardingAction.OnPartnerCodeInputChange -> _state.update {
                it.copy(partner = it.partner.copy(partnerCodeInput = action.value))
            }
            OnboardingAction.OnClickInviteShare -> {
                _state.update { it.copy(message = "초대 코드 공유 창을 열었습니다.") }
            }
            OnboardingAction.OnClickConnectPartner -> connectPartner()
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
                        onboardingCompleted = p.onboardingCompleted,
                        nickname = p.nickname,
                        role = p.role,
                        weddingDate = p.weddingDate,
                        isUndecided = p.isWeddingDateUndecided,
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
                        )
                    }

                    if (nextStep == OnboardingStep.PartnerConnect) {
                        loadInviteCode()
                    }
                }
                is OnboardingResult.Error -> {
                    _state.update {
                        it.copy(
                            step = OnboardingStep.ProfileSetup,
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
        nickname: String?,
        role: kr.jm.weddingledger.feature.onboarding.domain.model.UserRole?,
        weddingDate: kotlinx.datetime.LocalDate?,
        isUndecided: Boolean,
    ): OnboardingStep {
        if (onboardingCompleted) return OnboardingStep.Done

        val isProfileDone = !nickname.isNullOrBlank() && role != null && (isUndecided || weddingDate != null)
        return if (isProfileDone) OnboardingStep.PartnerConnect else OnboardingStep.ProfileSetup
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
                    _state.update { it.copy(step = OnboardingStep.PartnerConnect, isLoading = false) }
                    loadInviteCode()
                }
                is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun loadInviteCode() {
        viewModelScope.launch {
            when (val result = getOrCreateInviteCodeUseCase()) {
                is OnboardingResult.Success -> _state.update { it.copy(partner = it.partner.copy(inviteCode = result.data)) }
                is OnboardingResult.Error -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    private fun connectPartner() {
        val rawCode = _state.value.partner.partnerCodeInput
        val code = normalizeInviteCode(rawCode)
        if (code.trim().isBlank()) return
        if (!isValidInviteCode(code)) {
            _state.update { it.copy(message = "초대 코드는 W-1234 형식으로 입력해주세요.") }
            return
        }

        if (code != rawCode) {
            _state.update { it.copy(partner = it.partner.copy(partnerCodeInput = code)) }
        }

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val result = connectPartnerUseCase(code)) {
                is OnboardingResult.Success -> {
                    when (val complete = completeOnboardingUseCase()) {
                        is OnboardingResult.Success -> _state.update { it.copy(step = OnboardingStep.Done, isLoading = false) }
                        is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = complete.message) }
                    }
                }
                is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun completeOnboarding() {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            when (val complete = completeOnboardingUseCase()) {
                is OnboardingResult.Success -> _state.update { it.copy(step = OnboardingStep.Done, isLoading = false) }
                is OnboardingResult.Error -> _state.update { it.copy(isLoading = false, message = complete.message) }
            }
        }
    }
}

private val inviteCodeRegex = Regex("^W-\\d{4}$")

private fun normalizeInviteCode(value: String): String {
    val compact = value.trim().uppercase().replace(" ", "")
    val alnumAndDash = compact.filter { it.isLetterOrDigit() || it == '-' }
    if (Regex("^W\\d{4}$").matches(alnumAndDash)) {
        return "W-${alnumAndDash.drop(1)}"
    }
    return alnumAndDash
}

private fun isValidInviteCode(value: String): Boolean = inviteCodeRegex.matches(value)

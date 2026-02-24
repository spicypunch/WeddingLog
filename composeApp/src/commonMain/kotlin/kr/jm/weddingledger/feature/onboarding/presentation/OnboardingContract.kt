package kr.jm.weddingledger.feature.onboarding.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

data class OnboardingUiState(
    val step: OnboardingStep,
    val isLoading: Boolean,
    val message: String?,
    val profile: ProfileSetupUiState,
    val inviteShare: InviteShareUiState,
    val inviteJoin: InviteJoinUiState,
) {
    companion object {
        fun initial(): OnboardingUiState = OnboardingUiState(
            step = OnboardingStep.Loading,
            isLoading = true,
            message = null,
            profile = ProfileSetupUiState.initial(),
            inviteShare = InviteShareUiState.initial(),
            inviteJoin = InviteJoinUiState.initial(),
        )
    }
}

data class ProfileSetupUiState(
    val nickname: String,
    val selectedRole: UserRole?,
    val weddingDate: LocalDate?,
    val isDateUndecided: Boolean,
) {
    val weddingDateText: String
        get() = when {
            isDateUndecided -> "아직 정해지지 않았어요"
            weddingDate != null -> "${weddingDate.year}.${weddingDate.month.number.toString().padStart(2, '0')}.${weddingDate.day.toString().padStart(2, '0')}"
            else -> "날짜를 선택해주세요"
        }

    val isNextEnabled: Boolean
        get() = nickname.trim().isNotEmpty() &&
            selectedRole != null &&
            (isDateUndecided || weddingDate != null)

    companion object {
        fun initial(): ProfileSetupUiState = ProfileSetupUiState(
            nickname = "",
            selectedRole = null,
            weddingDate = null,
            isDateUndecided = false,
        )
    }
}

data class InviteShareUiState(
    val inviteCode: String,
) {
    companion object {
        fun initial(): InviteShareUiState = InviteShareUiState(
            inviteCode = "",
        )
    }
}

data class InviteJoinUiState(
    val nickname: String,
    val partnerCodeInput: String,
) {
    val isConnectEnabled: Boolean
        get() = nickname.trim().isNotEmpty() && partnerCodeInput.trim().isNotEmpty()

    companion object {
        fun initial(): InviteJoinUiState = InviteJoinUiState(
            nickname = "",
            partnerCodeInput = "",
        )
    }
}

sealed interface OnboardingAction {
    data object OnClickStartOnboarding : OnboardingAction
    data object OnClickJoinWithSharedCode : OnboardingAction
    data object OnClickBackToEntry : OnboardingAction

    data class OnNicknameChange(val value: String) : OnboardingAction
    data class OnSelectRole(val role: UserRole) : OnboardingAction
    data object OnClickWeddingDate : OnboardingAction
    data class OnWeddingDateSelected(val date: LocalDate) : OnboardingAction
    data class OnToggleDateUndecided(val checked: Boolean) : OnboardingAction
    data object OnClickProfileNext : OnboardingAction

    data class OnJoinNicknameChange(val value: String) : OnboardingAction
    data class OnPartnerCodeInputChange(val value: String) : OnboardingAction
    data object OnClickInviteShare : OnboardingAction
    data object OnClickConnectPartner : OnboardingAction
    data object OnClickSkipPartner : OnboardingAction

    data object OnDismissMessage : OnboardingAction
}

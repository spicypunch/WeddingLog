package kr.jm.weddingledger.feature.settings.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

data class ProfileEditUiState(
    val title: String,
    val nickname: String,
    val selectedRole: UserRole?,
    val weddingDate: LocalDate?,
    val isDateUndecided: Boolean,
    val isLoading: Boolean,
    val message: String?,
) {
    val weddingDateText: String
        get() = when {
            isDateUndecided -> "아직 정해지지 않았어요"
            weddingDate != null -> "${weddingDate.year}.${weddingDate.month.number.toString().padStart(2, '0')}.${weddingDate.day.toString().padStart(2, '0')}"
            else -> "예식일을 선택해주세요"
        }

    val canSave: Boolean
        get() = nickname.trim().isNotEmpty() &&
            selectedRole != null &&
            (isDateUndecided || weddingDate != null) &&
            !isLoading

    companion object {
        fun initial(): ProfileEditUiState = ProfileEditUiState(
            title = "프로필 수정",
            nickname = "",
            selectedRole = null,
            weddingDate = null,
            isDateUndecided = false,
            isLoading = true,
            message = null,
        )
    }
}

sealed interface ProfileEditAction {
    data class OnNicknameChange(val value: String) : ProfileEditAction
    data class OnSelectRole(val value: UserRole) : ProfileEditAction
    data object OnClickWeddingDate : ProfileEditAction
    data class OnSelectWeddingDate(val value: LocalDate) : ProfileEditAction
    data class OnToggleDateUndecided(val checked: Boolean) : ProfileEditAction
    data object OnClickSave : ProfileEditAction
    data object OnDismissMessage : ProfileEditAction
}

sealed interface ProfileEditEffect {
    data object Saved : ProfileEditEffect
}

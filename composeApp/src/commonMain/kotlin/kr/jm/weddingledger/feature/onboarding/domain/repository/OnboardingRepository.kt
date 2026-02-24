package kr.jm.weddingledger.feature.onboarding.domain.repository

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

interface OnboardingRepository {
    suspend fun fetchMyProfile(): OnboardingResult<MyProfile>

    suspend fun updateNickname(
        nickname: String,
    ): OnboardingResult<Unit>

    suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit>

    suspend fun getOrCreateInviteCode(): OnboardingResult<String>

    suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit>

    suspend fun completeOnboarding(): OnboardingResult<Unit>
}

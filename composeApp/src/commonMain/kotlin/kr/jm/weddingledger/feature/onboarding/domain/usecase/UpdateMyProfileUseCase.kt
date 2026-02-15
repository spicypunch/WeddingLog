package kr.jm.weddingledger.feature.onboarding.domain.usecase

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class UpdateMyProfileUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> = repository.updateMyProfile(
        nickname = nickname,
        role = role,
        weddingDate = weddingDate,
        isWeddingDateUndecided = isWeddingDateUndecided,
    )
}


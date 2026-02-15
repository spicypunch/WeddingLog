package kr.jm.weddingledger.feature.onboarding.domain.usecase

import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class FetchMyProfileUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke(): OnboardingResult<MyProfile> = repository.fetchMyProfile()
}


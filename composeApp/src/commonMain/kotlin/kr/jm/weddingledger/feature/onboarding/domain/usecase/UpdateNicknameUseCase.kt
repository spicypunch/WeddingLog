package kr.jm.weddingledger.feature.onboarding.domain.usecase

import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class UpdateNicknameUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke(
        nickname: String,
    ): OnboardingResult<Unit> = repository.updateNickname(nickname)
}

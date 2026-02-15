package kr.jm.weddingledger.feature.onboarding.domain.usecase

import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class ConnectPartnerUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke(inviteCode: String): OnboardingResult<Unit> =
        repository.connectPartner(inviteCode)
}


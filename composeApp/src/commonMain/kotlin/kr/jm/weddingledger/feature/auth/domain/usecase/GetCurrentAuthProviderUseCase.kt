package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthProvider
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class GetCurrentAuthProviderUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): AuthProvider? = authRepository.getCurrentAuthProvider()
}

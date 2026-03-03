package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class SignInWithKakaoUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AuthResult<Unit> {
        return authRepository.signInWithKakao()
    }
}

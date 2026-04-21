package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult<Unit> {
        return authRepository.signIn(email, password)
    }
}

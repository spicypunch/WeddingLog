package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.DeleteAccountRequest
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(request: DeleteAccountRequest): AuthResult<Unit> {
        return authRepository.deleteAccount(request)
    }
}

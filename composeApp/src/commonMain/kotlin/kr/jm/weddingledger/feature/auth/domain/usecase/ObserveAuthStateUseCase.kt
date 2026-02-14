package kr.jm.weddingledger.feature.auth.domain.usecase

import kotlinx.coroutines.flow.Flow
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = authRepository.authState
}


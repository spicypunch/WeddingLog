package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class HandleAuthDeepLinkUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(url: String): AuthResult<Unit> {
        if (url.isBlank()) return AuthResult.Error("인증 링크가 비어 있습니다.")
        return authRepository.handleAuthDeepLink(url)
    }
}

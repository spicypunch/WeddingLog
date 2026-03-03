package kr.jm.weddingledger.feature.auth.domain.usecase

import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class VerifyEmailOtpUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, token: String): AuthResult<Unit> {
        if (email.isBlank()) {
            return AuthResult.Error("이메일을 입력해주세요")
        }
        if (!email.contains("@")) {
            return AuthResult.Error("올바른 이메일 형식이 아닙니다")
        }

        val normalizedToken = token.trim()
        if (!normalizedToken.matches(Regex("^\\d{6}$"))) {
            return AuthResult.Error("인증 코드는 6자리 숫자로 입력해주세요")
        }

        return authRepository.verifyEmailOtp(
            email = email,
            token = normalizedToken,
        )
    }
}

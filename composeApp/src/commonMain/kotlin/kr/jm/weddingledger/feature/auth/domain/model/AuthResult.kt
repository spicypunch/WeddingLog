package kr.jm.weddingledger.feature.auth.domain.model

enum class AuthErrorType {
    EmailVerificationRequired,
    Unknown,
}

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(
        val message: String,
        val type: AuthErrorType = AuthErrorType.Unknown,
    ) : AuthResult<Nothing>()
}

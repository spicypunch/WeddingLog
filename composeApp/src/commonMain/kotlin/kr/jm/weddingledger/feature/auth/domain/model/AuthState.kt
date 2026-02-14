package kr.jm.weddingledger.feature.auth.domain.model

sealed interface AuthState {
    data object Loading : AuthState

    data class Unauthenticated(
        val reason: String? = null,
    ) : AuthState

    data class Authenticated(
        val user: AuthUser,
    ) : AuthState
}


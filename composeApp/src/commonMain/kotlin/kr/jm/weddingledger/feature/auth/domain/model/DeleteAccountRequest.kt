package kr.jm.weddingledger.feature.auth.domain.model

data class DeleteAccountRequest(
    val provider: AuthProvider,
    val appleAuthorizationCode: String? = null,
)

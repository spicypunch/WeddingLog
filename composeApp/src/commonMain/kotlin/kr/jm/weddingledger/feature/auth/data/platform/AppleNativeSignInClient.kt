package kr.jm.weddingledger.feature.auth.data.platform

interface AppleNativeSignInClient {
    suspend fun signIn(): AppleNativeSignInResult
}

sealed interface AppleNativeSignInResult {
    data class Success(
        val idToken: String,
        val rawNonce: String,
        val authorizationCode: String? = null,
    ) : AppleNativeSignInResult

    data class Unsupported(
        val reason: String,
    ) : AppleNativeSignInResult

    data class Error(
        val message: String,
    ) : AppleNativeSignInResult
}

expect fun createAppleNativeSignInClient(): AppleNativeSignInClient

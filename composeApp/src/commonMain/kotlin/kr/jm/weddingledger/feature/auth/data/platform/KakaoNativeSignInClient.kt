package kr.jm.weddingledger.feature.auth.data.platform

interface KakaoNativeSignInClient {
    suspend fun signIn(): KakaoNativeSignInResult
}

sealed interface KakaoNativeSignInResult {
    data class Success(
        val idToken: String,
        val accessToken: String?,
    ) : KakaoNativeSignInResult

    data class Unsupported(
        val reason: String,
    ) : KakaoNativeSignInResult

    data class Error(
        val message: String,
    ) : KakaoNativeSignInResult
}

expect fun createKakaoNativeSignInClient(): KakaoNativeSignInClient

package kr.jm.weddingledger.feature.auth.data.platform

actual fun createAppleNativeSignInClient(): AppleNativeSignInClient = AndroidAppleNativeSignInClient

private object AndroidAppleNativeSignInClient : AppleNativeSignInClient {
    override suspend fun signIn(): AppleNativeSignInResult {
        return AppleNativeSignInResult.Unsupported("Apple 로그인은 iPhone에서만 지원합니다.")
    }
}

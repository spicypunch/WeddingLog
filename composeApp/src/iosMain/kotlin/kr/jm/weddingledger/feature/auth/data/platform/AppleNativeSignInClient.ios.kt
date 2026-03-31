package kr.jm.weddingledger.feature.auth.data.platform

import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSNotificationCenter

actual fun createAppleNativeSignInClient(): AppleNativeSignInClient = IosAppleNativeSignInClient

private object IosAppleNativeSignInClient : AppleNativeSignInClient {
    override suspend fun signIn(): AppleNativeSignInResult {
        val deferred = beginIosAppleNativeSignIn()
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = IOS_APPLE_NATIVE_SIGN_IN_REQUEST_NOTIFICATION,
            `object` = null,
        )
        return withTimeoutOrNull(timeMillis = 90_000L) {
            deferred.await()
        } ?: AppleNativeSignInResult.Error("Apple 로그인 시간이 초과되었습니다. 다시 시도해주세요.")
    }
}

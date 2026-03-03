package kr.jm.weddingledger.feature.auth.data.platform

import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSNotificationCenter

actual fun createKakaoNativeSignInClient(): KakaoNativeSignInClient = IosKakaoNativeSignInClient

private object IosKakaoNativeSignInClient : KakaoNativeSignInClient {
    override suspend fun signIn(): KakaoNativeSignInResult {
        val deferred = beginIosKakaoNativeSignIn()
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = IOS_KAKAO_NATIVE_SIGN_IN_REQUEST_NOTIFICATION,
            `object` = null,
        )
        return withTimeoutOrNull(timeMillis = 90_000L) {
            deferred.await()
        } ?: KakaoNativeSignInResult.Error("카카오 로그인 시간이 초과되었습니다. 다시 시도해주세요.")
    }
}

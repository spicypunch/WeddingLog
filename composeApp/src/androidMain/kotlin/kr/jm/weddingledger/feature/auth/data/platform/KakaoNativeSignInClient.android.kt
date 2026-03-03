package kr.jm.weddingledger.feature.auth.data.platform

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual fun createKakaoNativeSignInClient(): KakaoNativeSignInClient = AndroidKakaoNativeSignInClient

private object AndroidKakaoNativeSignInClient : KakaoNativeSignInClient {
    override suspend fun signIn(): KakaoNativeSignInResult = suspendCancellableCoroutine { continuation ->
        if (!KakaoSdk.isInitialized) {
            continuation.resume(
                KakaoNativeSignInResult.Error(
                    message = "카카오 SDK 설정이 필요합니다. KAKAO_NATIVE_APP_KEY를 확인해주세요."
                )
            )
            return@suspendCancellableCoroutine
        }

        val activity = AndroidAuthActivityHolder.get()
        if (activity == null) {
            continuation.resume(
                KakaoNativeSignInResult.Error(
                    message = "카카오 로그인을 시작할 수 없습니다. 화면을 다시 열고 시도해주세요."
                )
            )
            return@suspendCancellableCoroutine
        }

        fun finish(result: KakaoNativeSignInResult) {
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        fun finishByToken(token: OAuthToken?) {
            val idToken = token?.idToken?.trim()
            if (!idToken.isNullOrEmpty()) {
                finish(
                    KakaoNativeSignInResult.Success(
                        idToken = idToken,
                        accessToken = token.accessToken,
                    )
                )
                return
            }

            UserApiClient.instance.loginWithNewScopes(
                context = activity,
                scopes = listOf("openid"),
            ) { refreshedToken, refreshedError ->
                when {
                    refreshedError != null -> {
                        finish(
                            KakaoNativeSignInResult.Error(
                                message = "카카오 ID 토큰을 가져오지 못했습니다. Kakao OpenID Connect 설정을 확인해주세요."
                            )
                        )
                    }

                    refreshedToken?.idToken.isNullOrBlank() -> {
                        finish(
                            KakaoNativeSignInResult.Error(
                                message = "카카오 ID 토큰이 비어 있습니다. Kakao OpenID Connect 설정을 확인해주세요."
                            )
                        )
                    }

                    else -> {
                        val refreshed = refreshedToken
                        finish(
                            KakaoNativeSignInResult.Success(
                                idToken = refreshed.idToken.orEmpty(),
                                accessToken = refreshed.accessToken,
                            )
                        )
                    }
                }
            }
        }

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> finish(KakaoNativeSignInResult.Error(message = error.toKakaoMessage()))
                token == null -> finish(KakaoNativeSignInResult.Error(message = "카카오 로그인 응답이 비어 있습니다."))
                else -> finishByToken(token)
            }
        }

        runCatching {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
                UserApiClient.instance.loginWithKakaoTalk(
                    context = activity,
                    callback = callback,
                )
            } else {
                UserApiClient.instance.loginWithKakaoAccount(
                    context = activity,
                    callback = callback,
                )
            }
        }.onFailure { throwable ->
            finish(KakaoNativeSignInResult.Error(message = throwable.toKakaoMessage()))
        }
    }
}

private fun Throwable.toKakaoMessage(): String {
    return when (this) {
        is ClientError -> {
            when (reason) {
                ClientErrorCause.Cancelled -> "카카오 로그인을 취소했습니다."
                else -> "카카오 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
            }
        }

        else -> "카카오 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
    }
}

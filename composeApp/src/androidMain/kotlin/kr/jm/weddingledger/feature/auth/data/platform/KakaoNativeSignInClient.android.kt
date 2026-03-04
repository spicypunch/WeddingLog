package kr.jm.weddingledger.feature.auth.data.platform

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ApiError
import com.kakao.sdk.common.model.AuthError
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
                                message = refreshedError.toKakaoMessage()
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

        fun loginWithKakaoAccount() {
            UserApiClient.instance.loginWithKakaoAccount(
                context = activity,
                callback = callback,
            )
        }

        runCatching {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
                UserApiClient.instance.loginWithKakaoTalk(
                    context = activity,
                    callback = talkCallback@{ token, error ->
                        when {
                            error != null -> {
                                val isCancelled =
                                    error is ClientError && error.reason == ClientErrorCause.Cancelled
                                if (isCancelled) {
                                    finish(KakaoNativeSignInResult.Error(message = error.toKakaoMessage()))
                                    return@talkCallback
                                }
                                // 카카오톡 앱 로그인 실패 시 계정 로그인으로 1회 폴백합니다.
                                loginWithKakaoAccount()
                            }

                            token == null -> {
                                finish(KakaoNativeSignInResult.Error(message = "카카오 로그인 응답이 비어 있습니다."))
                            }

                            else -> finishByToken(token)
                        }
                    },
                )
            } else {
                loginWithKakaoAccount()
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
                ClientErrorCause.NotSupported -> "카카오톡 로그인을 지원하지 않는 환경입니다. 카카오 계정 로그인으로 다시 시도해주세요."
                ClientErrorCause.BadParameter -> "카카오 로그인 설정값이 올바르지 않습니다. 앱 키/패키지명/키 해시를 확인해주세요."
                ClientErrorCause.IllegalState -> "카카오 로그인 상태가 올바르지 않습니다. 앱을 다시 열고 시도해주세요."
                else -> this.msg?.takeIf { it.isNotBlank() }
                    ?: "카카오 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
            }
        }

        is AuthError -> {
            val reasonText = reason.name
            val detail = response.errorDescription?.takeIf { it.isNotBlank() }
            when (reason) {
                com.kakao.sdk.common.model.AuthErrorCause.AccessDenied -> "카카오 로그인을 취소했습니다."
                com.kakao.sdk.common.model.AuthErrorCause.InvalidClient,
                com.kakao.sdk.common.model.AuthErrorCause.Misconfigured -> {
                    if (detail != null) {
                        "카카오 앱 설정이 올바르지 않습니다. (reason=$reasonText, $detail, status=$statusCode)"
                    } else {
                        "카카오 앱 설정이 올바르지 않습니다. (reason=$reasonText, status=$statusCode)"
                    }
                }
                else -> {
                    if (detail != null) {
                        "카카오 인증에 실패했습니다. (reason=$reasonText, $detail, status=$statusCode)"
                    } else {
                        "카카오 인증에 실패했습니다. (reason=$reasonText, status=$statusCode)"
                    }
                }
            }
        }

        is ApiError -> {
            val detail = response.msg.takeIf { it.isNotBlank() }
            if (detail != null) {
                "카카오 API 호출에 실패했습니다. (reason=${reason.name}, $detail, status=$statusCode)"
            } else {
                "카카오 API 호출에 실패했습니다. (reason=${reason.name}, status=$statusCode)"
            }
        }

        else -> message?.takeIf { it.isNotBlank() }
            ?: "카카오 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
    }
}

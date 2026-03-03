package kr.jm.weddingledger.feature.auth.data.platform

import kotlinx.coroutines.CompletableDeferred

internal const val IOS_KAKAO_NATIVE_SIGN_IN_REQUEST_NOTIFICATION: String =
    "WeddingLedgerKakaoNativeSignInRequest"

private var pendingIosKakaoNativeSignIn: CompletableDeferred<KakaoNativeSignInResult>? = null

internal fun beginIosKakaoNativeSignIn(): CompletableDeferred<KakaoNativeSignInResult> {
    pendingIosKakaoNativeSignIn
        ?.takeIf { it.isActive }
        ?.complete(KakaoNativeSignInResult.Error("이전 카카오 로그인 요청이 취소되었습니다. 다시 시도해주세요."))

    return CompletableDeferred<KakaoNativeSignInResult>().also {
        pendingIosKakaoNativeSignIn = it
    }
}

fun completeIosKakaoNativeSignIn(
    idToken: String?,
    accessToken: String?,
    errorMessage: String?,
) {
    when {
        !errorMessage.isNullOrBlank() -> {
            resolveIosKakaoNativeSignIn(KakaoNativeSignInResult.Error(errorMessage))
        }

        !idToken.isNullOrBlank() -> {
            resolveIosKakaoNativeSignIn(
                KakaoNativeSignInResult.Success(
                    idToken = idToken.trim(),
                    accessToken = accessToken,
                )
            )
        }

        else -> {
            resolveIosKakaoNativeSignIn(
                KakaoNativeSignInResult.Error("카카오 로그인 응답이 올바르지 않습니다.")
            )
        }
    }
}

private fun resolveIosKakaoNativeSignIn(result: KakaoNativeSignInResult) {
    val pending = pendingIosKakaoNativeSignIn
    pendingIosKakaoNativeSignIn = null
    pending?.takeIf { it.isActive }?.complete(result)
}

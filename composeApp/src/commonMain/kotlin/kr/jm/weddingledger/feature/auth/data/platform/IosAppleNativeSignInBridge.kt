package kr.jm.weddingledger.feature.auth.data.platform

import kotlinx.coroutines.CompletableDeferred

internal const val IOS_APPLE_NATIVE_SIGN_IN_REQUEST_NOTIFICATION: String =
    "WeddingLedgerAppleNativeSignInRequest"

private var pendingIosAppleNativeSignIn: CompletableDeferred<AppleNativeSignInResult>? = null

internal fun beginIosAppleNativeSignIn(): CompletableDeferred<AppleNativeSignInResult> {
    pendingIosAppleNativeSignIn
        ?.takeIf { it.isActive }
        ?.complete(AppleNativeSignInResult.Error("이전 Apple 로그인 요청이 취소되었습니다. 다시 시도해주세요."))

    return CompletableDeferred<AppleNativeSignInResult>().also {
        pendingIosAppleNativeSignIn = it
    }
}

fun completeIosAppleNativeSignIn(
    idToken: String?,
    rawNonce: String?,
    authorizationCode: String?,
    errorMessage: String?,
) {
    when {
        !errorMessage.isNullOrBlank() -> {
            resolveIosAppleNativeSignIn(AppleNativeSignInResult.Error(errorMessage))
        }

        !idToken.isNullOrBlank() && !rawNonce.isNullOrBlank() -> {
            resolveIosAppleNativeSignIn(
                AppleNativeSignInResult.Success(
                    idToken = idToken.trim(),
                    rawNonce = rawNonce.trim(),
                    authorizationCode = authorizationCode?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }

        else -> {
            resolveIosAppleNativeSignIn(
                AppleNativeSignInResult.Error("Apple 로그인 응답이 올바르지 않습니다.")
            )
        }
    }
}

private fun resolveIosAppleNativeSignIn(result: AppleNativeSignInResult) {
    val pending = pendingIosAppleNativeSignIn
    pendingIosAppleNativeSignIn = null
    pending?.takeIf { it.isActive }?.complete(result)
}

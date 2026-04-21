package kr.jm.weddingledger.feature.auth.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.network.logSupabaseError
import kr.jm.weddingledger.feature.auth.data.dto.DeleteAccountRequestDto
import kr.jm.weddingledger.feature.auth.data.platform.KakaoNativeSignInClient
import kr.jm.weddingledger.feature.auth.data.platform.KakaoNativeSignInResult
import kr.jm.weddingledger.feature.auth.data.platform.AppleNativeSignInClient
import kr.jm.weddingledger.feature.auth.data.platform.AppleNativeSignInResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthErrorType
import kr.jm.weddingledger.feature.auth.domain.model.AuthProvider
import kr.jm.weddingledger.feature.auth.domain.model.DeleteAccountRequest
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.AuthUser
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val kakaoNativeSignInClient: KakaoNativeSignInClient,
    private val appleNativeSignInClient: AppleNativeSignInClient,
    private val httpClient: HttpClient,
) : AuthRepository {

    override val authState: Flow<AuthState> = supabaseClient.auth.sessionStatus
        .map { status ->
            when (status) {
                SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user == null) {
                        AuthState.Unauthenticated(reason = "로그인 정보를 불러오지 못했습니다. 다시 로그인해주세요.")
                    } else {
                        AuthState.Authenticated(
                            AuthUser(
                                id = user.id,
                                email = user.email ?: "",
                                createdAt = user.createdAt?.toString()
                            )
                        )
                    }
                }
                is SessionStatus.RefreshFailure -> {
                    AuthState.Unauthenticated(reason = "세션을 갱신하지 못했습니다. 다시 로그인해주세요.")
                }
                is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated(
                    reason = if (status.isSignOut) null else null
                )
            }
        }
        .distinctUntilChanged()
        .catch { t ->
            // Kotlin/Native에서 unhandled Flow 예외는 앱 전체 크래시로 이어질 수 있어 방어합니다.
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            emit(AuthState.Unauthenticated(reason = "로그인 상태를 불러오지 못했습니다. 네트워크 상태를 확인하고 다시 시도해주세요."))
        }

    override suspend fun handleAuthDeepLink(url: String): AuthResult<Unit> {
        return try {
            val normalized = url.trim()
            val queryParams = normalized.parseQueryParams()
            val errorMessage = queryParams["error_description"]?.takeIf { it.isNotBlank() }
                ?: queryParams["error"]?.takeIf { it.isNotBlank() }

            if (errorMessage != null) {
                return AuthResult.Error("인증 링크 처리 실패: $errorMessage")
            }

            val code = queryParams["code"]?.trim()
            val tokenHash = queryParams["token_hash"]?.trim()
            val otpType = queryParams["type"].toEmailOtpTypeOrNull()

            when {
                !code.isNullOrBlank() -> {
                    supabaseClient.auth.exchangeCodeForSession(code)
                }

                normalized.contains("access_token=") && normalized.contains("refresh_token=") -> {
                    val session = supabaseClient.auth.parseSessionFromUrl(normalized)
                    supabaseClient.auth.importSession(session)
                }

                !tokenHash.isNullOrBlank() && otpType != null -> {
                    supabaseClient.auth.verifyEmailOtp(
                        type = otpType,
                        tokenHash = tokenHash,
                    )
                }

                else -> {
                    return AuthResult.Error("지원하지 않는 인증 링크 형식입니다.")
                }
            }

            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "인증 링크 처리에 실패했습니다.")
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        return AuthResult.Error("비밀번호 회원가입은 현재 비활성화되어 있습니다.")
    }

    override suspend fun signInWithGoogle(): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(Google)
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "Google 로그인에 실패했습니다")
        }
    }

    override suspend fun signInWithKakao(): AuthResult<Unit> {
        val nativeResult = kakaoNativeSignInClient.signIn()
        when (nativeResult) {
            is KakaoNativeSignInResult.Success -> {
                return signInWithKakaoIdToken(
                    idToken = nativeResult.idToken,
                    accessToken = nativeResult.accessToken,
                )
            }

            is KakaoNativeSignInResult.Unsupported -> {
                return AuthResult.Error(nativeResult.reason)
            }

            is KakaoNativeSignInResult.Error -> {
                return AuthResult.Error(nativeResult.message)
            }
        }
    }

    private suspend fun signInWithKakaoIdToken(
        idToken: String,
        accessToken: String?,
    ): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Kakao
                this.accessToken = accessToken
            }
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            val message = when (t) {
                is AuthRestException -> t.toKakaoSignInMessage()
                is RestException -> t.toKakaoSignInMessage()
                else -> t.toAuthError(defaultMessage = "Kakao 로그인에 실패했습니다").message
            }
            AuthResult.Error(message)
        }
    }

    override suspend fun signInWithApple(): AuthResult<Unit> {
        return when (val nativeResult = appleNativeSignInClient.signIn()) {
            is AppleNativeSignInResult.Success -> {
                signInWithAppleIdToken(
                    idToken = nativeResult.idToken,
                    rawNonce = nativeResult.rawNonce,
                )
            }

            is AppleNativeSignInResult.Unsupported -> {
                AuthResult.Error(nativeResult.reason)
            }

            is AppleNativeSignInResult.Error -> {
                AuthResult.Error(nativeResult.message)
            }
        }
    }

    private suspend fun signInWithAppleIdToken(
        idToken: String,
        rawNonce: String,
    ): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Apple
                nonce = rawNonce
            }
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAppleSignInError()
        }
    }

    override suspend fun sendEmailOtp(email: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(OTP) {
                this.email = email
                createUser = false
            }
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "OTP 전송에 실패했습니다")
        }
    }

    override suspend fun verifyEmailOtp(email: String, token: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = email,
                token = token,
            )
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "OTP 인증에 실패했습니다")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "이메일 로그인에 실패했습니다")
        }
    }

    override suspend fun resendSignUpVerificationEmail(email: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.resendEmail(
                type = OtpType.Email.SIGNUP,
                email = email,
            )
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "인증 메일 재전송에 실패했습니다")
        }
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signOut()
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "로그아웃에 실패했습니다")
        }
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "비밀번호 재설정 이메일 전송에 실패했습니다")
        }
    }

    override suspend fun deleteAccount(request: DeleteAccountRequest): AuthResult<Unit> {
        val provider = request.provider.takeUnless { it == AuthProvider.Unknown }
            ?: getCurrentAuthProvider()
            ?: AuthProvider.Unknown
        val appleAuthorizationCode = when (provider) {
            AuthProvider.Apple -> request.appleAuthorizationCode
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: obtainAppleAuthorizationCodeForDeletion()
            else -> null
        }

        return try {
            suspend fun executeDeleteRequest(accessToken: String) = httpClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/delete-account") {
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $accessToken")
                setBody(
                    DeleteAccountRequestDto(
                        provider = provider.name.lowercase(),
                        appleAuthorizationCode = appleAuthorizationCode,
                    )
                )
            }

            suspend fun resolveCurrentAccessToken(): String {
                return supabaseClient.auth.currentAccessTokenOrNull()
                    ?.trim()
                    .takeUnless { it.isNullOrEmpty() }
                    ?: throw IllegalStateException("로그인이 만료되었습니다. 다시 로그인 후 시도해주세요.")
            }

            var accessToken = resolveCurrentAccessToken()
            var response = executeDeleteRequest(accessToken)
            var body = response.bodyAsText()

            if (response.status == HttpStatusCode.Unauthorized && body.contains("Invalid JWT", ignoreCase = true)) {
                logSupabaseError(
                    featureName = AUTH_LOG_FEATURE_NAME,
                    message = "delete_account_invalid_jwt retrying_with_refresh",
                )
                supabaseClient.auth.refreshCurrentSession()
                accessToken = resolveCurrentAccessToken()
                response = executeDeleteRequest(accessToken)
                body = response.bodyAsText()
            }

            if (response.status == HttpStatusCode.OK) {
                runCatching {
                    supabaseClient.auth.signOut(scope = SignOutScope.LOCAL)
                }.onFailure { t ->
                    if (t is CancellationException) throw t
                    logSupabaseThrowable(t)
                }
                AuthResult.Success(Unit)
            } else {
                logSupabaseError(
                    featureName = AUTH_LOG_FEATURE_NAME,
                    message = "delete_account_failed status=${response.status.value} body=${body.take(200)}",
                )
                AuthResult.Error(
                    parseDeleteAccountErrorMessage(body)
                        ?: "회원탈퇴에 실패했습니다. 잠시 후 다시 시도해주세요."
                )
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            t.toAuthError(defaultMessage = "회원탈퇴에 실패했습니다.")
        }
    }

    override fun getCurrentAuthProvider(): AuthProvider? {
        val user = supabaseClient.auth.currentUserOrNull() ?: return null
        return user.toDomainAuthProvider()
    }

    override fun isLoggedIn(): Boolean {
        return supabaseClient.auth.currentUserOrNull() != null
    }

    private suspend fun obtainAppleAuthorizationCodeForDeletion(): String {
        return when (val result = appleNativeSignInClient.signIn()) {
            is AppleNativeSignInResult.Success -> {
                result.authorizationCode
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("Apple 재인증 코드를 확인하지 못했습니다.")
            }

            is AppleNativeSignInResult.Error -> {
                throw IllegalStateException(
                    if (result.message.contains("취소")) "회원탈퇴를 취소했습니다." else result.message
                )
            }

            is AppleNativeSignInResult.Unsupported -> {
                throw IllegalStateException(result.reason)
            }
        }
    }
}

private fun UserInfo.toDomainAuthProvider(): AuthProvider {
    val identityProvider = identities
        ?.firstNotNullOfOrNull { it.provider.toDomainAuthProviderOrNull() }
    if (identityProvider != null) return identityProvider

    val metadataProvider = appMetadata
        ?.get("provider")
        ?.jsonPrimitive
        ?.contentOrNull
        ?.toDomainAuthProviderOrNull()
    return metadataProvider ?: AuthProvider.Unknown
}

private fun String?.toDomainAuthProviderOrNull(): AuthProvider? {
    return when (this?.trim()?.lowercase()) {
        "apple" -> AuthProvider.Apple
        "google" -> AuthProvider.Google
        "kakao" -> AuthProvider.Kakao
        else -> null
    }
}

private fun parseDeleteAccountErrorMessage(rawBody: String): String? {
    if (rawBody.isBlank()) return null
    return runCatching {
        Json.parseToJsonElement(rawBody)
            .jsonObject["error"]
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun String.parseQueryParams(): Map<String, String> {
    val queryStart = indexOf('?')
    if (queryStart < 0) return emptyMap()

    val queryEnd = indexOf('#', startIndex = queryStart + 1).let { hashIndex ->
        if (hashIndex == -1) length else hashIndex
    }
    val rawQuery = substring(queryStart + 1, queryEnd)
    if (rawQuery.isBlank()) return emptyMap()

    return rawQuery
        .split('&')
        .asSequence()
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val separatorIndex = part.indexOf('=')
            val rawKey = if (separatorIndex >= 0) part.substring(0, separatorIndex) else part
            val rawValue = if (separatorIndex >= 0) part.substring(separatorIndex + 1) else ""
            val key = rawKey.decodeUrlPart()
            val value = rawValue.decodeUrlPart()
            key.takeIf { it.isNotBlank() }?.let { it to value }
        }
        .toMap()
}

private fun String.decodeUrlPart(): String {
    return runCatching { decodeURLQueryComponent(plusIsSpace = true) }.getOrDefault(this)
}

private fun String?.toEmailOtpTypeOrNull(): OtpType.Email? {
    return when (this?.trim()?.lowercase()) {
        "signup" -> OtpType.Email.SIGNUP
        "magiclink", "magic_link" -> OtpType.Email.MAGIC_LINK
        "invite" -> OtpType.Email.INVITE
        "recovery" -> OtpType.Email.RECOVERY
        "email_change", "emailchange" -> OtpType.Email.EMAIL_CHANGE
        "email" -> OtpType.Email.EMAIL
        else -> null
    }
}

private fun Throwable.toAuthError(defaultMessage: String): AuthResult.Error {
    return when (this) {
        is AuthRestException -> when (this.errorCode) {
            AuthErrorCode.InvalidCredentials -> AuthResult.Error("이메일 또는 비밀번호가 올바르지 않습니다.")
            AuthErrorCode.EmailNotConfirmed -> AuthResult.Error(
                message = "이메일 인증이 필요합니다. 메일함에서 인증 후 다시 시도해주세요.",
                type = AuthErrorType.EmailVerificationRequired,
            )
            AuthErrorCode.EmailExists,
            AuthErrorCode.UserAlreadyExists -> AuthResult.Error("이미 가입된 이메일입니다. 로그인하거나 비밀번호 찾기를 이용해주세요.")
            AuthErrorCode.UserNotFound -> AuthResult.Error("가입되지 않은 이메일입니다.")
            AuthErrorCode.SignupDisabled -> AuthResult.Error("현재 회원가입이 비활성화되어 있습니다.")
            AuthErrorCode.EmailProviderDisabled -> AuthResult.Error("이메일 로그인이 비활성화되어 있습니다. Supabase 설정을 확인해주세요.")
            AuthErrorCode.OtpDisabled -> AuthResult.Error("이메일 OTP 로그인이 비활성화되어 있습니다. 설정을 확인해주세요.")
            AuthErrorCode.OtpExpired -> AuthResult.Error("인증 코드가 만료되었습니다. 새 코드를 요청해주세요.")
            AuthErrorCode.UserBanned -> AuthResult.Error("이 계정은 차단되어 있습니다.")
            AuthErrorCode.WeakPassword -> AuthResult.Error("비밀번호가 너무 약합니다. 더 강한 비밀번호로 다시 시도해주세요.")
            AuthErrorCode.OverRequestRateLimit -> AuthResult.Error("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
            AuthErrorCode.OverEmailSendRateLimit -> AuthResult.Error("이메일 전송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
            AuthErrorCode.BadOauthCallback,
            AuthErrorCode.BadOauthState,
            AuthErrorCode.OauthProviderNotSupported,
            AuthErrorCode.ProviderDisabled -> AuthResult.Error("소셜 로그인 설정을 확인해주세요.")
            AuthErrorCode.ProviderEmailNeedsVerification -> AuthResult.Error(
                message = "이메일 인증이 필요합니다. 메일함에서 인증 후 다시 시도해주세요.",
                type = AuthErrorType.EmailVerificationRequired,
            )
            null -> AuthResult.Error(defaultMessage)
            else -> AuthResult.Error(defaultMessage)
        }
        is RestException -> AuthResult.Error(defaultMessage)
        else -> AuthResult.Error(defaultMessage)
    }
}

private fun AuthRestException.toKakaoSignInMessage(): String {
    return when (errorCode) {
        AuthErrorCode.BadOauthCallback,
        AuthErrorCode.BadOauthState,
        AuthErrorCode.OauthProviderNotSupported,
        AuthErrorCode.ProviderDisabled -> {
            "Supabase Kakao 로그인 설정을 확인해주세요."
        }
        AuthErrorCode.InvalidCredentials -> {
            "카카오 인증 토큰이 유효하지 않습니다. 카카오 OpenID Connect 설정을 확인해주세요."
        }
        AuthErrorCode.ProviderEmailNeedsVerification -> {
            "카카오 계정 이메일 인증이 필요합니다. 카카오 계정 설정에서 이메일 인증 후 다시 시도해주세요."
        }
        null -> buildDetailedKakaoFailureMessage(error = error, description = errorDescription)
        else -> buildDetailedKakaoFailureMessage(error = error, description = errorDescription)
    }
}

private fun RestException.toKakaoSignInMessage(): String {
    val statusSuffix = " (status=$statusCode)"
    val errorValue = error.trim()
    val descriptionValue = description?.trim().orEmpty()
    return when {
        errorValue.isNotEmpty() && descriptionValue.isNotEmpty() ->
            "Kakao 로그인에 실패했습니다. ($errorValue: $descriptionValue$statusSuffix)"
        errorValue.isNotEmpty() ->
            "Kakao 로그인에 실패했습니다. ($errorValue$statusSuffix)"
        descriptionValue.isNotEmpty() ->
            "Kakao 로그인에 실패했습니다. ($descriptionValue$statusSuffix)"
        else -> "Kakao 로그인에 실패했습니다$statusSuffix"
    }
}

private fun Throwable.toAppleSignInError(): AuthResult.Error {
    return when (this) {
        is AuthRestException -> when (errorCode) {
            AuthErrorCode.BadOauthCallback,
            AuthErrorCode.BadOauthState,
            AuthErrorCode.OauthProviderNotSupported,
            AuthErrorCode.ProviderDisabled -> {
                AuthResult.Error("Supabase Apple 로그인 설정을 확인해주세요.")
            }
            AuthErrorCode.InvalidCredentials -> {
                AuthResult.Error("Apple 로그인 토큰이 유효하지 않습니다. Apple 설정과 nonce 처리를 확인해주세요.")
            }
            null -> buildDetailedAppleFailureMessage(error = error, description = errorDescription)
            else -> buildDetailedAppleFailureMessage(error = error, description = errorDescription)
        }

        is RestException -> {
            val statusSuffix = " (status=$statusCode)"
            val errorValue = error.trim()
            val descriptionValue = description?.trim().orEmpty()
            when {
                errorValue.isNotEmpty() && descriptionValue.isNotEmpty() ->
                    AuthResult.Error("Apple 로그인에 실패했습니다. ($errorValue: $descriptionValue$statusSuffix)")
                errorValue.isNotEmpty() ->
                    AuthResult.Error("Apple 로그인에 실패했습니다. ($errorValue$statusSuffix)")
                descriptionValue.isNotEmpty() ->
                    AuthResult.Error("Apple 로그인에 실패했습니다. ($descriptionValue$statusSuffix)")
                else -> AuthResult.Error("Apple 로그인에 실패했습니다$statusSuffix")
            }
        }

        else -> toAuthError(defaultMessage = "Apple 로그인에 실패했습니다")
    }
}

private fun buildDetailedAppleFailureMessage(
    error: String?,
    description: String?,
): AuthResult.Error {
    val normalizedError = error?.trim().orEmpty().ifBlank { "unknown_error" }
    val normalizedDescription = description?.trim().orEmpty().ifBlank { null }
    return if (normalizedDescription != null) {
        AuthResult.Error("Apple 로그인에 실패했습니다. ($normalizedError: $normalizedDescription)")
    } else {
        AuthResult.Error("Apple 로그인에 실패했습니다. ($normalizedError)")
    }
}

private fun buildDetailedKakaoFailureMessage(
    error: String?,
    description: String?,
): String {
    val normalizedError = error?.trim().orEmpty().ifBlank { "unknown_error" }
    val normalizedDescription = description?.trim().orEmpty().ifBlank { null }
    return if (normalizedDescription != null) {
        "Kakao 로그인에 실패했습니다. ($normalizedError: $normalizedDescription)"
    } else {
        "Kakao 로그인에 실패했습니다. ($normalizedError)"
    }
}

private fun logSupabaseThrowable(t: Throwable) {
    // Avoid printing t.message (RestException includes URL + request headers).
    when (t) {
        is AuthRestException -> {
            logSupabaseError(
                featureName = AUTH_LOG_FEATURE_NAME,
                message = "auth_error=${t.error} description=${t.errorDescription} status=${t.statusCode}",
            )
        }
        is RestException -> {
            logSupabaseError(
                featureName = AUTH_LOG_FEATURE_NAME,
                message = "error=${t.error} description=${t.description} status=${t.statusCode}",
            )
        }
        else -> {
            logSupabaseError(
                featureName = AUTH_LOG_FEATURE_NAME,
                message = "type=${t::class.simpleName}",
            )
        }
    }
}

private const val AUTH_LOG_FEATURE_NAME: String = "Auth"

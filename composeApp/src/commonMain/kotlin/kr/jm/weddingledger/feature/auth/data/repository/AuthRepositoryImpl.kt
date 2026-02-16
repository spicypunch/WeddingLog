package kr.jm.weddingledger.feature.auth.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.model.AuthUser
import kr.jm.weddingledger.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override val authState: Flow<AuthState>
        get() = supabaseClient.auth.sessionStatus
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
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "인증 링크 처리에 실패했습니다."))
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "회원가입에 실패했습니다"))
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
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "로그인에 실패했습니다"))
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
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "인증 메일 재전송에 실패했습니다"))
        }
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return try {
            supabaseClient.auth.signOut()
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "로그아웃에 실패했습니다"))
        }
    }

    override suspend fun resetPassword(email: String): AuthResult<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            AuthResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            AuthResult.Error(t.toUserFriendlyMessage(defaultMessage = "비밀번호 재설정 이메일 전송에 실패했습니다"))
        }
    }

    override fun isLoggedIn(): Boolean {
        return supabaseClient.auth.currentUserOrNull() != null
    }
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

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is AuthRestException -> when (this.errorCode) {
            AuthErrorCode.InvalidCredentials -> "이메일 또는 비밀번호가 올바르지 않습니다."
            AuthErrorCode.EmailNotConfirmed -> "이메일 인증이 필요합니다. 메일함에서 인증 후 다시 시도해주세요."
            AuthErrorCode.EmailExists,
            AuthErrorCode.UserAlreadyExists -> "이미 가입된 이메일입니다. 로그인하거나 비밀번호 찾기를 이용해주세요."
            AuthErrorCode.UserNotFound -> "가입되지 않은 이메일입니다."
            AuthErrorCode.SignupDisabled -> "현재 회원가입이 비활성화되어 있습니다."
            AuthErrorCode.ProviderDisabled,
            AuthErrorCode.EmailProviderDisabled -> "이메일 로그인이 비활성화되어 있습니다. Supabase 설정을 확인해주세요."
            AuthErrorCode.UserBanned -> "이 계정은 차단되어 있습니다."
            AuthErrorCode.WeakPassword -> "비밀번호가 너무 약합니다. 더 강한 비밀번호로 다시 시도해주세요."
            AuthErrorCode.OverRequestRateLimit -> "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            AuthErrorCode.OverEmailSendRateLimit -> "이메일 전송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            AuthErrorCode.ProviderEmailNeedsVerification -> "이메일 인증이 필요합니다. 메일함에서 인증 후 다시 시도해주세요."
            null -> defaultMessage
            else -> defaultMessage
        }
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private fun logSupabaseThrowable(t: Throwable) {
    // Avoid printing t.message (RestException includes URL + request headers).
    when (t) {
        is AuthRestException -> {
            println("Supabase(Auth) error=${t.error} description=${t.errorDescription} status=${t.statusCode}")
        }
        is RestException -> {
            println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        }
        else -> {
            println("Auth error=${t::class.simpleName}")
        }
    }
}

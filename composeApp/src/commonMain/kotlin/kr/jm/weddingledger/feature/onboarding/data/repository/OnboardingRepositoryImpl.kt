package kr.jm.weddingledger.feature.onboarding.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.feature.onboarding.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.onboarding.data.dto.ProfileRowDto
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingErrorType
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : OnboardingRepository {

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            val profile = loadProfile(userId) ?: return OnboardingResult.Error("프로필 정보를 불러오지 못했습니다.")
            OnboardingResult.Success(profile.toDomain())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            t.toProfileFetchError()
        }
    }

    override suspend fun updateNickname(
        nickname: String,
    ): OnboardingResult<Unit> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            val payload = buildJsonObject {
                put("name", JsonPrimitive(nickname.trim()))
            }

            supabaseClient
                .from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                }

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            OnboardingResult.Error("닉네임 저장에 실패했습니다.")
        }
    }

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            val payload = buildJsonObject {
                put("name", JsonPrimitive(nickname.trim()))
                put("role", JsonPrimitive(role.name.uppercase()))
                put("is_wedding_date_undecided", JsonPrimitive(isWeddingDateUndecided))

                if (weddingDate == null) {
                    put("wedding_date", JsonNull)
                } else {
                    put("wedding_date", JsonPrimitive(weddingDate.toString()))
                }
            }

            supabaseClient
                .from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                }

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            OnboardingResult.Error("프로필 저장에 실패했습니다.")
        }
    }

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            val existing = loadProfile(userId)?.inviteCode?.trim()?.takeIf { it.isNotEmpty() }
            if (existing != null) return OnboardingResult.Success(existing)

            val code = assignInviteCodeWithRetry(userId)
                ?: return OnboardingResult.Error("초대 코드를 생성하지 못했습니다. 잠시 후 다시 시도해주세요.")

            OnboardingResult.Success(code)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            OnboardingResult.Error("초대 코드를 생성하지 못했습니다.")
        }
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> {
        val input = normalizeInviteCode(inviteCode)
        if (input.isBlank()) return OnboardingResult.Error("초대 코드를 입력해주세요.")
        if (!isValidInviteCode(input)) return OnboardingResult.Error("초대 코드는 6자리 숫자로 입력해주세요.")

        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            callJoinWeddingRpc(input)

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            t.toPartnerConnectError()
        }
    }

    override suspend fun completeOnboarding(): OnboardingResult<Unit> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            when (val ensured = ensureProfileRow(userId = userId, email = email)) {
                is OnboardingResult.Success -> Unit
                is OnboardingResult.Error -> return ensured
            }

            val payload = buildJsonObject { put("onboarding_completed", JsonPrimitive(true)) }
            supabaseClient
                .from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                }

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            OnboardingResult.Error("설정을 완료하지 못했습니다.")
        }
    }

    private suspend fun assignInviteCodeWithRetry(userId: String): String? {
        repeat(40) {
            val code = generateInviteCode()
            val payload = buildJsonObject { put("invite_code", JsonPrimitive(code)) }

            try {
                supabaseClient
                    .from("profiles")
                    .update(payload) {
                        filter { eq("id", userId) }
                    }
                return code
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (t.isInviteCodeConflict()) {
                    // Retry on unique-key collision.
                    return@repeat
                }
                throw t
            }
        }
        return null
    }

    private fun generateInviteCode(): String {
        return Random.nextInt(from = 0, until = 1_000_000).toString().padStart(6, '0')
    }

    private suspend fun loadProfile(userId: String): ProfileRowDto? {
        return supabaseClient
            .from("profiles")
            .select {
                filter { eq("id", userId) }
                limit(count = 1)
            }
            .decodeList<ProfileRowDto>()
            .firstOrNull()
    }

    private suspend fun ensureProfileRow(userId: String, email: String?): OnboardingResult<Unit> {
        val existing = runCatching { loadProfile(userId) }.getOrNull()
        if (existing != null) return OnboardingResult.Success(Unit)

        val weddingId = userId
        return try {
            supabaseClient
                .from("profiles")
                .insert(
                    ProfileInsertDto(
                        id = userId,
                        email = email,
                        name = null,
                        weddingId = weddingId,
                    )
                )
            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            t.toProfileFetchError()
        }
    }

    private suspend fun callJoinWeddingRpc(inviteCode: String) {
        val candidateParamNames = listOf("p_invite_code", "invite_code", "_invite_code")
        var lastParamError: RestException? = null

        for (paramName in candidateParamNames) {
            try {
                supabaseClient.postgrest.rpc(
                    "join_wedding",
                    buildJsonObject {
                        put(paramName, JsonPrimitive(inviteCode))
                    },
                )
                return
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (t is RestException && t.isJoinWeddingParamMismatch()) {
                    lastParamError = t
                    continue
                }
                throw t
            }
        }

        throw lastParamError ?: IllegalStateException("join_wedding RPC 호출에 실패했습니다.")
    }

    private fun currentUserIdAndEmail(): Pair<String, String?> {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")
        return user.id to user.email
    }
}

private val inviteCodeRegex = Regex("^\\d{6}$")

private fun normalizeInviteCode(value: String): String {
    return value.trim().filter { it in '0'..'9' }
}

private fun isValidInviteCode(value: String): Boolean = inviteCodeRegex.matches(value)

private fun ProfileRowDto.toDomain(): MyProfile = MyProfile(
    id = id,
    email = email,
    nickname = name,
    role = role.parseRoleOrNull(),
    weddingId = weddingId,
    weddingDate = weddingDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    isWeddingDateUndecided = isWeddingDateUndecided,
    inviteCode = inviteCode,
    onboardingCompleted = onboardingCompleted,
)

private fun String?.parseRoleOrNull(): UserRole? = when (this?.trim()?.uppercase()) {
    "GROOM" -> UserRole.Groom
    "BRIDE" -> UserRole.Bride
    else -> null
}

private fun Throwable.toPartnerConnectError(): OnboardingResult.Error {
    return when (this) {
        is RestException -> toPartnerConnectError()
        is IllegalStateException -> OnboardingResult.Error(
            message = "로그인이 필요합니다.",
            type = OnboardingErrorType.ReauthenticationRequired,
        )
        else -> OnboardingResult.Error("파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요.")
    }
}

private fun RestException.toPartnerConnectError(): OnboardingResult.Error {
    val primaryMessage = primaryMessageOrNull()
    return when {
        primaryMessage.matchesErrorMessage("내 초대 코드는 사용할 수 없습니다") ->
            OnboardingResult.Error("내 초대 코드는 사용할 수 없습니다.")
        primaryMessage.matchesErrorMessage("초대 코드를 입력해 주세요") ->
            OnboardingResult.Error("초대 코드를 입력해주세요.")
        primaryMessage.matchesErrorMessage("초대 코드가 유효하지 않습니다") ->
            OnboardingResult.Error("초대 코드가 올바르지 않습니다.")
        primaryMessage.matchesErrorMessage("로그인 후 사용해 주세요") ->
            OnboardingResult.Error(
                message = "로그인이 만료되었습니다. 다시 로그인 후 시도해주세요.",
                type = OnboardingErrorType.ReauthenticationRequired,
            )
        statusCode == 401 ->
            OnboardingResult.Error(
                message = "로그인이 필요합니다.",
                type = OnboardingErrorType.ReauthenticationRequired,
            )
        statusCode == 403 || errorCodeOrNull() == POSTGRES_INSUFFICIENT_PRIVILEGE ->
            OnboardingResult.Error("권한 문제로 파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요.")
        statusCode == 404 || isJoinWeddingFunctionMissing() ->
            OnboardingResult.Error("서버 설정이 아직 반영되지 않았습니다. 잠시 후 다시 시도해주세요.")
        else -> OnboardingResult.Error("파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요.")
    }
}

private fun Throwable.toProfileFetchError(): OnboardingResult.Error {
    return when (this) {
        is IllegalStateException -> OnboardingResult.Error(
            message = "로그인이 필요합니다. 다시 로그인해주세요.",
            type = OnboardingErrorType.ReauthenticationRequired,
        )
        is RestException -> toProfileFetchError()
        else -> OnboardingResult.Error("프로필 정보를 불러오지 못했습니다.")
    }
}

private fun RestException.toProfileFetchError(): OnboardingResult.Error {
    val primaryMessage = primaryMessageOrNull()
    return when {
        statusCode == 401 || primaryMessage.matchesErrorMessage("jwt expired") ->
            OnboardingResult.Error(
                message = "로그인이 만료되었습니다. 다시 로그인해주세요.",
                type = OnboardingErrorType.ReauthenticationRequired,
            )
        statusCode == 403 || errorCodeOrNull() == POSTGRES_INSUFFICIENT_PRIVILEGE ->
            OnboardingResult.Error(
                message = "로그인 권한을 확인할 수 없습니다. 다시 로그인해주세요.",
                type = OnboardingErrorType.ReauthenticationRequired,
            )
        errorCodeOrNull() == POSTGRES_FOREIGN_KEY_VIOLATION ->
            OnboardingResult.Error(
                message = "계정 정보를 찾을 수 없습니다. 다시 로그인해주세요.",
                type = OnboardingErrorType.ReauthenticationRequired,
            )
        else -> OnboardingResult.Error("프로필 정보를 불러오지 못했습니다.")
    }
}

private fun RestException.isJoinWeddingParamMismatch(): Boolean {
    return isJoinWeddingFunctionMissing()
}

private fun Throwable.isInviteCodeConflict(): Boolean {
    return this is RestException &&
        (errorCodeOrNull() == POSTGRES_UNIQUE_VIOLATION || statusCode == 409)
}

private fun RestException.isJoinWeddingFunctionMissing(): Boolean {
    return errorCodeOrNull() == POSTGREST_FUNCTION_NOT_FOUND ||
        errorCodeOrNull() == POSTGRES_UNDEFINED_FUNCTION ||
        statusCode == 404
}

private fun RestException.errorCodeOrNull(): String? {
    return (this as? PostgrestRestException)?.code?.trim()?.uppercase()
}

private fun RestException.primaryMessageOrNull(): String? {
    return error
        .trim()
        .ifBlank { description?.lineSequence()?.firstOrNull()?.trim().orEmpty() }
        .ifBlank { null }
}

private fun String?.matchesErrorMessage(expected: String): Boolean {
    return this?.normalizeErrorMessage() == expected.normalizeErrorMessage()
}

private fun String.normalizeErrorMessage(): String {
    return trim()
        .lowercase()
        .replace(" ", "")
        .removeSuffix(".")
        .removeSuffix("!")
        .removeSuffix("?")
}

private const val FEATURE_NAME: String = "Onboarding"
private const val POSTGRES_FOREIGN_KEY_VIOLATION: String = "23503"
private const val POSTGRES_INSUFFICIENT_PRIVILEGE: String = "42501"
private const val POSTGRES_UNDEFINED_FUNCTION: String = "42883"
private const val POSTGRES_UNIQUE_VIOLATION: String = "23505"
private const val POSTGREST_FUNCTION_NOT_FOUND: String = "PGRST202"

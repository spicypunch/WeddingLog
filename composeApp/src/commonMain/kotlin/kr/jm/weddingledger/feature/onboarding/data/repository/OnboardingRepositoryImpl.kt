package kr.jm.weddingledger.feature.onboarding.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
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
import kr.jm.weddingledger.feature.onboarding.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.onboarding.data.dto.ProfileRowDto
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : OnboardingRepository {

    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

            val profile = loadProfile(userId) ?: return OnboardingResult.Error("프로필 정보를 불러오지 못했습니다.")
            OnboardingResult.Success(profile.toDomain())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            OnboardingResult.Error("프로필 정보를 불러오지 못했습니다.")
        }
    }

    override suspend fun updateNickname(
        nickname: String,
    ): OnboardingResult<Unit> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

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
            logSupabaseThrowable(t)
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
            ensureProfileRow(userId = userId, email = email)

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
            logSupabaseThrowable(t)
            OnboardingResult.Error("프로필 저장에 실패했습니다.")
        }
    }

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

            val existing = loadProfile(userId)?.inviteCode?.trim()?.takeIf { it.isNotEmpty() }
            if (existing != null) return OnboardingResult.Success(existing)

            val code = assignInviteCodeWithRetry(userId)
                ?: return OnboardingResult.Error("초대 코드를 생성하지 못했습니다. 잠시 후 다시 시도해주세요.")

            OnboardingResult.Success(code)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            OnboardingResult.Error("초대 코드를 생성하지 못했습니다.")
        }
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> {
        val input = normalizeInviteCode(inviteCode)
        if (input.isBlank()) return OnboardingResult.Error("초대 코드를 입력해주세요.")
        if (!isValidInviteCode(input)) return OnboardingResult.Error("초대 코드는 W-1234 형식으로 입력해주세요.")

        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

            callJoinWeddingRpc(input)

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            OnboardingResult.Error(t.toPartnerConnectMessage())
        }
    }

    override suspend fun completeOnboarding(): OnboardingResult<Unit> {
        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

            val payload = buildJsonObject { put("onboarding_completed", JsonPrimitive(true)) }
            supabaseClient
                .from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                }

            OnboardingResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
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
        return "W-" + Random.nextInt(from = 0, until = 10_000).toString().padStart(4, '0')
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

    private suspend fun ensureProfileRow(userId: String, email: String?) {
        val existing = runCatching { loadProfile(userId) }.getOrNull()
        if (existing != null) return

        val weddingId = userId
        runCatching {
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
        }
    }

    private suspend fun callJoinWeddingRpc(inviteCode: String) {
        val candidateParamNames = listOf("invite_code", "p_invite_code", "_invite_code")
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

private val inviteCodeRegex = Regex("^W-\\d{4}$")

private fun normalizeInviteCode(value: String): String {
    val compact = value.trim().uppercase().replace(" ", "")
    val alnumAndDash = compact.filter { it.isLetterOrDigit() || it == '-' }
    if (Regex("^W\\d{4}$").matches(alnumAndDash)) {
        return "W-${alnumAndDash.drop(1)}"
    }
    return alnumAndDash
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

private fun logSupabaseThrowable(t: Throwable) {
    // Avoid printing t.message (RestException may include URL + request headers).
    when (t) {
        is RestException -> println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("Onboarding error=${t::class.simpleName}")
    }
}

private fun Throwable.toPartnerConnectMessage(): String {
    return when (this) {
        is RestException -> toPartnerConnectMessage()
        is IllegalStateException -> "로그인이 필요합니다."
        else -> "파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
    }
}

private fun RestException.toPartnerConnectMessage(): String {
    val normalized = listOfNotNull(error, description)
        .joinToString(separator = " ")
        .lowercase()

    return when {
        normalized.contains("내 초대 코드는 사용할 수 없습니다") -> "내 초대 코드는 사용할 수 없습니다."
        normalized.contains("초대 코드를 입력해 주세요") -> "초대 코드를 입력해주세요."
        normalized.contains("초대 코드가 유효하지 않습니다") -> "초대 코드가 올바르지 않습니다."
        normalized.contains("로그인 후 사용해 주세요") -> "로그인이 만료되었습니다. 다시 로그인 후 시도해주세요."
        statusCode == 401 -> "로그인이 필요합니다."
        statusCode == 403 || normalized.contains("permission denied") -> "권한 문제로 파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
        statusCode == 404 || (normalized.contains("join_wedding") && normalized.contains("does not exist")) ->
            "서버 설정이 아직 반영되지 않았습니다. 잠시 후 다시 시도해주세요."
        else -> "파트너 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
    }
}

private fun RestException.isJoinWeddingParamMismatch(): Boolean {
    val normalized = listOfNotNull(error, description)
        .joinToString(separator = " ")
        .lowercase()

    return normalized.contains("join_wedding") &&
        (
            normalized.contains("does not exist") ||
                normalized.contains("unexpected parameter")
            )
}

private fun Throwable.isInviteCodeConflict(): Boolean {
    if (this !is RestException) return false
    val normalized = listOfNotNull(error, description)
        .joinToString(separator = " ")
        .lowercase()
    return normalized.contains("duplicate key") ||
        normalized.contains("profiles_invite_code_key") ||
        normalized.contains("23505")
}

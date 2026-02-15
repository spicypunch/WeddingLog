package kr.jm.weddingledger.feature.onboarding.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
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

            val code = createUniqueInviteCode()
            val payload = buildJsonObject { put("invite_code", JsonPrimitive(code)) }
            supabaseClient
                .from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                }

            OnboardingResult.Success(code)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            OnboardingResult.Error("초대 코드를 생성하지 못했습니다.")
        }
    }

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> {
        val input = inviteCode.trim().uppercase()
        if (input.isBlank()) return OnboardingResult.Error("초대 코드를 입력해주세요.")

        return try {
            val (userId, email) = currentUserIdAndEmail()
            ensureProfileRow(userId = userId, email = email)

            val inviter = supabaseClient
                .from("profiles")
                .select {
                    filter { eq("invite_code", input) }
                    limit(count = 1)
                }
                .decodeList<ProfileRowDto>()
                .firstOrNull()
                ?: return OnboardingResult.Error("초대 코드가 올바르지 않습니다.")

            val weddingIdToJoin = inviter.weddingId ?: inviter.id

            val payload = buildJsonObject {
                put("wedding_id", JsonPrimitive(weddingIdToJoin))

                // Best-effort: 상대방이 입력한 예식일 정보를 복사해 동일하게 시작할 수 있게 합니다.
                put("wedding_date", inviter.weddingDate?.let { JsonPrimitive(it) } ?: JsonNull)
                put("is_wedding_date_undecided", JsonPrimitive(inviter.isWeddingDateUndecided))
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
            OnboardingResult.Error("파트너 연결에 실패했습니다.")
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

    private suspend fun createUniqueInviteCode(): String {
        repeat(20) { // 충분한 시도 (충돌 시 재시도)
            val code = "W-" + Random.nextInt(from = 0, until = 10_000).toString().padStart(4, '0')
            val exists = runCatching {
                supabaseClient
                    .from("profiles")
                    .select {
                        filter { eq("invite_code", code) }
                        limit(count = 1)
                    }
                    .decodeList<ProfileRowDto>()
                    .isNotEmpty()
            }.getOrDefault(false)
            if (!exists) return code
        }

        // Fallback: 거의 발생하지 않음
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

    private fun currentUserIdAndEmail(): Pair<String, String?> {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")
        return user.id to user.email
    }
}

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


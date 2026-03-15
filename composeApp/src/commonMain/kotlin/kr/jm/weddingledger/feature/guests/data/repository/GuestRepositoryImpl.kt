package kr.jm.weddingledger.feature.guests.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.core.network.WeddingIdResolver
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.feature.guests.data.dto.GuestInsertDto
import kr.jm.weddingledger.feature.guests.data.dto.GuestRowDto
import kr.jm.weddingledger.feature.guests.data.dto.GuestSettingsRowDto
import kr.jm.weddingledger.feature.guests.data.dto.GuestSettingsUpsertDto
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class GuestRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : GuestRepository {

    private val weddingIdResolver = WeddingIdResolver(supabaseClient)

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val rows = supabaseClient
                .from("guests")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "name", order = Order.ASCENDING)
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<GuestRowDto>()

            GuestsResult.Success(rows.map { it.toDomain() })
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 목록을 불러오지 못했습니다."))
        }
    }

    override suspend fun fetchGuestSettings(): GuestsResult<GuestSettings> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val row = supabaseClient
                .from(GUEST_SETTINGS_TABLE)
                .select {
                    filter { eq("wedding_id", weddingId) }
                }
                .decodeList<GuestSettingsRowDto>()
                .firstOrNull()

            GuestsResult.Success(
                GuestSettings(
                    guaranteedGuestCount = row?.guaranteedGuestCount?.coerceAtLeast(0) ?: 0,
                )
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 설정을 불러오지 못했습니다."))
        }
    }

    override suspend fun updateGuestSettings(settings: GuestSettings): GuestsResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val existing = supabaseClient
                .from(GUEST_SETTINGS_TABLE)
                .select {
                    filter { eq("wedding_id", weddingId) }
                }
                .decodeList<GuestSettingsRowDto>()
                .firstOrNull()

            if (existing == null) {
                supabaseClient
                    .from(GUEST_SETTINGS_TABLE)
                    .insert(
                        GuestSettingsUpsertDto(
                            weddingId = weddingId,
                            guaranteedGuestCount = settings.guaranteedGuestCount.coerceAtLeast(0),
                        )
                    )
            } else {
                supabaseClient
                    .from(GUEST_SETTINGS_TABLE)
                    .update(
                        buildJsonObject {
                            put("guaranteed_guest_count", JsonPrimitive(settings.guaranteedGuestCount.coerceAtLeast(0)))
                        }
                    ) {
                        filter { eq("wedding_id", weddingId) }
                    }
            }

            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "보증인원 저장에 실패했습니다."))
        }
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("guests")
                .insert(
                    GuestInsertDto(
                        weddingId = weddingId,
                        name = guest.name.trim(),
                        group = guest.side.toDbValue(),
                        relationshipScore = guest.relationshipScore.coerceIn(0, 2),
                        expectedGiftMoney = null,
                        isAttended = guest.attendance.toLegacyDbValue(),
                        attendanceLikelihood = guest.attendance.toDbValue(),
                        companionCount = guest.companionCount.coerceAtLeast(0),
                        actualAttended = guest.actualAttended,
                        actualGiftMoney = guest.actualGiftMoney?.takeIf { it > 0L },
                        note = guest.note?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 저장에 실패했습니다."))
        }
    }

    override suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("name", JsonPrimitive(guest.name.trim()))
                put("group", JsonPrimitive(guest.side.toDbValue()))
                put("relationship_score", JsonPrimitive(guest.relationshipScore.coerceIn(0, 2)))
                put("attendance_likelihood", JsonPrimitive(guest.attendance.toDbValue()))

                val legacyAttendance = guest.attendance.toLegacyDbValue()
                if (legacyAttendance == null) {
                    put("is_attended", JsonNull)
                } else {
                    put("is_attended", JsonPrimitive(legacyAttendance))
                }

                put("companion_count", JsonPrimitive(guest.companionCount.coerceAtLeast(0)))

                if (guest.actualAttended == null) {
                    put("actual_attended", JsonNull)
                } else {
                    put("actual_attended", JsonPrimitive(guest.actualAttended))
                }

                val actualGiftMoney = guest.actualGiftMoney?.takeIf { it > 0L }
                if (actualGiftMoney == null) {
                    put("actual_gift_money", JsonNull)
                } else {
                    put("actual_gift_money", JsonPrimitive(actualGiftMoney))
                }

                val note = guest.note?.trim()?.takeIf { it.isNotEmpty() }
                if (note == null) {
                    put("note", JsonNull)
                } else {
                    put("note", JsonPrimitive(note))
                }
            }

            supabaseClient
                .from("guests")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 수정에 실패했습니다."))
        }
    }

    override suspend fun deleteGuest(id: String): GuestsResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("guests")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 삭제에 실패했습니다."))
        }
    }

    override suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("attendance_likelihood", JsonPrimitive(attendance.toDbValue()))
                val legacyAttendance = attendance.toLegacyDbValue()
                if (legacyAttendance == null) {
                    put("is_attended", JsonNull)
                } else {
                    put("is_attended", JsonPrimitive(legacyAttendance))
                }
            }

            supabaseClient
                .from("guests")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "참석 상태 변경에 실패했습니다."))
        }
    }
}

private fun GuestRowDto.toDomain(): Guest {
    return Guest(
        id = id,
        name = name,
        side = group.toGuestSideType(),
        relationshipScore = (relationshipScore ?: 1).coerceIn(0, 2),
        attendance = attendanceLikelihood.toGuestAttendance() ?: isAttended.toGuestAttendance(),
        companionCount = (companionCount ?: 0).coerceAtLeast(0),
        actualAttended = actualAttended,
        actualGiftMoney = actualGiftMoney?.takeIf { it >= 0L },
        note = note?.trim()?.takeIf { it.isNotEmpty() },
    )
}

private fun String?.toGuestSideType(): GuestSideType {
    return when (this?.trim()?.lowercase()) {
        "groom", "신랑", "신랑측" -> GuestSideType.Groom
        "bride", "신부", "신부측" -> GuestSideType.Bride
        else -> GuestSideType.Both
    }
}

private fun Boolean?.toGuestAttendance(): GuestAttendance {
    return when (this) {
        true -> GuestAttendance.Attending
        false -> GuestAttendance.NotAttending
        null -> GuestAttendance.Undecided
    }
}

private fun String?.toGuestAttendance(): GuestAttendance? {
    return when (this?.trim()?.uppercase()) {
        "CERTAIN" -> GuestAttendance.Attending
        "NO" -> GuestAttendance.NotAttending
        "MAYBE" -> GuestAttendance.Undecided
        else -> null
    }
}

private fun GuestSideType.toDbValue(): String {
    return when (this) {
        GuestSideType.Groom -> "groom"
        GuestSideType.Bride -> "bride"
        GuestSideType.Both -> "both"
    }
}

private fun GuestAttendance.toLegacyDbValue(): Boolean? {
    return when (this) {
        GuestAttendance.Attending -> true
        GuestAttendance.NotAttending -> false
        GuestAttendance.Undecided -> null
    }
}

private fun GuestAttendance.toDbValue(): String {
    return when (this) {
        GuestAttendance.Attending -> "CERTAIN"
        GuestAttendance.Undecided -> "MAYBE"
        GuestAttendance.NotAttending -> "NO"
    }
}

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> {
            val postgresCode = postgresCodeOrNull()
            when {
                postgresCode == POSTGRES_UNDEFINED_TABLE ->
                    "하객 테이블이 아직 준비되지 않았습니다."
                statusCode == 403 || postgresCode == POSTGRES_INSUFFICIENT_PRIVILEGE ->
                    "권한 문제로 하객 정보를 불러오지 못했습니다."
                statusCode == 401 -> "로그인이 필요합니다."
                else -> defaultMessage
            }
        }
        else -> defaultMessage
    }
}

private fun RestException.postgresCodeOrNull(): String? {
    return (this as? PostgrestRestException)?.code?.trim()?.uppercase()
}

private const val POSTGRES_UNDEFINED_TABLE: String = "42P01"
private const val POSTGRES_INSUFFICIENT_PRIVILEGE: String = "42501"
private const val FEATURE_NAME: String = "Guests"
private const val GUEST_SETTINGS_TABLE: String = "guest_settings"

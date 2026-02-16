package kr.jm.weddingledger.feature.guests.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kr.jm.weddingledger.feature.guests.data.dto.GuestInsertDto
import kr.jm.weddingledger.feature.guests.data.dto.GuestRowDto
import kr.jm.weddingledger.feature.guests.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.guests.data.dto.ProfileRowDto
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class GuestRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : GuestRepository {

    private var cachedWeddingId: String? = null

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> {
        return try {
            val weddingId = getWeddingId()
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
            logSupabaseThrowable(t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 목록을 불러오지 못했습니다."))
        }
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("guests")
                .insert(
                    GuestInsertDto(
                        weddingId = weddingId,
                        name = guest.name.trim(),
                        group = guest.side.toDbValue(),
                        relationshipScore = guest.relationshipScore.coerceIn(0, 2),
                        expectedGiftMoney = guest.expectedGiftMoney?.takeIf { it > 0 },
                        isAttended = guest.attendance.toDbValue(),
                        note = guest.note?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 저장에 실패했습니다."))
        }
    }

    override suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            val payload = buildJsonObject {
                put("name", JsonPrimitive(guest.name.trim()))
                put("group", JsonPrimitive(guest.side.toDbValue()))
                put("relationship_score", JsonPrimitive(guest.relationshipScore.coerceIn(0, 2)))

                val expectedGiftMoney = guest.expectedGiftMoney?.takeIf { it > 0 }
                if (expectedGiftMoney == null) {
                    put("expected_gift_money", JsonNull)
                } else {
                    put("expected_gift_money", JsonPrimitive(expectedGiftMoney))
                }

                val isAttended = guest.attendance.toDbValue()
                if (isAttended == null) {
                    put("is_attended", JsonNull)
                } else {
                    put("is_attended", JsonPrimitive(isAttended))
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
            logSupabaseThrowable(t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 수정에 실패했습니다."))
        }
    }

    override suspend fun deleteGuest(id: String): GuestsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("guests")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }
            GuestsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "하객 삭제에 실패했습니다."))
        }
    }

    override suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            val payload = buildJsonObject {
                val isAttended = attendance.toDbValue()
                if (isAttended == null) {
                    put("is_attended", JsonNull)
                } else {
                    put("is_attended", JsonPrimitive(isAttended))
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
            logSupabaseThrowable(t)
            GuestsResult.Error(t.toUserFriendlyMessage(defaultMessage = "참석 상태 변경에 실패했습니다."))
        }
    }

    private suspend fun getWeddingId(): String {
        cachedWeddingId?.let { return it }

        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id

        val profile = try {
            supabaseClient
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(count = 1)
                }
                .decodeList<ProfileRowDto>()
                .firstOrNull()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            null
        }

        val weddingId = profile?.weddingId ?: userId

        if (profile == null) {
            try {
                supabaseClient
                    .from("profiles")
                    .insert(
                        ProfileInsertDto(
                            id = userId,
                            email = user.email,
                            weddingId = weddingId,
                        )
                    )
            } catch (_: Throwable) {
                // Ignore: RLS/trigger may already handle profile row creation.
            }
        }

        cachedWeddingId = weddingId
        return weddingId
    }
}

private fun GuestRowDto.toDomain(): Guest {
    return Guest(
        id = id,
        name = name,
        side = group.toGuestSideType(),
        relationshipScore = (relationshipScore ?: 1).coerceIn(0, 2),
        expectedGiftMoney = expectedGiftMoney,
        attendance = isAttended.toGuestAttendance(),
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

private fun GuestSideType.toDbValue(): String {
    return when (this) {
        GuestSideType.Groom -> "groom"
        GuestSideType.Bride -> "bride"
        GuestSideType.Both -> "both"
    }
}

private fun GuestAttendance.toDbValue(): Boolean? {
    return when (this) {
        GuestAttendance.Attending -> true
        GuestAttendance.NotAttending -> false
        GuestAttendance.Undecided -> null
    }
}

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> {
            val normalized = listOfNotNull(error, description).joinToString(" ").lowercase()
            when {
                normalized.contains("relation") && normalized.contains("guests") && normalized.contains("does not exist") ->
                    "하객 테이블이 아직 준비되지 않았습니다."
                normalized.contains("permission denied") || statusCode == 403 ->
                    "권한 문제로 하객 정보를 불러오지 못했습니다."
                statusCode == 401 -> "로그인이 필요합니다."
                else -> defaultMessage
            }
        }
        else -> defaultMessage
    }
}

private fun logSupabaseThrowable(t: Throwable) {
    when (t) {
        is RestException -> println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("Guests error=${t::class.simpleName}")
    }
}

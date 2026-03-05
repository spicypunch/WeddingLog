package kr.jm.weddingledger.feature.schedule.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.feature.schedule.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.schedule.data.dto.ProfileRowDto
import kr.jm.weddingledger.feature.schedule.data.dto.ScheduleEventRowDto
import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class ScheduleEventRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : ScheduleEventRepository {

    private var cachedUserId: String? = null
    private var cachedWeddingId: String? = null

    override suspend fun fetchEvents(): ScheduleResult<List<ScheduleEvent>> {
        return try {
            val weddingId = getWeddingId()
            val rows = supabaseClient
                .from("schedule_events")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "event_date", order = Order.ASCENDING)
                    order(column = "start_time", order = Order.ASCENDING)
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<ScheduleEventRowDto>()

            ScheduleResult.Success(rows.mapNotNull { it.toDomainOrNull() })
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("schedule_events")) {
                ScheduleResult.Error("일정 기능 초기 설정이 필요합니다. schedule_events 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "일정을 불러오지 못했습니다."))
            }
        }
    }

    override suspend fun createEvent(event: NewScheduleEvent): ScheduleResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            try {
                supabaseClient
                    .from("schedule_events")
                    .insert(buildCreatePayload(weddingId = weddingId, event = event, preferRepeatColumn = true))
            } catch (t: Throwable) {
                if (t is RestException && t.isMissingColumn(REPEAT_RULE_COLUMN)) {
                    supabaseClient
                        .from("schedule_events")
                        .insert(buildCreatePayload(weddingId = weddingId, event = event, preferRepeatColumn = false))
                } else {
                    throw t
                }
            }

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("schedule_events")) {
                ScheduleResult.Error("일정 테이블이 아직 준비되지 않았습니다. SQL을 먼저 실행해 주세요.")
            } else {
                ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "일정 저장에 실패했습니다."))
            }
        }
    }

    override suspend fun updateEvent(id: String, event: NewScheduleEvent): ScheduleResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            try {
                supabaseClient
                    .from("schedule_events")
                    .update(buildUpdatePayload(event = event, preferRepeatColumn = true)) {
                        filter { eq("id", id) }
                        filter { eq("wedding_id", weddingId) }
                    }
            } catch (t: Throwable) {
                if (t is RestException && t.isMissingColumn(REPEAT_RULE_COLUMN)) {
                    supabaseClient
                        .from("schedule_events")
                        .update(buildUpdatePayload(event = event, preferRepeatColumn = false)) {
                            filter { eq("id", id) }
                            filter { eq("wedding_id", weddingId) }
                        }
                } else {
                    throw t
                }
            }

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("schedule_events")) {
                ScheduleResult.Error("일정 테이블이 아직 준비되지 않았습니다. SQL을 먼저 실행해 주세요.")
            } else {
                ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "일정 수정에 실패했습니다."))
            }
        }
    }

    override suspend fun deleteEvent(id: String): ScheduleResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("schedule_events")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }
            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("schedule_events")) {
                ScheduleResult.Error("일정 테이블이 아직 준비되지 않았습니다. SQL을 먼저 실행해 주세요.")
            } else {
                ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "일정 삭제에 실패했습니다."))
            }
        }
    }

    private suspend fun getWeddingId(): String {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id
        if (cachedUserId == userId) {
            cachedWeddingId?.let { return it }
        }
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
                // Ignore: profile may be managed by trigger/RLS policy.
            }
        }

        cachedUserId = userId
        cachedWeddingId = weddingId
        return weddingId
    }
}

private fun buildCreatePayload(
    weddingId: String,
    event: NewScheduleEvent,
    preferRepeatColumn: Boolean,
) = buildJsonObject {
    put("wedding_id", JsonPrimitive(weddingId))
    put("title", JsonPrimitive(event.title.trim()))
    put("event_date", JsonPrimitive(event.eventDate.toString()))

    val startTime = event.startTime?.trim()?.takeIf { it.isNotEmpty() }
    if (startTime == null) put("start_time", JsonNull) else put("start_time", JsonPrimitive(startTime))

    val location = event.location?.trim()?.takeIf { it.isNotEmpty() }
    if (location == null) put("location", JsonNull) else put("location", JsonPrimitive(location))

    val memo = event.memo?.trim()?.takeIf { it.isNotEmpty() }
    if (preferRepeatColumn) {
        val repeatRule = event.repeatRule.toDbRepeatRuleOrNull()
        if (repeatRule == null) put("repeat_rule", JsonNull) else put("repeat_rule", JsonPrimitive(repeatRule))
        if (memo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(memo))
    } else {
        val encodedMemo = encodeMemo(memo, event.repeatRule)
        if (encodedMemo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(encodedMemo))
    }
}

private fun buildUpdatePayload(
    event: NewScheduleEvent,
    preferRepeatColumn: Boolean,
) = buildJsonObject {
    put("title", JsonPrimitive(event.title.trim()))
    put("event_date", JsonPrimitive(event.eventDate.toString()))

    val startTime = event.startTime?.trim()?.takeIf { it.isNotEmpty() }
    if (startTime == null) put("start_time", JsonNull) else put("start_time", JsonPrimitive(startTime))

    val location = event.location?.trim()?.takeIf { it.isNotEmpty() }
    if (location == null) put("location", JsonNull) else put("location", JsonPrimitive(location))

    val memo = event.memo?.trim()?.takeIf { it.isNotEmpty() }
    if (preferRepeatColumn) {
        val repeatRule = event.repeatRule.toDbRepeatRuleOrNull()
        if (repeatRule == null) put("repeat_rule", JsonNull) else put("repeat_rule", JsonPrimitive(repeatRule))
        if (memo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(memo))
    } else {
        val encodedMemo = encodeMemo(memo, event.repeatRule)
        if (encodedMemo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(encodedMemo))
    }
}

private fun ScheduleEventRowDto.toDomainOrNull(): ScheduleEvent? {
    val date = runCatching { LocalDate.parse(eventDate) }.getOrNull() ?: return null
    val (memo, legacyRepeatRule) = decodeMemoAndRepeatRule(memo)
    val repeatRule = repeatRule.toDomainRepeatRuleOrNull() ?: legacyRepeatRule
    return ScheduleEvent(
        id = id,
        title = title,
        eventDate = date,
        startTime = startTime,
        location = location,
        memo = memo,
        repeatRule = repeatRule,
    )
}

private fun EventRepeatRule.toDbRepeatRuleOrNull(): String? {
    return when (this) {
        EventRepeatRule.None -> null
        EventRepeatRule.Weekly -> "WEEKLY"
        EventRepeatRule.Monthly -> "MONTHLY"
    }
}

private fun String?.toDomainRepeatRuleOrNull(): EventRepeatRule? {
    val normalized = this?.trim()?.uppercase().orEmpty()
    return when (normalized) {
        "NONE" -> EventRepeatRule.None
        "WEEKLY" -> EventRepeatRule.Weekly
        "MONTHLY" -> EventRepeatRule.Monthly
        else -> null
    }
}

private fun encodeMemo(
    memo: String?,
    repeatRule: EventRepeatRule,
): String? {
    val pureMemo = memo?.trim()?.takeIf { it.isNotEmpty() }
    if (repeatRule == EventRepeatRule.None) return pureMemo
    val header = "$REPEAT_MARKER${repeatRule.name}"
    return if (pureMemo == null) {
        header
    } else {
        "$header\n$pureMemo"
    }
}

private fun decodeMemoAndRepeatRule(
    rawMemo: String?,
): Pair<String?, EventRepeatRule> {
    val memo = rawMemo?.trim().orEmpty()
    if (memo.isBlank()) return null to EventRepeatRule.None

    val firstLine = memo.lineSequence().firstOrNull()?.trim().orEmpty()
    if (!firstLine.startsWith(REPEAT_MARKER)) {
        return memo to EventRepeatRule.None
    }

    val ruleText = firstLine.removePrefix(REPEAT_MARKER).trim()
    val repeatRule = EventRepeatRule.entries.firstOrNull { it.name == ruleText } ?: EventRepeatRule.None
    val remain = memo
        .lineSequence()
        .drop(1)
        .joinToString("\n")
        .trim()
        .ifBlank { null }
    return remain to repeatRule
}

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private fun RestException.isMissingRelation(relationName: String): Boolean {
    val normalized = listOfNotNull(error, description).joinToString(" ").lowercase()
    return normalized.contains("relation") &&
        normalized.contains(relationName.lowercase()) &&
        normalized.contains("does not exist")
}

private fun RestException.isMissingColumn(columnName: String): Boolean {
    val normalized = listOfNotNull(error, description).joinToString(" ").lowercase()
    return normalized.contains("column") &&
        normalized.contains(columnName.lowercase()) &&
        normalized.contains("does not exist")
}

private fun logSupabaseThrowable(t: Throwable) {
    when (t) {
        is RestException -> println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("ScheduleEvents error=${t::class.simpleName}")
    }
}

private const val REPEAT_MARKER: String = "@repeat="
private const val REPEAT_RULE_COLUMN: String = "repeat_rule"

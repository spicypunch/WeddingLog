package kr.jm.weddingledger.feature.schedule.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.core.network.WeddingIdResolver
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.feature.schedule.data.dto.ChecklistInsertDto
import kr.jm.weddingledger.feature.schedule.data.dto.ChecklistRowDto
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository

class ChecklistRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : ChecklistRepository {

    private val weddingIdResolver = WeddingIdResolver(supabaseClient)

    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val result = supabaseClient
                .from("checklists")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }

            val rows = result.decodeList<ChecklistRowDto>()
            ScheduleResult.Success(rows.map { it.toDomain() })
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "체크리스트를 불러오지 못했습니다."))
        }
    }

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()

            val dto = ChecklistInsertDto(
                weddingId = weddingId,
                title = task.title.trim(),
                groupTitle = task.groupTitle,
                dDayTag = task.dTag,
                isChecked = task.isChecked,
                targetDate = task.targetDate?.toString(), // ISO-8601 (yyyy-MM-dd)
                memo = task.memo?.trim()?.takeIf { it.isNotEmpty() },
            )

            supabaseClient
                .from("checklists")
                .insert(dto)

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "체크리스트 저장에 실패했습니다."))
        }
    }

    override suspend fun updateChecklist(id: String, task: NewChecklistTask): ScheduleResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()

            val payload = buildJsonObject {
                put("title", JsonPrimitive(task.title.trim()))
                put("group_title", JsonPrimitive(task.groupTitle))
                put("d_day_tag", JsonPrimitive(task.dTag))
                put("is_checked", JsonPrimitive(task.isChecked))

                if (task.targetDate == null) {
                    put("target_date", JsonNull)
                } else {
                    put("target_date", JsonPrimitive(task.targetDate.toString()))
                }

                val memo = task.memo?.trim()?.takeIf { it.isNotEmpty() }
                if (memo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(memo))
            }

            supabaseClient
                .from("checklists")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "체크리스트 수정에 실패했습니다."))
        }
    }

    override suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("is_checked", JsonPrimitive(checked))
            }

            supabaseClient
                .from("checklists")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "체크 상태를 변경하지 못했습니다."))
        }
    }

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("checklists")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            ScheduleResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            ScheduleResult.Error(t.toUserFriendlyMessage(defaultMessage = "체크리스트 삭제에 실패했습니다."))
        }
    }
}

private fun ChecklistRowDto.toDomain(): ChecklistTask = ChecklistTask(
    id = id,
    title = title,
    dTag = dDayTag,
    groupTitle = groupTitle,
    isChecked = isChecked,
    targetDate = targetDate?.let { parseLocalDateOrNull(it) },
    memo = memo,
)

private fun parseLocalDateOrNull(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private const val FEATURE_NAME: String = "Checklist"

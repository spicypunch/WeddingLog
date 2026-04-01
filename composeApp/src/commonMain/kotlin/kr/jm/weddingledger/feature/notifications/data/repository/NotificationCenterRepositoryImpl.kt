package kr.jm.weddingledger.feature.notifications.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kr.jm.weddingledger.core.platform.LocalNotificationPayload
import kr.jm.weddingledger.core.platform.LocalNotificationScheduleResult
import kr.jm.weddingledger.core.platform.cancelLocalNotification
import kr.jm.weddingledger.core.platform.scheduleLocalNotification
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.notifications.data.dto.PartnerActivityNotificationRowDto
import kr.jm.weddingledger.feature.notifications.data.local.ReminderPreferenceStorage
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterErrorCode
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationCenterResult
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedItem
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationFeedSection
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationReminderPlan
import kr.jm.weddingledger.feature.notifications.domain.repository.NotificationCenterRepository
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository

class NotificationCenterRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val scheduleEventRepository: ScheduleEventRepository,
    private val checklistRepository: ChecklistRepository,
    private val moneyRepository: MoneyRepository,
) : NotificationCenterRepository {

    override suspend fun fetchFeed(): NotificationCenterResult<List<NotificationFeedItem>> {
        val nowEpoch = Clock.System.now().toEpochMilliseconds()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val zone = TimeZone.currentSystemDefault()

        val activityItems = loadPartnerActivityItems()
            .sortedBy { it.sortKeyEpochMillis }
            .take(MAX_ACTIVITY_ITEMS)
        val reminderItems = buildList {
            addAll(loadScheduleItems(today = today, nowEpoch = nowEpoch, zone = zone))
            addAll(loadChecklistItems(today = today, nowEpoch = nowEpoch, zone = zone))
            addAll(loadBudgetWarningItems(nowEpoch = nowEpoch))
        }
            .sortedBy { it.sortKeyEpochMillis }
            .take(MAX_REMINDER_ITEMS)

        return NotificationCenterResult.Success(activityItems + reminderItems)
    }

    override suspend fun markActivityItemsRead(
        notificationIds: List<String>,
    ): NotificationCenterResult<Unit> {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: return NotificationCenterResult.Error("로그인 후 다시 시도해주세요.")
        val sanitizedIds = notificationIds.map(String::trim).filter(String::isNotEmpty).distinct()
        if (sanitizedIds.isEmpty()) {
            return NotificationCenterResult.Success(Unit)
        }

        return runCatching {
            supabaseClient
                .from("partner_activity_notifications")
                .update(
                    buildJsonObject {
                        put("is_read", JsonPrimitive(true))
                    }
                ) {
                    filter {
                        eq("target_user_id", user.id)
                        isIn("id", sanitizedIds)
                    }
                }
            NotificationCenterResult.Success(Unit)
        }.getOrElse {
            NotificationCenterResult.Error("읽음 처리에 실패했습니다.")
        }
    }

    override suspend fun scheduleReminder(
        plan: NotificationReminderPlan,
    ): NotificationCenterResult<Unit> {
        return when (
            scheduleLocalNotification(
                payload = LocalNotificationPayload(
                    id = plan.notificationId,
                    title = plan.title,
                    body = plan.body,
                    triggerAtEpochMillis = plan.triggerAtEpochMillis,
                )
            )
        ) {
            LocalNotificationScheduleResult.Scheduled -> {
                ReminderPreferenceStorage.addScheduledReminderId(plan.notificationId)
                NotificationCenterResult.Success(Unit)
            }
            LocalNotificationScheduleResult.PermissionDenied -> {
                NotificationCenterResult.Error(
                    message = "알림 권한이 꺼져 있어요. 권한 허용 후 다시 시도해주세요.",
                    code = NotificationCenterErrorCode.PermissionDenied,
                )
            }

            LocalNotificationScheduleResult.InvalidInput -> {
                NotificationCenterResult.Error(
                    message = "리마인더 정보가 올바르지 않습니다.",
                    code = NotificationCenterErrorCode.InvalidInput,
                )
            }

            LocalNotificationScheduleResult.Failed -> {
                NotificationCenterResult.Error("리마인더 예약에 실패했습니다.")
            }
        }
    }

    override suspend fun cancelReminder(
        notificationId: String,
    ): NotificationCenterResult<Unit> {
        val success = cancelLocalNotification(notificationId)
        if (!success) return NotificationCenterResult.Error("리마인더 취소에 실패했습니다.")
        ReminderPreferenceStorage.removeScheduledReminderId(notificationId)
        return NotificationCenterResult.Success(Unit)
    }

    override suspend fun fetchScheduledReminderIds(): NotificationCenterResult<Set<String>> {
        return runCatching { ReminderPreferenceStorage.scheduledReminderIds.first() }
            .fold(
                onSuccess = { ids -> NotificationCenterResult.Success(ids) },
                onFailure = { NotificationCenterResult.Error("리마인더 상태를 불러오지 못했습니다.") },
            )
    }

    private suspend fun loadPartnerActivityItems(): List<NotificationFeedItem> {
        val user = supabaseClient.auth.currentUserOrNull() ?: return emptyList()
        val rows = runCatching {
            supabaseClient
                .from("partner_activity_notifications")
                .select {
                    filter { eq("target_user_id", user.id) }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(MAX_ACTIVITY_ITEMS.toLong())
                }
                .decodeList<PartnerActivityNotificationRowDto>()
        }.getOrElse {
            return emptyList()
        }

        return rows.mapNotNull { it.toFeedItemOrNull() }
    }

    private suspend fun loadScheduleItems(
        today: LocalDate,
        nowEpoch: Long,
        zone: TimeZone,
    ): List<NotificationFeedItem> = when (val result = scheduleEventRepository.fetchEvents()) {
        is ScheduleResult.Success -> {
            result.data
                .asSequence()
                .filter { it.eventDate >= today }
                .map { event ->
                    val eventEpoch = event.toEventEpochMillis(zone)
                    val dDayText = toDdayText(today = today, date = event.eventDate)
                    val locationText = event.location?.trim()?.takeIf { it.isNotEmpty() } ?: "장소 미정"
                    val startTimeText = event.startTime?.trim()?.takeIf { it.isNotEmpty() } ?: "시간 미정"

                    NotificationFeedItem(
                        id = "event-${event.id}",
                        type = NotificationItemType.Schedule,
                        section = NotificationFeedSection.Reminder,
                        title = "일정: ${event.title}",
                        subtitle = "${event.eventDate.toDotDate()} · $startTimeText · $dDayText",
                        detail = locationText,
                        sortKeyEpochMillis = eventEpoch,
                        reminderPlan = createReminderPlan(
                            id = "event-reminder-${event.id}",
                            triggerAtEpochMillis = eventEpoch - ONE_DAY_MILLIS,
                            nowEpoch = nowEpoch,
                            title = "내일 일정: ${event.title}",
                            body = "${event.eventDate.toDotDate()} $startTimeText · $locationText",
                        ),
                    )
                }
                .toList()
        }

        is ScheduleResult.Error -> emptyList()
    }

    private suspend fun loadChecklistItems(
        today: LocalDate,
        nowEpoch: Long,
        zone: TimeZone,
    ): List<NotificationFeedItem> = when (val result = checklistRepository.fetchChecklist()) {
        is ScheduleResult.Success -> {
            result.data
                .asSequence()
                .filter { !it.isChecked && it.targetDate != null }
                .map { task ->
                    val targetDate = task.targetDate ?: return@map null
                    if (targetDate < today) return@map null

                    val dueEpoch = LocalDateTime(
                        year = targetDate.year,
                        month = targetDate.month,
                        day = targetDate.day,
                        hour = 9,
                        minute = 0,
                    ).toInstant(zone).toEpochMilliseconds()
                    val dDayText = toDdayText(today = today, date = targetDate)

                    NotificationFeedItem(
                        id = "checklist-${task.id}",
                        type = NotificationItemType.Checklist,
                        section = NotificationFeedSection.Reminder,
                        title = "체크리스트: ${task.title}",
                        subtitle = "마감 ${targetDate.toDotDate()} · $dDayText",
                        detail = task.memo?.trim()?.takeIf { it.isNotEmpty() } ?: "체크가 필요한 할 일이 있어요.",
                        sortKeyEpochMillis = dueEpoch,
                        reminderPlan = createReminderPlan(
                            id = "checklist-reminder-${task.id}",
                            triggerAtEpochMillis = dueEpoch - ONE_DAY_MILLIS,
                            nowEpoch = nowEpoch,
                            title = "내일 체크리스트 마감: ${task.title}",
                            body = "마감일 ${targetDate.toDotDate()} · 체크리스트를 확인해주세요.",
                        ),
                    )
                }
                .filterNotNull()
                .toList()
        }

        is ScheduleResult.Error -> emptyList()
    }

    private suspend fun loadBudgetWarningItems(
        nowEpoch: Long,
    ): List<NotificationFeedItem> = when (val result = moneyRepository.fetchSnapshot()) {
        is MoneyResult.Success -> {
            val spentByBudgetItem = result.data.expenses
                .filter { it.budgetItemId != null }
                .groupBy { it.budgetItemId }
                .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

            result.data.categories.mapNotNull { category ->
                val categoryItems = result.data.budgetItems.filter { it.categoryId == category.id }
                if (categoryItems.isEmpty()) return@mapNotNull null

                val target = categoryItems.sumOf { it.targetAmount }
                if (target <= 0) return@mapNotNull null

                val spent = categoryItems.sumOf { item -> spentByBudgetItem[item.id] ?: 0 }
                if (spent <= target) return@mapNotNull null

                val exceeded = spent - target
                NotificationFeedItem(
                    id = "budget-${category.id}",
                    type = NotificationItemType.BudgetWarning,
                    section = NotificationFeedSection.Reminder,
                    title = "예산 초과: ${category.name}",
                    subtitle = "목표 ${formatKrw(target)} / 사용 ${formatKrw(spent)}",
                    detail = "${formatKrw(exceeded)} 초과되었습니다.",
                    sortKeyEpochMillis = nowEpoch,
                    reminderPlan = null,
                )
            }
        }

        is MoneyResult.Error -> emptyList()
    }
}

private fun createReminderPlan(
    id: String,
    triggerAtEpochMillis: Long,
    nowEpoch: Long,
    title: String,
    body: String,
): NotificationReminderPlan? {
    if (triggerAtEpochMillis <= nowEpoch + 30_000L) return null
    return NotificationReminderPlan(
        notificationId = id,
        title = title,
        body = body,
        triggerAtEpochMillis = triggerAtEpochMillis,
    )
}

private fun toDdayText(
    today: LocalDate,
    date: LocalDate,
): String {
    val diff = today.daysUntil(date)
    return when {
        diff > 0 -> "D-$diff"
        diff == 0 -> "D-DAY"
        else -> "D+${diff.absoluteValue}"
    }
}

private fun String?.toTimeOrDefault(): Pair<Int, Int> {
    val raw = this?.trim().orEmpty()
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(raw) ?: return 9 to 0
    val hour = match.groupValues[1].toIntOrNull() ?: return 9 to 0
    val minute = match.groupValues[2].toIntOrNull() ?: return 9 to 0
    if (hour !in 0..23 || minute !in 0..59) return 9 to 0
    return hour to minute
}

private fun kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent.toEventEpochMillis(
    zone: TimeZone,
): Long {
    val (hour, minute) = startTime.toTimeOrDefault()
    return LocalDateTime(
        year = eventDate.year,
        month = eventDate.month,
        day = eventDate.day,
        hour = hour,
        minute = minute,
    ).toInstant(zone).toEpochMilliseconds()
}

private fun LocalDate.toDotDate(): String {
    val mm = month.number.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "${year}.$mm.$dd"
}

private fun formatKrw(amount: Int): String {
    val sign = if (amount < 0) "-" else ""
    return "${sign}₩${formatNumber(amount.toLong().absoluteValue)}"
}

private fun formatNumber(value: Long): String = value
    .toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

private fun PartnerActivityNotificationRowDto.toFeedItemOrNull(): NotificationFeedItem? {
    val itemType = when (type.trim().lowercase()) {
        "expense_added" -> NotificationItemType.PartnerExpenseAdded
        "schedule_added" -> NotificationItemType.PartnerScheduleAdded
        else -> return null
    }
    val createdInstant = runCatching { Instant.parse(createdAt) }.getOrElse { return null }
    val createdEpoch = createdInstant.toEpochMilliseconds()
    return NotificationFeedItem(
        id = id,
        type = itemType,
        section = NotificationFeedSection.Activity,
        title = title,
        subtitle = createdInstant.toKoreanActivityDateText(),
        detail = body,
        sortKeyEpochMillis = -createdEpoch,
        reminderPlan = null,
        isUnread = !isRead,
    )
}

private fun Instant.toKoreanActivityDateText(): String {
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.date.toDotDate()} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}

private const val MAX_ACTIVITY_ITEMS: Int = 30
private const val MAX_REMINDER_ITEMS: Int = 30
private const val ONE_DAY_MILLIS: Long = 24L * 60L * 60L * 1000L

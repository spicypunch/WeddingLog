package kr.jm.weddingledger.feature.schedule.presentation

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule

enum class ScheduleTab(val label: String) {
    Schedule("일정"),
    Checklist("체크리스트"),
}

data class ScheduleUiState(
    val selectedTab: ScheduleTab,
    val calendar: CalendarUiState,
    val checklist: ChecklistUiState,
    val isRefreshing: Boolean = false,
)

data class CalendarUiState(
    val monthLabel: String,
    val dayOfWeekLabels: List<String>,
    val cells: List<CalendarDayCell>,
    val upcoming: List<UpcomingScheduleItem>,
    val upcomingTitle: String,
    val upcomingEmptyText: String,
    val selectedDayTitle: String? = null,
    val selectedDayItems: List<UpcomingScheduleItem> = emptyList(),
    val selectedDayEmptyText: String = "선택한 날짜에 일정이 없어요.",
    val checklistFocusTitle: String = "마감 임박 체크리스트",
    val checklistFocusItems: List<ChecklistItem> = emptyList(),
    val checklistFocusEmptyText: String = "예정일이 있는 체크리스트가 없어요.",
    val selectedDayId: String? = null,
    val searchQuery: String = "",
    val sortType: UpcomingSortType = UpcomingSortType.DateAsc,
    val message: String? = null,
)

enum class CalendarDayTextTone {
    Normal,
    Primary,
    Muted,
    OnPrimary,
}

enum class CalendarDayBackgroundTone {
    None,
    SoftPrimary,
    SolidPrimary,
}

enum class CalendarDotStyle {
    Primary,
    Neutral,
}

enum class UpcomingSortType(val label: String) {
    DateAsc("날짜 빠른순"),
    DateDesc("날짜 늦은순"),
    Title("이름순"),
}

data class CalendarDayCell(
    val id: String,
    val day: Int,
    val inMonth: Boolean,
    val isSelected: Boolean = false,
    val isDDay: Boolean = false,
    val dDayLabel: String? = null,
    val textTone: CalendarDayTextTone = CalendarDayTextTone.Normal,
    val backgroundTone: CalendarDayBackgroundTone = CalendarDayBackgroundTone.None,
    val dotStyle: CalendarDotStyle? = null,
    val markerCount: Int = 0,
)

enum class UpcomingTagTone {
    Primary,
    Neutral,
}

enum class UpcomingSource {
    Event,
    Checklist,
}

data class UpcomingScheduleItem(
    val id: String,
    val source: UpcomingSource,
    val sourceEntityId: String,
    val date: LocalDate,
    val monthAbbr: String,
    val dayOfMonth: String,
    val title: String,
    val subtitle: String,
    val dTagText: String,
    val tagTone: UpcomingTagTone,
    val isCompleted: Boolean,
    val eventStartTime: String? = null,
    val eventLocation: String? = null,
    val eventMemo: String? = null,
    val repeatRule: EventRepeatRule = EventRepeatRule.None,
)

data class ChecklistUiState(
    val groups: List<ChecklistGroup>,
    val isLoading: Boolean = false,
    val message: String? = null,
) {
    val totalCount: Int
        get() = groups.sumOf { it.items.size }

    val completedCount: Int
        get() = groups.sumOf { group -> group.items.count { it.isDone } }

    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val percentText: String
        get() = "${(progressFraction * 100).toInt()}%"

    val countText: String
        get() = "${completedCount}/${totalCount}개"

    companion object {
        fun empty(): ChecklistUiState = ChecklistUiState(groups = emptyList())
    }
}

enum class DTagStyle {
    Primary,
    Neutral,
}

data class ChecklistGroup(
    val id: String,
    val dTag: String,
    val title: String,
    val tagStyle: DTagStyle,
    val items: List<ChecklistItem>,
) {
    val completedCount: Int
        get() = items.count { it.isDone }

    val totalCount: Int
        get() = items.size

    val progressText: String
        get() = "$completedCount/$totalCount 완료"
}

enum class ChecklistItemSubtitleTone {
    Muted,
    Primary,
}

data class ChecklistItem(
    val id: String,
    val dTag: String? = null,
    val title: String,
    val targetDate: LocalDate? = null,
    val memo: String? = null,
    val subtitle: String?,
    val subtitleTone: ChecklistItemSubtitleTone,
    val isDone: Boolean,
)

sealed interface ScheduleAction {
    data class OnSelectTab(val tab: ScheduleTab) : ScheduleAction
    data object OnClickAdd : ScheduleAction
    data object OnClickNotifications : ScheduleAction
    data object OnClickPrevMonth : ScheduleAction
    data object OnClickNextMonth : ScheduleAction
    data class OnSelectCalendarDay(val dayId: String) : ScheduleAction
    data class OnCalendarQueryChange(val value: String) : ScheduleAction
    data class OnSelectUpcomingSort(val value: UpcomingSortType) : ScheduleAction
    data object OnClickViewAllUpcoming : ScheduleAction
    data class OnClickUpcomingSchedule(val id: String) : ScheduleAction
    data class OnClickUpcomingMore(val id: String) : ScheduleAction
    data class OnToggleChecklistItem(val itemId: String) : ScheduleAction
    data class OnClickChecklistItemMore(val itemId: String) : ScheduleAction

    data object OnRefresh : ScheduleAction
    data object OnRefreshChecklist : ScheduleAction
    data class OnCreateChecklist(
        val title: String,
        val dTag: String,
        val targetDate: LocalDate?,
        val isCompleted: Boolean,
        val memo: String,
    ) : ScheduleAction
    data class OnUpdateChecklist(
        val id: String,
        val title: String,
        val dTag: String,
        val targetDate: LocalDate?,
        val isCompleted: Boolean,
        val memo: String,
    ) : ScheduleAction
    data class OnCreateScheduleEvent(
        val title: String,
        val eventDate: LocalDate,
        val startTime: String,
        val location: String,
        val memo: String,
        val repeatRule: EventRepeatRule,
    ) : ScheduleAction
    data class OnUpdateScheduleEvent(
        val id: String,
        val title: String,
        val eventDate: LocalDate,
        val startTime: String,
        val location: String,
        val memo: String,
        val repeatRule: EventRepeatRule,
    ) : ScheduleAction
    data class OnDeleteScheduleEvent(val id: String) : ScheduleAction
    data class OnDeleteChecklist(val id: String) : ScheduleAction
}

sealed interface ScheduleEffect {
    data object ChecklistSaved : ScheduleEffect
    data object ChecklistSaveFailed : ScheduleEffect
    data object ScheduleEventSaved : ScheduleEffect
    data object ScheduleEventSaveFailed : ScheduleEffect
}

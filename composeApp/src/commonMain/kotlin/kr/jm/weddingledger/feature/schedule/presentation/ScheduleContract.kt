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
) {
    companion object {
        fun sample(): ScheduleUiState = ScheduleUiState(
            selectedTab = ScheduleTab.Schedule,
            calendar = CalendarUiState.sample(),
            checklist = ChecklistUiState.sample(),
        )
    }
}

data class CalendarUiState(
    val monthLabel: String,
    val dayOfWeekLabels: List<String>,
    val cells: List<CalendarDayCell>,
    val upcoming: List<UpcomingScheduleItem>,
    val upcomingTitle: String,
    val upcomingEmptyText: String,
    val selectedDayId: String? = null,
    val searchQuery: String = "",
    val sortType: UpcomingSortType = UpcomingSortType.DateAsc,
    val message: String? = null,
) {
    companion object {
        fun sample(): CalendarUiState = CalendarUiState(
            monthLabel = "2023년 10월",
            dayOfWeekLabels = listOf("일", "월", "화", "수", "목", "금", "토"),
            cells = listOf(
                CalendarDayCell(id = "p-28", day = 28, inMonth = false, textTone = CalendarDayTextTone.Muted),
                CalendarDayCell(id = "p-29", day = 29, inMonth = false, textTone = CalendarDayTextTone.Muted),
                CalendarDayCell(id = "p-30", day = 30, inMonth = false, textTone = CalendarDayTextTone.Muted),

                CalendarDayCell(id = "d-1", day = 1, inMonth = true),
                CalendarDayCell(id = "d-2", day = 2, inMonth = true),
                CalendarDayCell(
                    id = "d-3",
                    day = 3,
                    inMonth = true,
                    textTone = CalendarDayTextTone.Primary,
                    dotStyle = CalendarDotStyle.Primary,
                ),
                CalendarDayCell(id = "d-4", day = 4, inMonth = true),
                CalendarDayCell(id = "d-5", day = 5, inMonth = true),
                CalendarDayCell(id = "d-6", day = 6, inMonth = true),
                CalendarDayCell(id = "d-7", day = 7, inMonth = true),
                CalendarDayCell(id = "d-8", day = 8, inMonth = true),
                CalendarDayCell(
                    id = "d-9",
                    day = 9,
                    inMonth = true,
                    isSelected = true,
                    textTone = CalendarDayTextTone.Primary,
                    backgroundTone = CalendarDayBackgroundTone.SoftPrimary,
                    dotStyle = CalendarDotStyle.Primary,
                ),
                CalendarDayCell(id = "d-10", day = 10, inMonth = true),
                CalendarDayCell(id = "d-11", day = 11, inMonth = true),
                CalendarDayCell(id = "d-12", day = 12, inMonth = true),
                CalendarDayCell(id = "d-13", day = 13, inMonth = true),
                CalendarDayCell(
                    id = "d-14",
                    day = 14,
                    inMonth = true,
                    dotStyle = CalendarDotStyle.Neutral,
                ),
                CalendarDayCell(id = "d-15", day = 15, inMonth = true),
                CalendarDayCell(id = "d-16", day = 16, inMonth = true),
                CalendarDayCell(id = "d-17", day = 17, inMonth = true),
                CalendarDayCell(id = "d-18", day = 18, inMonth = true),
                CalendarDayCell(id = "d-19", day = 19, inMonth = true),
                CalendarDayCell(id = "d-20", day = 20, inMonth = true),
                CalendarDayCell(id = "d-21", day = 21, inMonth = true),
                CalendarDayCell(id = "d-22", day = 22, inMonth = true),
                CalendarDayCell(id = "d-23", day = 23, inMonth = true),
                CalendarDayCell(
                    id = "d-24",
                    day = 24,
                    inMonth = true,
                    isDDay = true,
                    dDayLabel = "D-DAY",
                    textTone = CalendarDayTextTone.OnPrimary,
                    backgroundTone = CalendarDayBackgroundTone.SolidPrimary,
                ),
                CalendarDayCell(id = "d-25", day = 25, inMonth = true),
                CalendarDayCell(id = "d-26", day = 26, inMonth = true),
                CalendarDayCell(id = "d-27", day = 27, inMonth = true),
                CalendarDayCell(id = "d-28", day = 28, inMonth = true),
                CalendarDayCell(id = "d-29", day = 29, inMonth = true),
                CalendarDayCell(id = "d-30", day = 30, inMonth = true),
                CalendarDayCell(id = "d-31", day = 31, inMonth = true),
            ),
            upcoming = listOf(
                UpcomingScheduleItem(
                    id = "up-1",
                    source = UpcomingSource.Event,
                    sourceEntityId = "up-1",
                    date = LocalDate(2023, 10, 14),
                    monthAbbr = "OCT",
                    dayOfMonth = "14",
                    title = "스튜디오 촬영",
                    subtitle = "강남구 루브르 스튜디오 · 오후 2:00",
                    dTagText = "D-10",
                    tagTone = UpcomingTagTone.Primary,
                    isCompleted = false,
                    eventStartTime = "14:00",
                    eventLocation = "강남구 루브르 스튜디오",
                ),
                UpcomingScheduleItem(
                    id = "up-2",
                    source = UpcomingSource.Event,
                    sourceEntityId = "up-2",
                    date = LocalDate(2023, 10, 9),
                    monthAbbr = "OCT",
                    dayOfMonth = "09",
                    title = "청첩장 모임",
                    subtitle = "홍대입구역 3번 출구 · 오후 7:00",
                    dTagText = "D-15",
                    tagTone = UpcomingTagTone.Primary,
                    isCompleted = false,
                    eventStartTime = "19:00",
                    eventLocation = "홍대입구역 3번 출구",
                ),
                UpcomingScheduleItem(
                    id = "up-3",
                    source = UpcomingSource.Checklist,
                    sourceEntityId = "up-3",
                    date = LocalDate(2023, 10, 3),
                    monthAbbr = "OCT",
                    dayOfMonth = "03",
                    title = "드레스 피팅",
                    subtitle = "완료된 일정",
                    dTagText = "D+6",
                    tagTone = UpcomingTagTone.Neutral,
                    isCompleted = true,
                ),
            ),
            upcomingTitle = "다가오는 일정",
            upcomingEmptyText = "예정된 일정이 없어요.",
        )
    }
}

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

        fun sample(): ChecklistUiState = ChecklistUiState(
            groups = listOf(
                ChecklistGroup(
                    id = "d180",
                    dTag = "D-180",
                    title = "상견례 및 웨딩홀",
                    tagStyle = DTagStyle.Primary,
                    items = listOf(
                        ChecklistItem(
                            id = "d180-1",
                            title = "양가 상견례 일정 조율",
                            subtitle = "2023.10.15 완료",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = true,
                        ),
                        ChecklistItem(
                            id = "d180-2",
                            title = "웨딩홀 투어 및 예약",
                            subtitle = "2023.11.01 완료 · 아펠가모",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = true,
                        ),
                    )
                ),
                ChecklistGroup(
                    id = "d100",
                    dTag = "D-100",
                    title = "스튜디오 촬영",
                    tagStyle = DTagStyle.Neutral,
                    items = listOf(
                        ChecklistItem(
                            id = "d100-1",
                            title = "촬영용 드레스 가봉",
                            subtitle = "이번 주말 예정",
                            subtitleTone = ChecklistItemSubtitleTone.Primary,
                            isDone = false,
                        ),
                        ChecklistItem(
                            id = "d100-2",
                            title = "헤어 & 메이크업 상담",
                            subtitle = "청담동 샵 예약 필요",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                        ChecklistItem(
                            id = "d100-3",
                            title = "스튜디오 예약금 입금",
                            subtitle = "완료",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = true,
                        ),
                    )
                ),
                ChecklistGroup(
                    id = "d30",
                    dTag = "D-30",
                    title = "본식 준비",
                    tagStyle = DTagStyle.Neutral,
                    items = listOf(
                        ChecklistItem(
                            id = "d30-1",
                            title = "청첩장 모임 시작",
                            subtitle = "친구 리스트 정리하기",
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                        ChecklistItem(
                            id = "d30-2",
                            title = "부케 선정 및 예약",
                            subtitle = null,
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                        ChecklistItem(
                            id = "d30-3",
                            title = "축가/사회자 최종 확정",
                            subtitle = null,
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                        ChecklistItem(
                            id = "d30-4",
                            title = "본식 리허설 일정 확인",
                            subtitle = null,
                            subtitleTone = ChecklistItemSubtitleTone.Muted,
                            isDone = false,
                        ),
                    )
                ),
            )
        )
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

package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.ui.graphics.Color

data class GuestsUiState(
    val totalGuestsText: String,
    val expectedGiftTotalText: String,
    val searchQuery: String,
    val filters: List<GuestsFilterChip>,
    val sections: List<GuestsSection>,
) {
    companion object {
        fun sample(): GuestsUiState = GuestsUiState(
            totalGuestsText = "120명",
            expectedGiftTotalText = "1,200만",
            searchQuery = "",
            filters = listOf(
                GuestsFilterChip(id = "all", label = "전체", countText = null, selected = true),
                GuestsFilterChip(id = "groom", label = "신랑측", countText = "65", selected = false),
                GuestsFilterChip(id = "bride", label = "신부측", countText = "55", selected = false),
            ),
            sections = listOf(
                GuestsSection(
                    title = "ㄱ",
                    items = listOf(
                        GuestListItem(
                            id = "guest-1",
                            avatarText = "김",
                            avatarContainer = Color(0xFFFDE9EB),
                            avatarContent = Color(0xFFEF3948),
                            name = "김철수",
                            tagText = "신랑 · 친구",
                            note = "식권 1매 발급 완료",
                            giftAmountText = "₩100,000",
                            status = GuestAttendanceStatus.Attending,
                        ),
                        GuestListItem(
                            id = "guest-2",
                            avatarText = "강",
                            avatarContainer = Color(0xFFF3F4F6),
                            avatarContent = Color(0xFF6B7280),
                            name = "강민지",
                            tagText = "신부 · 친척",
                            note = "가족 동반 (2인)",
                            giftAmountText = "₩200,000",
                            status = GuestAttendanceStatus.Attending,
                        ),
                    )
                ),
                GuestsSection(
                    title = "ㅇ",
                    items = listOf(
                        GuestListItem(
                            id = "guest-3",
                            avatarText = "이",
                            avatarContainer = Color(0xFFF8FAFC),
                            avatarContent = Color(0xFF94A3B8),
                            name = "이영희",
                            tagText = "신랑 · 직장",
                            note = "청첩장 발송 완료",
                            giftAmountText = null,
                            status = GuestAttendanceStatus.Undecided,
                        ),
                        GuestListItem(
                            id = "guest-4",
                            avatarText = "오",
                            avatarContainer = Color(0xFFF8FAFC),
                            avatarContent = Color(0xFF94A3B8),
                            name = "오상진",
                            tagText = "신부 · 학교",
                            note = "사유: 해외 출장",
                            giftAmountText = "₩50,000",
                            status = GuestAttendanceStatus.NotAttending,
                            isDimmed = true,
                        ),
                    )
                ),
                GuestsSection(
                    title = "ㅎ",
                    items = listOf(
                        GuestListItem(
                            id = "guest-5",
                            avatarText = "홍",
                            avatarContainer = Color(0xFFF3E8FF),
                            avatarContent = Color(0xFFA855F7),
                            name = "홍길동",
                            tagText = "신랑 · 동창",
                            note = "답례품 수령",
                            giftAmountText = "₩150,000",
                            status = GuestAttendanceStatus.Attending,
                        ),
                    )
                ),
            )
        )
    }
}

data class GuestsFilterChip(
    val id: String,
    val label: String,
    val countText: String?,
    val selected: Boolean,
)

data class GuestsSection(
    val title: String,
    val items: List<GuestListItem>,
)

enum class GuestAttendanceStatus(val label: String) {
    Attending("참석"),
    Undecided("미정"),
    NotAttending("불참"),
}

data class GuestListItem(
    val id: String,
    val avatarText: String,
    val avatarContainer: Color,
    val avatarContent: Color,
    val name: String,
    val tagText: String,
    val note: String?,
    val giftAmountText: String?,
    val status: GuestAttendanceStatus,
    val isDimmed: Boolean = false,
)

sealed interface GuestsAction {
    data class OnSearchQueryChange(val value: String) : GuestsAction
    data class OnSelectFilter(val id: String) : GuestsAction
    data object OnClickAdd : GuestsAction
    data class OnClickGuest(val id: String) : GuestsAction
}


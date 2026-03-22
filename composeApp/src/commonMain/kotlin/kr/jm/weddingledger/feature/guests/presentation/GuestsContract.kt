package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.ui.graphics.Color
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

enum class GuestsTab(val label: String) {
    Expected("예상 하객"),
    GiftLedger("축의대"),
}

enum class SummaryTone {
    Primary,
    Neutral,
    Positive,
    Warning,
}

data class GuestsUiState(
    val selectedTab: GuestsTab,
    val currentUserRole: UserRole? = null,
    val expected: ExpectedGuestsUiState,
    val giftLedger: GiftLedgerUiState,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null,
) {
    companion object {
        fun sample(): GuestsUiState = GuestsUiState(
            selectedTab = GuestsTab.Expected,
            expected = ExpectedGuestsUiState.sample(),
            giftLedger = GiftLedgerUiState.sample(),
        )
    }
}

data class ExpectedGuestsUiState(
    val totalExpectedCountText: String,
    val guaranteedGuestCountText: String,
    val guaranteedDiffText: String,
    val guaranteedDiffTone: SummaryTone,
    val sideSummaries: List<GuestsSideSummaryItem>,
    val statusSummaries: List<ExpectedStatusSummaryItem>,
    val searchQuery: String,
    val sideFilters: List<GuestsFilterChip>,
    val statusFilters: List<GuestsFilterChip>,
    val sections: List<ExpectedGuestSection>,
    val emptyText: String,
) {
    companion object {
        fun sample(): ExpectedGuestsUiState = ExpectedGuestsUiState(
            totalExpectedCountText = "178.5명",
            guaranteedGuestCountText = "170명",
            guaranteedDiffText = "보증 대비 8.5명 초과",
            guaranteedDiffTone = SummaryTone.Positive,
            sideSummaries = listOf(
                GuestsSideSummaryItem(label = "신랑측", primaryText = "92.0명", secondaryText = "꼭 옴 64명"),
                GuestsSideSummaryItem(label = "신부측", primaryText = "84.5명", secondaryText = "꼭 옴 58명"),
                GuestsSideSummaryItem(label = "공통", primaryText = "2.0명", secondaryText = "동반 포함"),
            ),
            statusSummaries = listOf(
                ExpectedStatusSummaryItem(label = "꼭 옴", valueText = "124명", tone = SummaryTone.Primary),
                ExpectedStatusSummaryItem(label = "애매", valueText = "41명", tone = SummaryTone.Warning),
                ExpectedStatusSummaryItem(label = "안 옴", valueText = "36명", tone = SummaryTone.Neutral),
            ),
            searchQuery = "",
            sideFilters = listOf(
                GuestsFilterChip(id = "all", label = "전체", countText = null, selected = true),
                GuestsFilterChip(id = "groom", label = "신랑측", countText = "92.0", selected = false),
                GuestsFilterChip(id = "bride", label = "신부측", countText = "84.5", selected = false),
            ),
            statusFilters = listOf(
                GuestsFilterChip(id = "all", label = "전체", countText = null, selected = true),
                GuestsFilterChip(id = "certain", label = "꼭 옴", countText = "124", selected = false),
                GuestsFilterChip(id = "maybe", label = "애매", countText = "41", selected = false),
                GuestsFilterChip(id = "companions", label = "동반 있음", countText = "28", selected = false),
            ),
            sections = listOf(
                ExpectedGuestSection(
                    title = "꼭 올 하객",
                    items = listOf(
                        ExpectedGuestItem(
                            id = "guest-1",
                            avatarText = "김",
                            avatarContainer = Color(0xFFFDE9EB),
                            avatarContent = Color(0xFFEF3948),
                            name = "김철수",
                            sideLabel = "신랑측",
                            relationshipLabel = "가까운 분",
                            status = GuestExpectedStatus.Certain,
                            statusText = "꼭 옴",
                            companionText = "동반 1명",
                            expectedHeadcountText = "예상 2.0명",
                            note = "식권 2장 준비",
                            isDimmed = false,
                            editorSnapshot = GuestEditorSnapshot(
                                name = "김철수",
                                side = GuestSide.Groom,
                                relationship = GuestRelationshipLevel.Normal,
                                expectedStatus = GuestExpectedStatus.Certain,
                                companionCount = 1,
                                actualAttendance = GiftAttendanceState.Unknown,
                                actualGiftAmount = null,
                                note = "식권 2장 준비",
                            ),
                        )
                    )
                )
            ),
            emptyText = "등록된 예상 하객이 없어요.",
        )
    }
}

data class GiftLedgerUiState(
    val totalGiftText: String,
    val actualAttendingCountText: String,
    val completedEntryCountText: String,
    val pendingEntryCountText: String,
    val sideSummaries: List<GuestsSideSummaryItem>,
    val searchQuery: String,
    val sideFilters: List<GuestsFilterChip>,
    val statusFilters: List<GuestsFilterChip>,
    val sections: List<GiftGuestSection>,
    val emptyText: String,
) {
    companion object {
        fun sample(): GiftLedgerUiState = GiftLedgerUiState(
            totalGiftText = "₩12,800,000",
            actualAttendingCountText = "154명 참석",
            completedEntryCountText = "132명 입력 완료",
            pendingEntryCountText = "22명 미입력",
            sideSummaries = listOf(
                GuestsSideSummaryItem(label = "신랑측", primaryText = "₩6,900,000", secondaryText = "71명 입력"),
                GuestsSideSummaryItem(label = "신부측", primaryText = "₩5,700,000", secondaryText = "59명 입력"),
                GuestsSideSummaryItem(label = "공통", primaryText = "₩200,000", secondaryText = "2명 입력"),
            ),
            searchQuery = "",
            sideFilters = listOf(
                GuestsFilterChip(id = "all", label = "전체", countText = null, selected = true),
                GuestsFilterChip(id = "groom", label = "신랑측", countText = "71", selected = false),
                GuestsFilterChip(id = "bride", label = "신부측", countText = "59", selected = false),
            ),
            statusFilters = listOf(
                GuestsFilterChip(id = "all", label = "전체", countText = null, selected = true),
                GuestsFilterChip(id = "completed", label = "입력 완료", countText = "132", selected = false),
                GuestsFilterChip(id = "pending", label = "미입력", countText = "22", selected = false),
                GuestsFilterChip(id = "attended", label = "참석", countText = "154", selected = false),
            ),
            sections = listOf(
                GiftGuestSection(
                    title = "입력 완료",
                    items = listOf(
                        GiftGuestItem(
                            id = "gift-1",
                            avatarText = "김",
                            avatarContainer = Color(0xFFFDE9EB),
                            avatarContent = Color(0xFFEF3948),
                            name = "김철수",
                            sideLabel = "신랑측",
                            relationshipLabel = "가까운 분",
                            attendanceLabel = "참석",
                            amountText = "₩100,000",
                            note = "모바일 청첩장으로 참석 확인",
                            isPending = false,
                            isDimmed = false,
                            editorSnapshot = GuestEditorSnapshot(
                                name = "김철수",
                                side = GuestSide.Groom,
                                relationship = GuestRelationshipLevel.Normal,
                                expectedStatus = GuestExpectedStatus.Certain,
                                companionCount = 1,
                                actualAttendance = GiftAttendanceState.Attended,
                                actualGiftAmount = 100_000L,
                                note = "모바일 청첩장으로 참석 확인",
                            ),
                        )
                    )
                ),
                GiftGuestSection(
                    title = "미입력",
                    items = listOf(
                        GiftGuestItem(
                            id = "gift-2",
                            avatarText = "박",
                            avatarContainer = Color(0xFFEAF4FF),
                            avatarContent = Color(0xFF2563EB),
                            name = "박민지",
                            sideLabel = "신부측",
                            relationshipLabel = "지인",
                            attendanceLabel = "참석 여부 미기록",
                            amountText = "금액 입력",
                            note = null,
                            isPending = true,
                            isDimmed = false,
                            editorSnapshot = GuestEditorSnapshot(
                                name = "박민지",
                                side = GuestSide.Bride,
                                relationship = GuestRelationshipLevel.Unknown,
                                expectedStatus = GuestExpectedStatus.Maybe,
                                companionCount = 0,
                                actualAttendance = GiftAttendanceState.Unknown,
                                actualGiftAmount = null,
                                note = null,
                            ),
                        )
                    )
                ),
            ),
            emptyText = "축의대에 표시할 하객이 없어요.",
        )
    }
}

data class GuestsSideSummaryItem(
    val label: String,
    val primaryText: String,
    val secondaryText: String,
)

data class ExpectedStatusSummaryItem(
    val label: String,
    val valueText: String,
    val tone: SummaryTone,
)

data class GuestsFilterChip(
    val id: String,
    val label: String,
    val countText: String?,
    val selected: Boolean,
)

data class ExpectedGuestSection(
    val title: String,
    val items: List<ExpectedGuestItem>,
)

data class ExpectedGuestItem(
    val id: String,
    val avatarText: String,
    val avatarContainer: Color,
    val avatarContent: Color,
    val name: String,
    val sideLabel: String,
    val relationshipLabel: String,
    val status: GuestExpectedStatus,
    val statusText: String,
    val companionText: String,
    val expectedHeadcountText: String,
    val note: String?,
    val isDimmed: Boolean,
    val editorSnapshot: GuestEditorSnapshot,
)

data class GiftGuestSection(
    val title: String,
    val items: List<GiftGuestItem>,
)

data class GiftGuestItem(
    val id: String,
    val avatarText: String,
    val avatarContainer: Color,
    val avatarContent: Color,
    val name: String,
    val sideLabel: String,
    val relationshipLabel: String,
    val attendanceLabel: String,
    val amountText: String,
    val note: String?,
    val isPending: Boolean,
    val isDimmed: Boolean,
    val editorSnapshot: GuestEditorSnapshot,
)

data class GuestEditorSnapshot(
    val name: String,
    val side: GuestSide,
    val relationship: GuestRelationshipLevel,
    val expectedStatus: GuestExpectedStatus,
    val companionCount: Int,
    val actualAttendance: GiftAttendanceState,
    val actualGiftAmount: Long?,
    val note: String?,
)

enum class GuestExpectedStatus(val label: String) {
    Certain("꼭 옴"),
    Maybe("애매"),
    NotAttending("안 옴"),
}

enum class GiftAttendanceState(val label: String) {
    Unknown("미기록"),
    Attended("참석"),
    NotAttended("불참"),
}

sealed interface GuestsAction {
    data class OnSelectTab(val tab: GuestsTab) : GuestsAction
    data class OnExpectedSearchQueryChange(val value: String) : GuestsAction
    data class OnGiftSearchQueryChange(val value: String) : GuestsAction
    data class OnSelectExpectedSideFilter(val id: String) : GuestsAction
    data class OnSelectExpectedStatusFilter(val id: String) : GuestsAction
    data class OnSelectGiftSideFilter(val id: String) : GuestsAction
    data class OnSelectGiftStatusFilter(val id: String) : GuestsAction
    data object OnRefresh : GuestsAction
    data object OnClickNotifications : GuestsAction
    data object OnClickAdd : GuestsAction
    data object OnClickEditGuaranteedCount : GuestsAction
    data class OnClickExpectedGuest(val id: String) : GuestsAction
    data class OnClickGiftGuest(val id: String) : GuestsAction
    data class OnQuickSetExpectedStatus(val id: String, val value: GuestExpectedStatus) : GuestsAction
    data class OnCreateGuest(
        val name: String,
        val side: GuestSide,
        val relationship: GuestRelationshipLevel,
        val expectedStatus: GuestExpectedStatus,
        val companionCount: Int,
        val actualAttended: GiftAttendanceState,
        val actualGiftAmount: Long?,
        val note: String?,
    ) : GuestsAction
    data class OnUpdateGuest(
        val id: String,
        val name: String,
        val side: GuestSide,
        val relationship: GuestRelationshipLevel,
        val expectedStatus: GuestExpectedStatus,
        val companionCount: Int,
        val actualAttended: GiftAttendanceState,
        val actualGiftAmount: Long?,
        val note: String?,
    ) : GuestsAction
    data class OnDeleteGuest(val id: String) : GuestsAction
    data class OnUpdateGuaranteedCount(val value: Int) : GuestsAction
    data object OnDismissMessage : GuestsAction
}

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
)

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
)

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
)

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

sealed interface GuestsEffect {
    data object GuestSaved : GuestsEffect
    data object GuestSaveFailed : GuestsEffect
    data object GuaranteedCountSaved : GuestsEffect
    data object GuaranteedCountSaveFailed : GuestsEffect
}

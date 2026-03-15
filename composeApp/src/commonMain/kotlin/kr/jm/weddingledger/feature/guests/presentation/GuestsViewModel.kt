package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.usecase.AddGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.DeleteGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.SetGuestAttendanceUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestUseCase

class GuestsViewModel(
    private val fetchGuestsUseCase: FetchGuestsUseCase,
    private val fetchGuestSettingsUseCase: FetchGuestSettingsUseCase,
    private val updateGuestSettingsUseCase: UpdateGuestSettingsUseCase,
    private val addGuestUseCase: AddGuestUseCase,
    private val updateGuestUseCase: UpdateGuestUseCase,
    private val deleteGuestUseCase: DeleteGuestUseCase,
    private val setGuestAttendanceUseCase: SetGuestAttendanceUseCase,
) : ViewModel() {

    private var allGuests: List<Guest> = emptyList()
    private var guestSettings: GuestSettings = GuestSettings(guaranteedGuestCount = 0)
    private var selectedTab: GuestsTab = GuestsTab.Expected
    private var expectedSearchQuery: String = ""
    private var giftSearchQuery: String = ""
    private var expectedSideFilterId: String = FILTER_ALL
    private var expectedStatusFilterId: String = EXPECTED_FILTER_ALL
    private var giftSideFilterId: String = FILTER_ALL
    private var giftStatusFilterId: String = GIFT_FILTER_ALL

    private val _state = MutableStateFlow(
        GuestsUiState(
            selectedTab = selectedTab,
            expected = emptyExpectedUiState(),
            giftLedger = emptyGiftLedgerUiState(),
            isLoading = false,
        )
    )
    val state: StateFlow<GuestsUiState> = _state.asStateFlow()

    init {
        refresh(showLoading = true)
    }

    fun onAction(action: GuestsAction) {
        when (action) {
            is GuestsAction.OnSelectTab -> {
                selectedTab = action.tab
                applyGuestsToState()
            }
            is GuestsAction.OnExpectedSearchQueryChange -> {
                expectedSearchQuery = action.value
                applyGuestsToState()
            }
            is GuestsAction.OnGiftSearchQueryChange -> {
                giftSearchQuery = action.value
                applyGuestsToState()
            }
            is GuestsAction.OnSelectExpectedSideFilter -> {
                expectedSideFilterId = action.id
                applyGuestsToState()
            }
            is GuestsAction.OnSelectExpectedStatusFilter -> {
                expectedStatusFilterId = action.id
                applyGuestsToState()
            }
            is GuestsAction.OnSelectGiftSideFilter -> {
                giftSideFilterId = action.id
                applyGuestsToState()
            }
            is GuestsAction.OnSelectGiftStatusFilter -> {
                giftStatusFilterId = action.id
                applyGuestsToState()
            }
            GuestsAction.OnRefresh -> refresh(showLoading = false, isRefreshing = true)
            is GuestsAction.OnCreateGuest -> createGuest(action)
            is GuestsAction.OnUpdateGuest -> updateGuest(action)
            is GuestsAction.OnDeleteGuest -> deleteGuest(action.id)
            is GuestsAction.OnQuickSetExpectedStatus -> quickSetExpectedStatus(action.id, action.value)
            is GuestsAction.OnUpdateGuaranteedCount -> updateGuaranteedCount(action.value)
            GuestsAction.OnDismissMessage -> _state.update { it.copy(message = null) }

            GuestsAction.OnClickNotifications,
            GuestsAction.OnClickAdd,
            GuestsAction.OnClickEditGuaranteedCount,
            is GuestsAction.OnClickExpectedGuest,
            is GuestsAction.OnClickGiftGuest,
            -> Unit
        }
    }

    private fun refresh(showLoading: Boolean, isRefreshing: Boolean = false) {
        _state.update {
            it.copy(
                isLoading = showLoading,
                isRefreshing = isRefreshing,
                message = null,
            )
        }

        viewModelScope.launch {
            var message: String? = null

            when (val settingsResult = fetchGuestSettingsUseCase()) {
                is GuestsResult.Success -> guestSettings = settingsResult.data
                is GuestsResult.Error -> message = settingsResult.message
            }

            when (val result = fetchGuestsUseCase()) {
                is GuestsResult.Success -> allGuests = result.data
                is GuestsResult.Error -> message = result.message
            }

            applyGuestsToState(
                message = message,
                isLoading = false,
                isRefreshing = false,
            )
        }
    }

    private fun createGuest(action: GuestsAction.OnCreateGuest) {
        val name = action.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(message = "이름을 입력해주세요.") }
            return
        }

        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            val guest = action.toNewGuest()
            when (val result = addGuestUseCase(guest)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun updateGuest(action: GuestsAction.OnUpdateGuest) {
        val name = action.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(message = "이름을 입력해주세요.") }
            return
        }

        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            val guest = action.toNewGuest()
            when (val result = updateGuestUseCase(action.id, guest)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun deleteGuest(id: String) {
        if (id.isBlank()) {
            _state.update { it.copy(message = "삭제할 하객을 찾지 못했습니다.") }
            return
        }

        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }
        viewModelScope.launch {
            when (val result = deleteGuestUseCase(id)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun quickSetExpectedStatus(id: String, status: GuestExpectedStatus) {
        if (id.isBlank()) return

        viewModelScope.launch {
            when (val result = setGuestAttendanceUseCase(id, status.toDomain())) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    private fun updateGuaranteedCount(value: Int) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }
        viewModelScope.launch {
            when (val result = updateGuestSettingsUseCase(value)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun applyGuestsToState(
        message: String? = _state.value.message,
        isLoading: Boolean = _state.value.isLoading,
        isRefreshing: Boolean = _state.value.isRefreshing,
    ) {
        _state.update {
            it.copy(
                selectedTab = selectedTab,
                expected = buildExpectedUiState(
                    guests = allGuests,
                    guaranteedGuestCount = guestSettings.guaranteedGuestCount,
                    searchQuery = expectedSearchQuery,
                    sideFilterId = expectedSideFilterId,
                    statusFilterId = expectedStatusFilterId,
                ),
                giftLedger = buildGiftLedgerUiState(
                    guests = allGuests,
                    searchQuery = giftSearchQuery,
                    sideFilterId = giftSideFilterId,
                    statusFilterId = giftStatusFilterId,
                ),
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                message = message,
            )
        }
    }
}

private fun buildExpectedUiState(
    guests: List<Guest>,
    guaranteedGuestCount: Int,
    searchQuery: String,
    sideFilterId: String,
    statusFilterId: String,
): ExpectedGuestsUiState {
    val totalExpected = guests.sumOf { it.expectedHeadcount() }
    val diff = totalExpected - guaranteedGuestCount.toDouble()

    val visible = guests
        .filterBySide(sideFilterId)
        .filterByExpectedStatus(statusFilterId)
        .filterBySearch(searchQuery)

    val sections = listOf(
        "꼭 올 하객" to GuestAttendance.Attending,
        "애매한 하객" to GuestAttendance.Undecided,
        "안 오는 하객" to GuestAttendance.NotAttending,
    ).mapNotNull { (title, status) ->
        val items = visible
            .filter { it.attendance == status }
            .sortedBy { it.name }
            .map { it.toExpectedGuestItem() }
        if (items.isEmpty()) null else ExpectedGuestSection(title = title, items = items)
    }

    return ExpectedGuestsUiState(
        totalExpectedCountText = formatHeadcount(totalExpected) + "명",
        guaranteedGuestCountText = "${guaranteedGuestCount}명",
        guaranteedDiffText = when {
            diff > 0 -> "보증 대비 ${formatHeadcount(diff)}명 초과"
            diff < 0 -> "보증 대비 ${formatHeadcount(-diff)}명 부족"
            else -> "보증인원과 같아요"
        },
        guaranteedDiffTone = when {
            diff > 0 -> SummaryTone.Positive
            diff < 0 -> SummaryTone.Warning
            else -> SummaryTone.Neutral
        },
        sideSummaries = buildExpectedSideSummaries(guests),
        statusSummaries = listOf(
            ExpectedStatusSummaryItem(label = "꼭 옴", valueText = "${guests.count { it.attendance == GuestAttendance.Attending }}명", tone = SummaryTone.Primary),
            ExpectedStatusSummaryItem(label = "애매", valueText = "${guests.count { it.attendance == GuestAttendance.Undecided }}명", tone = SummaryTone.Warning),
            ExpectedStatusSummaryItem(label = "안 옴", valueText = "${guests.count { it.attendance == GuestAttendance.NotAttending }}명", tone = SummaryTone.Neutral),
        ),
        searchQuery = searchQuery,
        sideFilters = buildExpectedSideFilters(guests, sideFilterId),
        statusFilters = buildExpectedStatusFilters(guests, statusFilterId),
        sections = sections,
        emptyText = if (guests.isEmpty()) "등록된 예상 하객이 없어요." else "조건에 맞는 예상 하객이 없어요.",
    )
}

private fun buildGiftLedgerUiState(
    guests: List<Guest>,
    searchQuery: String,
    sideFilterId: String,
    statusFilterId: String,
): GiftLedgerUiState {
    val visible = guests
        .filterBySide(sideFilterId)
        .filterByGiftStatus(statusFilterId)
        .filterBySearch(searchQuery)

    val sections = listOf(
        "입력 완료" to true,
        "입력 대기" to false,
    ).mapNotNull { (title, completed) ->
        val items = visible
            .filter { it.isGiftEntryCompleted() == completed }
            .sortedBy { it.name }
            .map { it.toGiftGuestItem() }
        if (items.isEmpty()) null else GiftGuestSection(title = title, items = items)
    }

    val completedCount = guests.count { it.isGiftEntryCompleted() }

    return GiftLedgerUiState(
        totalGiftText = formatKrw(guests.sumOf { it.actualGiftMoney ?: 0L }),
        actualAttendingCountText = "${guests.count { it.actualAttended == true }}명 참석",
        completedEntryCountText = "${completedCount}명 입력 완료",
        pendingEntryCountText = "${(guests.size - completedCount).coerceAtLeast(0)}명 미입력",
        sideSummaries = buildGiftSideSummaries(guests),
        searchQuery = searchQuery,
        sideFilters = buildGiftSideFilters(guests, sideFilterId),
        statusFilters = buildGiftStatusFilters(guests, statusFilterId),
        sections = sections,
        emptyText = if (guests.isEmpty()) "등록된 하객이 없어요." else "조건에 맞는 축의대 항목이 없어요.",
    )
}

private fun buildExpectedSideSummaries(guests: List<Guest>): List<GuestsSideSummaryItem> {
    return listOf(GuestSideType.Groom, GuestSideType.Bride, GuestSideType.Both).mapNotNull { side ->
        val sideGuests = guests.filter { it.side == side }
        if (sideGuests.isEmpty()) return@mapNotNull null
        GuestsSideSummaryItem(
            label = side.toLabel(),
            primaryText = formatHeadcount(sideGuests.sumOf { it.expectedHeadcount() }) + "명",
            secondaryText = "꼭 옴 ${sideGuests.count { it.attendance == GuestAttendance.Attending }}명",
        )
    }
}

private fun buildGiftSideSummaries(guests: List<Guest>): List<GuestsSideSummaryItem> {
    return listOf(GuestSideType.Groom, GuestSideType.Bride, GuestSideType.Both).mapNotNull { side ->
        val sideGuests = guests.filter { it.side == side }
        if (sideGuests.isEmpty()) return@mapNotNull null
        GuestsSideSummaryItem(
            label = side.toLabel(),
            primaryText = formatKrw(sideGuests.sumOf { it.actualGiftMoney ?: 0L }),
            secondaryText = "${sideGuests.count { it.isGiftEntryCompleted() }}명 기록",
        )
    }
}

private fun buildExpectedSideFilters(
    guests: List<Guest>,
    selectedId: String,
): List<GuestsFilterChip> {
    return listOf(
        GuestsFilterChip(id = FILTER_ALL, label = "전체", countText = null, selected = selectedId == FILTER_ALL),
        GuestsFilterChip(id = FILTER_GROOM, label = "신랑측", countText = formatHeadcount(guests.filter { it.side == GuestSideType.Groom }.sumOf { it.expectedHeadcount() }), selected = selectedId == FILTER_GROOM),
        GuestsFilterChip(id = FILTER_BRIDE, label = "신부측", countText = formatHeadcount(guests.filter { it.side == GuestSideType.Bride }.sumOf { it.expectedHeadcount() }), selected = selectedId == FILTER_BRIDE),
        GuestsFilterChip(id = FILTER_BOTH, label = "공통", countText = formatHeadcount(guests.filter { it.side == GuestSideType.Both }.sumOf { it.expectedHeadcount() }), selected = selectedId == FILTER_BOTH),
    )
}

private fun buildExpectedStatusFilters(
    guests: List<Guest>,
    selectedId: String,
): List<GuestsFilterChip> {
    return listOf(
        GuestsFilterChip(id = EXPECTED_FILTER_ALL, label = "전체", countText = null, selected = selectedId == EXPECTED_FILTER_ALL),
        GuestsFilterChip(id = EXPECTED_FILTER_CERTAIN, label = "꼭 옴", countText = guests.count { it.attendance == GuestAttendance.Attending }.toString(), selected = selectedId == EXPECTED_FILTER_CERTAIN),
        GuestsFilterChip(id = EXPECTED_FILTER_MAYBE, label = "애매", countText = guests.count { it.attendance == GuestAttendance.Undecided }.toString(), selected = selectedId == EXPECTED_FILTER_MAYBE),
        GuestsFilterChip(id = EXPECTED_FILTER_NOT_ATTENDING, label = "안 옴", countText = guests.count { it.attendance == GuestAttendance.NotAttending }.toString(), selected = selectedId == EXPECTED_FILTER_NOT_ATTENDING),
        GuestsFilterChip(id = EXPECTED_FILTER_COMPANIONS, label = "동반 있음", countText = guests.count { it.companionCount > 0 }.toString(), selected = selectedId == EXPECTED_FILTER_COMPANIONS),
    )
}

private fun buildGiftSideFilters(
    guests: List<Guest>,
    selectedId: String,
): List<GuestsFilterChip> {
    return listOf(
        GuestsFilterChip(id = FILTER_ALL, label = "전체", countText = null, selected = selectedId == FILTER_ALL),
        GuestsFilterChip(id = FILTER_GROOM, label = "신랑측", countText = guests.count { it.side == GuestSideType.Groom }.toString(), selected = selectedId == FILTER_GROOM),
        GuestsFilterChip(id = FILTER_BRIDE, label = "신부측", countText = guests.count { it.side == GuestSideType.Bride }.toString(), selected = selectedId == FILTER_BRIDE),
        GuestsFilterChip(id = FILTER_BOTH, label = "공통", countText = guests.count { it.side == GuestSideType.Both }.toString(), selected = selectedId == FILTER_BOTH),
    )
}

private fun buildGiftStatusFilters(
    guests: List<Guest>,
    selectedId: String,
): List<GuestsFilterChip> {
    return listOf(
        GuestsFilterChip(id = GIFT_FILTER_ALL, label = "전체", countText = null, selected = selectedId == GIFT_FILTER_ALL),
        GuestsFilterChip(id = GIFT_FILTER_COMPLETED, label = "입력 완료", countText = guests.count { it.isGiftEntryCompleted() }.toString(), selected = selectedId == GIFT_FILTER_COMPLETED),
        GuestsFilterChip(id = GIFT_FILTER_PENDING, label = "미입력", countText = guests.count { !it.isGiftEntryCompleted() }.toString(), selected = selectedId == GIFT_FILTER_PENDING),
        GuestsFilterChip(id = GIFT_FILTER_ATTENDED, label = "참석", countText = guests.count { it.actualAttended == true }.toString(), selected = selectedId == GIFT_FILTER_ATTENDED),
        GuestsFilterChip(id = GIFT_FILTER_NOT_ATTENDED, label = "불참", countText = guests.count { it.actualAttended == false }.toString(), selected = selectedId == GIFT_FILTER_NOT_ATTENDED),
    )
}

private fun Guest.toExpectedGuestItem(): ExpectedGuestItem {
    val visuals = avatarVisualsFor(name)
    return ExpectedGuestItem(
        id = id,
        avatarText = name.firstOrNull()?.toString() ?: "?",
        avatarContainer = visuals.container,
        avatarContent = visuals.content,
        name = name,
        sideLabel = side.toLabel(),
        relationshipLabel = relationshipScore.toRelationshipLabel(),
        status = attendance.toUiExpectedStatus(),
        statusText = attendance.toUiExpectedStatus().label,
        companionText = if (companionCount > 0) "동반 ${companionCount}명" else "동반 없음",
        expectedHeadcountText = "예상 ${formatHeadcount(expectedHeadcount())}명",
        note = note,
        isDimmed = attendance == GuestAttendance.NotAttending,
        editorSnapshot = toEditorSnapshot(),
    )
}

private fun Guest.toGiftGuestItem(): GiftGuestItem {
    val visuals = avatarVisualsFor(name)
    return GiftGuestItem(
        id = id,
        avatarText = name.firstOrNull()?.toString() ?: "?",
        avatarContainer = visuals.container,
        avatarContent = visuals.content,
        name = name,
        sideLabel = side.toLabel(),
        relationshipLabel = relationshipScore.toRelationshipLabel(),
        attendanceLabel = when (actualAttended) {
            true -> "참석"
            false -> "불참"
            null -> "참석 여부 미기록"
        },
        amountText = when {
            actualGiftMoney != null -> formatKrw(actualGiftMoney)
            actualAttended == false -> "불참"
            else -> "금액 입력"
        },
        note = note,
        isPending = !isGiftEntryCompleted(),
        isDimmed = actualAttended == false,
        editorSnapshot = toEditorSnapshot(),
    )
}

private fun List<Guest>.filterBySide(filterId: String): List<Guest> {
    return when (filterId) {
        FILTER_GROOM -> filter { it.side == GuestSideType.Groom }
        FILTER_BRIDE -> filter { it.side == GuestSideType.Bride }
        FILTER_BOTH -> filter { it.side == GuestSideType.Both }
        else -> this
    }
}

private fun List<Guest>.filterByExpectedStatus(filterId: String): List<Guest> {
    return when (filterId) {
        EXPECTED_FILTER_CERTAIN -> filter { it.attendance == GuestAttendance.Attending }
        EXPECTED_FILTER_MAYBE -> filter { it.attendance == GuestAttendance.Undecided }
        EXPECTED_FILTER_NOT_ATTENDING -> filter { it.attendance == GuestAttendance.NotAttending }
        EXPECTED_FILTER_COMPANIONS -> filter { it.companionCount > 0 }
        else -> this
    }
}

private fun List<Guest>.filterByGiftStatus(filterId: String): List<Guest> {
    return when (filterId) {
        GIFT_FILTER_COMPLETED -> filter { it.isGiftEntryCompleted() }
        GIFT_FILTER_PENDING -> filter { !it.isGiftEntryCompleted() }
        GIFT_FILTER_ATTENDED -> filter { it.actualAttended == true }
        GIFT_FILTER_NOT_ATTENDED -> filter { it.actualAttended == false }
        else -> this
    }
}

private fun List<Guest>.filterBySearch(query: String): List<Guest> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this
    return filter { guest ->
        listOf(
            guest.name,
            guest.side.toLabel(),
            guest.relationshipScore.toRelationshipLabel(),
            guest.note.orEmpty(),
        ).joinToString(" ").contains(trimmed, ignoreCase = true)
    }
}

private fun GuestsAction.OnCreateGuest.toNewGuest(): NewGuest = NewGuest(
    name = name.trim(),
    side = side.toDomain(),
    relationshipScore = relationship.toScore(),
    attendance = expectedStatus.toDomain(),
    companionCount = companionCount.coerceAtLeast(0),
    actualAttended = actualAttended.toNullableBoolean(),
    actualGiftMoney = actualGiftAmount?.takeIf { it > 0L },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
)

private fun GuestsAction.OnUpdateGuest.toNewGuest(): NewGuest = NewGuest(
    name = name.trim(),
    side = side.toDomain(),
    relationshipScore = relationship.toScore(),
    attendance = expectedStatus.toDomain(),
    companionCount = companionCount.coerceAtLeast(0),
    actualAttended = actualAttended.toNullableBoolean(),
    actualGiftMoney = actualGiftAmount?.takeIf { it > 0L },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
)

private fun GuestSide.toDomain(): GuestSideType {
    return when (this) {
        GuestSide.Groom -> GuestSideType.Groom
        GuestSide.Bride -> GuestSideType.Bride
        GuestSide.Both -> GuestSideType.Both
    }
}

private fun GuestRelationshipLevel.toScore(): Int {
    return when (this) {
        GuestRelationshipLevel.Unknown -> 0
        GuestRelationshipLevel.Normal -> 1
        GuestRelationshipLevel.Close -> 2
    }
}

private fun Int.toUiRelationship(): GuestRelationshipLevel {
    return when (coerceIn(0, 2)) {
        0 -> GuestRelationshipLevel.Unknown
        2 -> GuestRelationshipLevel.Close
        else -> GuestRelationshipLevel.Normal
    }
}

private fun Int.toRelationshipLabel(): String {
    return when (coerceIn(0, 2)) {
        0 -> "지인"
        2 -> "가족/절친"
        else -> "가까운 분"
    }
}

private fun GuestSideType.toUi(): GuestSide {
    return when (this) {
        GuestSideType.Groom -> GuestSide.Groom
        GuestSideType.Bride -> GuestSide.Bride
        GuestSideType.Both -> GuestSide.Both
    }
}

private fun GuestSideType.toLabel(): String {
    return when (this) {
        GuestSideType.Groom -> "신랑측"
        GuestSideType.Bride -> "신부측"
        GuestSideType.Both -> "공통"
    }
}

private fun GuestAttendance.toUiExpectedStatus(): GuestExpectedStatus {
    return when (this) {
        GuestAttendance.Attending -> GuestExpectedStatus.Certain
        GuestAttendance.Undecided -> GuestExpectedStatus.Maybe
        GuestAttendance.NotAttending -> GuestExpectedStatus.NotAttending
    }
}

private fun GuestExpectedStatus.toDomain(): GuestAttendance {
    return when (this) {
        GuestExpectedStatus.Certain -> GuestAttendance.Attending
        GuestExpectedStatus.Maybe -> GuestAttendance.Undecided
        GuestExpectedStatus.NotAttending -> GuestAttendance.NotAttending
    }
}

private fun GiftAttendanceState.toNullableBoolean(): Boolean? {
    return when (this) {
        GiftAttendanceState.Attended -> true
        GiftAttendanceState.NotAttended -> false
        GiftAttendanceState.Unknown -> null
    }
}

private fun Boolean?.toGiftAttendanceState(): GiftAttendanceState {
    return when (this) {
        true -> GiftAttendanceState.Attended
        false -> GiftAttendanceState.NotAttended
        null -> GiftAttendanceState.Unknown
    }
}

private fun Guest.toEditorSnapshot(): GuestEditorSnapshot {
    return GuestEditorSnapshot(
        name = name,
        side = side.toUi(),
        relationship = relationshipScore.toUiRelationship(),
        expectedStatus = attendance.toUiExpectedStatus(),
        companionCount = companionCount.coerceAtLeast(0),
        actualAttendance = actualAttended.toGiftAttendanceState(),
        actualGiftAmount = actualGiftMoney?.takeIf { it >= 0L },
        note = note,
    )
}

private fun Guest.isGiftEntryCompleted(): Boolean {
    return when {
        actualGiftMoney != null -> true
        actualAttended == false -> true
        else -> false
    }
}

private fun Guest.expectedHeadcount(): Double {
    val partySize = 1 + companionCount.coerceAtLeast(0)
    return when (attendance) {
        GuestAttendance.Attending -> partySize.toDouble()
        GuestAttendance.Undecided -> partySize * 0.5
        GuestAttendance.NotAttending -> 0.0
    }
}

private fun formatHeadcount(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun formatKrw(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    val absText = amount.toString().removePrefix("-")
    val reversed = absText.reversed()
    val withComma = buildString {
        reversed.forEachIndexed { index, c ->
            append(c)
            if ((index + 1) % 3 == 0 && index != reversed.lastIndex) append(',')
        }
    }.reversed()
    return "${sign}₩$withComma"
}

private fun emptyExpectedUiState(): ExpectedGuestsUiState = ExpectedGuestsUiState(
    totalExpectedCountText = "0명",
    guaranteedGuestCountText = "0명",
    guaranteedDiffText = "보증인원을 입력해보세요",
    guaranteedDiffTone = SummaryTone.Neutral,
    sideSummaries = emptyList(),
    statusSummaries = emptyList(),
    searchQuery = "",
    sideFilters = listOf(GuestsFilterChip(id = FILTER_ALL, label = "전체", countText = null, selected = true)),
    statusFilters = listOf(GuestsFilterChip(id = EXPECTED_FILTER_ALL, label = "전체", countText = null, selected = true)),
    sections = emptyList(),
    emptyText = "등록된 예상 하객이 없어요.",
)

private fun emptyGiftLedgerUiState(): GiftLedgerUiState = GiftLedgerUiState(
    totalGiftText = "₩0",
    actualAttendingCountText = "0명 참석",
    completedEntryCountText = "0명 입력 완료",
    pendingEntryCountText = "0명 미입력",
    sideSummaries = emptyList(),
    searchQuery = "",
    sideFilters = listOf(GuestsFilterChip(id = FILTER_ALL, label = "전체", countText = null, selected = true)),
    statusFilters = listOf(GuestsFilterChip(id = GIFT_FILTER_ALL, label = "전체", countText = null, selected = true)),
    sections = emptyList(),
    emptyText = "축의대에 표시할 하객이 없어요.",
)

private data class AvatarVisuals(
    val container: Color,
    val content: Color,
)

private fun avatarVisualsFor(name: String): AvatarVisuals {
    val palettes = listOf(
        AvatarVisuals(container = Color(0xFFFDE9EB), content = Color(0xFFEF3948)),
        AvatarVisuals(container = Color(0xFFEAF4FF), content = Color(0xFF2563EB)),
        AvatarVisuals(container = Color(0xFFF3E8FF), content = Color(0xFFA855F7)),
        AvatarVisuals(container = Color(0xFFE8FFF5), content = Color(0xFF059669)),
        AvatarVisuals(container = Color(0xFFF3F4F6), content = Color(0xFF6B7280)),
    )
    val index = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) } % palettes.size
    return palettes[index]
}

private const val FILTER_ALL = "all"
private const val FILTER_GROOM = "groom"
private const val FILTER_BRIDE = "bride"
private const val FILTER_BOTH = "both"
private const val EXPECTED_FILTER_ALL = "all"
private const val EXPECTED_FILTER_CERTAIN = "certain"
private const val EXPECTED_FILTER_MAYBE = "maybe"
private const val EXPECTED_FILTER_NOT_ATTENDING = "not_attending"
private const val EXPECTED_FILTER_COMPANIONS = "companions"
private const val GIFT_FILTER_ALL = "all"
private const val GIFT_FILTER_COMPLETED = "completed"
private const val GIFT_FILTER_PENDING = "pending"
private const val GIFT_FILTER_ATTENDED = "attended"
private const val GIFT_FILTER_NOT_ATTENDED = "not_attended"

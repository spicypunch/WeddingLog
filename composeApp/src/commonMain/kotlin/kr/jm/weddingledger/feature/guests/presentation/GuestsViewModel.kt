package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.usecase.AddGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.DeleteGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.SetGuestAttendanceUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestUseCase

class GuestsViewModel(
    private val fetchGuestsUseCase: FetchGuestsUseCase,
    private val addGuestUseCase: AddGuestUseCase,
    private val updateGuestUseCase: UpdateGuestUseCase,
    private val deleteGuestUseCase: DeleteGuestUseCase,
    private val setGuestAttendanceUseCase: SetGuestAttendanceUseCase,
) : ViewModel() {

    private var allGuests: List<Guest> = emptyList()
    private var selectedFilterId: String = FILTER_ALL
    private var searchQuery: String = ""

    private val _state = MutableStateFlow(
        GuestsUiState(
            totalGuestsText = "0명",
            expectedGiftTotalText = "₩0",
            expectedAttendanceText = "0명 예상",
            expectedAttendingGiftText = "₩0",
            searchQuery = "",
            filters = buildFilterChips(emptyList(), FILTER_ALL),
            groupSummaries = emptyList(),
            sections = emptyList(),
            isLoading = false,
            message = null,
        )
    )
    val state: StateFlow<GuestsUiState> = _state.asStateFlow()

    init {
        refresh(showLoading = true)
    }

    fun onAction(action: GuestsAction) {
        when (action) {
            is GuestsAction.OnSearchQueryChange -> {
                searchQuery = action.value
                applyGuestsToState()
            }
            is GuestsAction.OnSelectFilter -> {
                selectedFilterId = action.id
                applyGuestsToState()
            }
            GuestsAction.OnRefresh -> refresh(showLoading = true)
            is GuestsAction.OnCreateGuest -> createGuest(action)
            is GuestsAction.OnUpdateGuest -> updateGuest(action)
            is GuestsAction.OnDeleteGuest -> deleteGuest(action.id)
            is GuestsAction.OnQuickSetAttendance -> quickSetAttendance(action.id, action.value)
            GuestsAction.OnDismissMessage -> _state.update { it.copy(message = null) }

            GuestsAction.OnClickAdd,
            is GuestsAction.OnClickGuest,
            -> Unit
        }
    }

    private fun refresh(showLoading: Boolean) {
        _state.update { it.copy(isLoading = showLoading, message = null) }

        viewModelScope.launch {
            when (val result = fetchGuestsUseCase()) {
                is GuestsResult.Success -> {
                    allGuests = result.data
                    applyGuestsToState(message = null, isLoading = false)
                }
                is GuestsResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = result.message) }
                }
            }
        }
    }

    private fun createGuest(action: GuestsAction.OnCreateGuest) {
        val name = action.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(message = "이름을 입력해주세요.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            val guest = NewGuest(
                name = name,
                side = action.side.toDomain(),
                relationshipScore = action.relationship.toScore(),
                expectedGiftMoney = action.expectedGiftAmount?.takeIf { it > 0L },
                attendance = action.attendance.toDomain(),
                note = action.note?.trim()?.takeIf { it.isNotEmpty() },
            )

            when (val result = addGuestUseCase(guest)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun updateGuest(action: GuestsAction.OnUpdateGuest) {
        val name = action.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(message = "이름을 입력해주세요.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            val guest = NewGuest(
                name = name,
                side = action.side.toDomain(),
                relationshipScore = action.relationship.toScore(),
                expectedGiftMoney = action.expectedGiftAmount?.takeIf { it > 0L },
                attendance = action.attendance.toDomain(),
                note = action.note?.trim()?.takeIf { it.isNotEmpty() },
            )

            when (val result = updateGuestUseCase(action.id, guest)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun deleteGuest(id: String) {
        if (id.isBlank()) {
            _state.update { it.copy(message = "삭제할 하객을 찾지 못했습니다.") }
            return
        }

        _state.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = deleteGuestUseCase(id)) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun quickSetAttendance(id: String, status: GuestAttendanceStatus) {
        if (id.isBlank()) return

        viewModelScope.launch {
            when (val result = setGuestAttendanceUseCase(id, status.toDomain())) {
                is GuestsResult.Success -> refresh(showLoading = false)
                is GuestsResult.Error -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    private fun applyGuestsToState(
        message: String? = _state.value.message,
        isLoading: Boolean = _state.value.isLoading,
    ) {
        val filteredBySide = when (selectedFilterId) {
            FILTER_GROOM -> allGuests.filter { it.side == GuestSideType.Groom }
            FILTER_BRIDE -> allGuests.filter { it.side == GuestSideType.Bride }
            FILTER_BOTH -> allGuests.filter { it.side == GuestSideType.Both }
            else -> allGuests
        }

        val query = searchQuery.trim()
        val searched = if (query.isEmpty()) {
            filteredBySide
        } else {
            filteredBySide.filter { guest ->
                val haystack = listOf(
                    guest.name,
                    guest.side.toLabel(),
                    guest.relationshipScore.toRelationshipLabel(),
                    guest.note.orEmpty(),
                ).joinToString(" ")

                haystack.contains(query, ignoreCase = true)
            }
        }

        val grouped = searched
            .sortedBy { it.name }
            .groupBy { it.name.firstOrNull().toSectionKey() }

        val sections = grouped
            .keys
            .sorted()
            .map { title ->
                GuestsSection(
                    title = title,
                    items = grouped[title].orEmpty().map { guest -> guest.toGuestListItem() },
                )
            }

        val totalExpectedGift = allGuests.sumOf { it.expectedGiftMoney ?: 0L }
        val expectedAttendance = allGuests
            .sumOf { guest -> guest.expectedAttendanceProbability() }
            .roundToInt()
        val expectedAttendingGift = allGuests.sumOf { guest ->
            val amount = guest.expectedGiftMoney ?: return@sumOf 0L
            (amount.toDouble() * guest.expectedGiftParticipationProbability()).toLong()
        }

        _state.update {
            it.copy(
                totalGuestsText = "${allGuests.size}명",
                expectedGiftTotalText = formatKrw(totalExpectedGift),
                expectedAttendanceText = "${expectedAttendance}명 예상",
                expectedAttendingGiftText = formatKrw(expectedAttendingGift),
                searchQuery = searchQuery,
                filters = buildFilterChips(allGuests, selectedFilterId),
                groupSummaries = buildGroupSummaries(allGuests),
                sections = sections,
                isLoading = isLoading,
                message = message,
            )
        }
    }
}

private fun buildFilterChips(
    guests: List<Guest>,
    selectedFilterId: String,
): List<GuestsFilterChip> {
    val groomCount = guests.count { it.side == GuestSideType.Groom }
    val brideCount = guests.count { it.side == GuestSideType.Bride }
    val bothCount = guests.count { it.side == GuestSideType.Both }

    return listOf(
        GuestsFilterChip(id = FILTER_ALL, label = "전체", countText = null, selected = selectedFilterId == FILTER_ALL),
        GuestsFilterChip(id = FILTER_GROOM, label = "신랑측", countText = groomCount.toString(), selected = selectedFilterId == FILTER_GROOM),
        GuestsFilterChip(id = FILTER_BRIDE, label = "신부측", countText = brideCount.toString(), selected = selectedFilterId == FILTER_BRIDE),
        GuestsFilterChip(id = FILTER_BOTH, label = "공통", countText = bothCount.toString(), selected = selectedFilterId == FILTER_BOTH),
    )
}

private fun buildGroupSummaries(
    guests: List<Guest>,
): List<GuestGroupSummary> {
    val groups = listOf(
        FILTER_GROOM to GuestSideType.Groom,
        FILTER_BRIDE to GuestSideType.Bride,
        FILTER_BOTH to GuestSideType.Both,
    )

    return groups.mapNotNull { (id, side) ->
        val groupGuests = guests.filter { it.side == side }
        if (groupGuests.isEmpty()) return@mapNotNull null

        val total = groupGuests.size
        val expectedAttendance = groupGuests
            .sumOf { guest -> guest.expectedAttendanceProbability() }
            .roundToInt()
        val expectedGift = groupGuests.sumOf { guest ->
            val amount = guest.expectedGiftMoney ?: return@sumOf 0L
            (amount.toDouble() * guest.expectedGiftParticipationProbability()).toLong()
        }

        GuestGroupSummary(
            id = id,
            label = if (side == GuestSideType.Both) "공통" else "${side.toLabel()}측",
            countText = "${total}명",
            attendanceText = "${expectedAttendance}명 예상",
            giftText = formatKrw(expectedGift),
        )
    }
}

private fun Guest.toGuestListItem(): GuestListItem {
    val visuals = avatarVisualsFor(name)
    val status = attendance.toUi()
    return GuestListItem(
        id = id,
        avatarText = name.firstOrNull()?.toString() ?: "?",
        avatarContainer = visuals.container,
        avatarContent = visuals.content,
        name = name,
        side = side.toUi(),
        relationship = relationshipScore.toUiRelationship(),
        tagText = "${side.toLabel()} · ${relationshipScore.toRelationshipLabel()}",
        note = note ?: status.toDefaultNote(),
        expectedGiftAmount = expectedGiftMoney,
        giftAmountText = expectedGiftMoney?.let(::formatKrw),
        status = status,
        isDimmed = status == GuestAttendanceStatus.NotAttending,
    )
}

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

private fun GuestAttendanceStatus.toDomain(): GuestAttendance {
    return when (this) {
        GuestAttendanceStatus.Attending -> GuestAttendance.Attending
        GuestAttendanceStatus.Undecided -> GuestAttendance.Undecided
        GuestAttendanceStatus.NotAttending -> GuestAttendance.NotAttending
    }
}

private fun GuestAttendance.toUi(): GuestAttendanceStatus {
    return when (this) {
        GuestAttendance.Attending -> GuestAttendanceStatus.Attending
        GuestAttendance.Undecided -> GuestAttendanceStatus.Undecided
        GuestAttendance.NotAttending -> GuestAttendanceStatus.NotAttending
    }
}

private fun GuestSideType.toLabel(): String {
    return when (this) {
        GuestSideType.Groom -> "신랑"
        GuestSideType.Bride -> "신부"
        GuestSideType.Both -> "공통"
    }
}

private fun Int.toRelationshipLabel(): String {
    return when (coerceIn(0, 2)) {
        0 -> "모름"
        2 -> "절친"
        else -> "보통"
    }
}

private fun Int.toUiRelationship(): GuestRelationshipLevel {
    return when (coerceIn(0, 2)) {
        0 -> GuestRelationshipLevel.Unknown
        2 -> GuestRelationshipLevel.Close
        else -> GuestRelationshipLevel.Normal
    }
}

private fun GuestSideType.toUi(): GuestSide {
    return when (this) {
        GuestSideType.Groom -> GuestSide.Groom
        GuestSideType.Bride -> GuestSide.Bride
        GuestSideType.Both -> GuestSide.Both
    }
}

private fun GuestAttendanceStatus.toDefaultNote(): String? {
    return when (this) {
        GuestAttendanceStatus.Attending -> null
        GuestAttendanceStatus.Undecided -> "참석 여부 확인 필요"
        GuestAttendanceStatus.NotAttending -> "불참"
    }
}

private fun Guest.expectedAttendanceProbability(): Double {
    return when (attendance) {
        GuestAttendance.Attending -> 1.0
        GuestAttendance.NotAttending -> 0.0
        GuestAttendance.Undecided -> relationshipScore.toUndecidedAttendanceProbability()
    }
}

private fun Guest.expectedGiftParticipationProbability(): Double {
    return when (attendance) {
        GuestAttendance.Attending -> 1.0
        GuestAttendance.NotAttending -> 0.0
        GuestAttendance.Undecided -> {
            relationshipScore.toUndecidedAttendanceProbability() *
                relationshipScore.toUndecidedGiftConversionMultiplier()
        }
    }
}

private fun Int.toUndecidedAttendanceProbability(): Double {
    return when (coerceIn(0, 2)) {
        0 -> 0.45
        2 -> 0.80
        else -> 0.62
    }
}

private fun Int.toUndecidedGiftConversionMultiplier(): Double {
    return when (coerceIn(0, 2)) {
        0 -> 0.60
        2 -> 0.95
        else -> 0.82
    }
}

private fun Char?.toSectionKey(): String {
    if (this == null) return "#"
    val code = this.code
    if (code in HANGUL_START..HANGUL_END) {
        val choseongIndex = (code - HANGUL_START) / HANGUL_CYCLE
        return HANGUL_INITIALS[choseongIndex].toString()
    }
    return this.uppercaseChar().toString()
}

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
    val index = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) } % palettes.size
    return palettes[index]
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

private const val FILTER_ALL = "all"
private const val FILTER_GROOM = "groom"
private const val FILTER_BRIDE = "bride"
private const val FILTER_BOTH = "both"

private const val HANGUL_START = 0xAC00
private const val HANGUL_END = 0xD7A3
private const val HANGUL_CYCLE = 21 * 28
private val HANGUL_INITIALS = listOf(
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
    'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
)

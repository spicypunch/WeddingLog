package kr.jm.weddingledger.feature.guests.presentation

enum class GuestSide(val label: String) {
    Groom("신랑측"),
    Bride("신부측"),
    Both("공통"),
}

enum class GuestRelationshipLevel(val label: String) {
    Unknown("지인"),
    Normal("가까운 분"),
    Close("가족/절친"),
}

enum class GuestEditorMode(val title: String, val saveLabel: String) {
    Expected("예상 하객", "저장하기"),
    GiftLedger("축의대", "저장하기"),
}

data class AddGuestUiState(
    val mode: GuestEditorMode,
    val name: String,
    val side: GuestSide,
    val relationship: GuestRelationshipLevel,
    val expectedStatus: GuestExpectedStatus,
    val companionCount: String,
    val actualAttendance: GiftAttendanceState,
    val actualGiftAmount: String,
    val note: String,
) {
    val isSaveEnabled: Boolean
        get() = name.trim().isNotEmpty()

    companion object {
        fun expectedSample(): AddGuestUiState = AddGuestUiState(
            mode = GuestEditorMode.Expected,
            name = "",
            side = GuestSide.Bride,
            relationship = GuestRelationshipLevel.Normal,
            expectedStatus = GuestExpectedStatus.Maybe,
            companionCount = "0",
            actualAttendance = GiftAttendanceState.Unknown,
            actualGiftAmount = "",
            note = "",
        )

        fun giftSample(): AddGuestUiState = AddGuestUiState(
            mode = GuestEditorMode.GiftLedger,
            name = "",
            side = GuestSide.Bride,
            relationship = GuestRelationshipLevel.Normal,
            expectedStatus = GuestExpectedStatus.Certain,
            companionCount = "0",
            actualAttendance = GiftAttendanceState.Attended,
            actualGiftAmount = "",
            note = "",
        )
    }
}

sealed interface AddGuestAction {
    data object OnClickClose : AddGuestAction
    data object OnClickSave : AddGuestAction
    data class OnNameChange(val value: String) : AddGuestAction
    data class OnSelectSide(val side: GuestSide) : AddGuestAction
    data class OnSelectRelationship(val level: GuestRelationshipLevel) : AddGuestAction
    data class OnSelectExpectedStatus(val value: GuestExpectedStatus) : AddGuestAction
    data class OnCompanionCountChange(val value: String) : AddGuestAction
    data class OnSelectActualAttendance(val value: GiftAttendanceState) : AddGuestAction
    data class OnActualGiftAmountChange(val value: String) : AddGuestAction
    data class OnNoteChange(val value: String) : AddGuestAction
}

package kr.jm.weddingledger.feature.guests.presentation

enum class GuestSide(val label: String) {
    Groom("신랑"),
    Bride("신부"),
    Both("공통"),
}

enum class GuestRelationshipLevel(val label: String) {
    Unknown("모름"),
    Normal("보통"),
    Close("절친"),
}

data class AddGuestUiState(
    val name: String,
    val side: GuestSide,
    val relationship: GuestRelationshipLevel,
    val expectedGiftAmount: String,
    val note: String,
    val quickAddAmounts: List<String>,
    val attendance: GuestAttendanceStatus,
) {
    val isSaveEnabled: Boolean
        get() = name.trim().isNotEmpty()

    companion object {
        fun sample(): AddGuestUiState = AddGuestUiState(
            name = "",
            side = GuestSide.Bride,
            relationship = GuestRelationshipLevel.Normal,
            expectedGiftAmount = "50,000",
            note = "",
            quickAddAmounts = listOf("30,000", "50,000", "100,000"),
            attendance = GuestAttendanceStatus.Undecided,
        )
    }
}

sealed interface AddGuestAction {
    data object OnClickClose : AddGuestAction
    data object OnClickSave : AddGuestAction
    data class OnNameChange(val value: String) : AddGuestAction
    data class OnSelectSide(val side: GuestSide) : AddGuestAction
    data class OnSelectRelationship(val level: GuestRelationshipLevel) : AddGuestAction
    data class OnExpectedGiftAmountChange(val value: String) : AddGuestAction
    data class OnQuickAddExpectedGift(val value: String) : AddGuestAction
    data class OnSelectAttendance(val value: GuestAttendanceStatus) : AddGuestAction
    data class OnNoteChange(val value: String) : AddGuestAction
}

package kr.jm.weddingledger.feature.guests.domain.model

data class Guest(
    val id: String,
    val name: String,
    val side: GuestSideType,
    val relationshipScore: Int,
    val attendance: GuestAttendance,
    val companionCount: Int,
    val actualAttended: Boolean?,
    val actualGiftMoney: Long?,
    val note: String?,
)

enum class GuestSideType {
    Groom,
    Bride,
    Both,
}

enum class GuestAttendance {
    Attending,
    Undecided,
    NotAttending,
}

data class NewGuest(
    val name: String,
    val side: GuestSideType,
    val relationshipScore: Int,
    val attendance: GuestAttendance,
    val companionCount: Int,
    val actualAttended: Boolean?,
    val actualGiftMoney: Long?,
    val note: String?,
)

data class GuestSettings(
    val guaranteedGuestCount: Int,
)

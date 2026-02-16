package kr.jm.weddingledger.feature.guests.domain.model

data class Guest(
    val id: String,
    val name: String,
    val side: GuestSideType,
    val relationshipScore: Int,
    val expectedGiftMoney: Long?,
    val attendance: GuestAttendance,
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
    val expectedGiftMoney: Long?,
    val attendance: GuestAttendance,
    val note: String?,
)

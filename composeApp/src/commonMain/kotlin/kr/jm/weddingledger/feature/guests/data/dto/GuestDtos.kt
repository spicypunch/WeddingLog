package kr.jm.weddingledger.feature.guests.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String? = null,
    val name: String,
    @SerialName("group")
    val group: String? = null,
    @SerialName("relationship_score")
    val relationshipScore: Int? = null,
    @SerialName("expected_gift_money")
    val expectedGiftMoney: Long? = null,
    @SerialName("is_attended")
    val isAttended: Boolean? = null,
    @SerialName("attendance_likelihood")
    val attendanceLikelihood: String? = null,
    @SerialName("companion_count")
    val companionCount: Int? = null,
    @SerialName("actual_attended")
    val actualAttended: Boolean? = null,
    @SerialName("actual_gift_money")
    val actualGiftMoney: Long? = null,
    val note: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class GuestInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    val name: String,
    @SerialName("group")
    val group: String,
    @SerialName("relationship_score")
    val relationshipScore: Int,
    @SerialName("expected_gift_money")
    val expectedGiftMoney: Long? = null,
    @SerialName("is_attended")
    val isAttended: Boolean? = null,
    @SerialName("attendance_likelihood")
    val attendanceLikelihood: String,
    @SerialName("companion_count")
    val companionCount: Int,
    @SerialName("actual_attended")
    val actualAttended: Boolean? = null,
    @SerialName("actual_gift_money")
    val actualGiftMoney: Long? = null,
    val note: String? = null,
)

@Serializable
data class GuestSettingsRowDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("guaranteed_guest_count")
    val guaranteedGuestCount: Int? = null,
)

@Serializable
data class GuestSettingsUpsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("guaranteed_guest_count")
    val guaranteedGuestCount: Int,
)

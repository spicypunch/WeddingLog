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
    val note: String? = null,
)

@Serializable
data class ProfileRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String? = null,
)

@Serializable
data class ProfileInsertDto(
    val id: String,
    val email: String? = null,
    @SerialName("wedding_id")
    val weddingId: String,
)

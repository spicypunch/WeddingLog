package kr.jm.weddingledger.feature.schedule.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRowDto(
    val id: String,
    val email: String? = null,
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


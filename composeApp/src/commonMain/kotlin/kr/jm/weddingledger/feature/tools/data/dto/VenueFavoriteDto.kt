package kr.jm.weddingledger.feature.tools.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VenueFavoriteRowDto(
    @SerialName("venue_id")
    val venueId: String,
)

@Serializable
data class VenueFavoriteInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("venue_id")
    val venueId: String,
)


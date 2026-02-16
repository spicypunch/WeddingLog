package kr.jm.weddingledger.feature.tools.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VenueRowDto(
    val id: String,
    val name: String,
    val location: String,
    @SerialName("price_per_guest")
    val pricePerGuest: Int,
    val rating: Double,
    val capacity: Int,
    val tags: List<String> = emptyList(),
    @SerialName("accent_hex")
    val accentHex: String? = null,
)


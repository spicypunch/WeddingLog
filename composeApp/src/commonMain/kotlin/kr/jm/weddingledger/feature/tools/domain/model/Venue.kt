package kr.jm.weddingledger.feature.tools.domain.model

data class Venue(
    val id: String,
    val name: String,
    val location: String,
    val pricePerGuest: Int,
    val rating: Double,
    val capacity: Int,
    val tags: List<String>,
    val accentHex: Long,
)


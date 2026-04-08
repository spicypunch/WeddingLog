package kr.jm.weddingledger.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteAccountRequestDto(
    val provider: String,
    @SerialName("apple_authorization_code")
    val appleAuthorizationCode: String? = null,
)

package kr.jm.weddingledger.feature.onboarding.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRowDto(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("wedding_id")
    val weddingId: String? = null,
    val role: String? = null,
    @SerialName("wedding_date")
    val weddingDate: String? = null,
    @SerialName("is_wedding_date_undecided")
    val isWeddingDateUndecided: Boolean = false,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    @SerialName("onboarding_completed")
    val onboardingCompleted: Boolean = false,
)

@Serializable
data class ProfileInsertDto(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("wedding_id")
    val weddingId: String,
)


package kr.jm.weddingledger.feature.onboarding.domain.model

import kotlinx.datetime.LocalDate

enum class UserRole(val label: String) {
    Groom("신랑"),
    Bride("신부"),
}

data class MyProfile(
    val id: String,
    val email: String?,
    val nickname: String?,
    val role: UserRole?,
    val weddingId: String?,
    val weddingDate: LocalDate?,
    val isWeddingDateUndecided: Boolean,
    val inviteCode: String?,
    val onboardingCompleted: Boolean,
)

enum class OnboardingStep {
    Loading,
    EntryChoice,
    ProfileSetup,
    InviteShare,
    InviteJoin,
    Done,
}

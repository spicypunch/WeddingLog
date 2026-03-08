package kr.jm.weddingledger.feature.onboarding.domain.model

enum class OnboardingErrorType {
    ReauthenticationRequired,
    Unknown,
}

sealed interface OnboardingResult<out T> {
    data class Success<T>(val data: T) : OnboardingResult<T>
    data class Error(
        val message: String,
        val type: OnboardingErrorType = OnboardingErrorType.Unknown,
    ) : OnboardingResult<Nothing>
}

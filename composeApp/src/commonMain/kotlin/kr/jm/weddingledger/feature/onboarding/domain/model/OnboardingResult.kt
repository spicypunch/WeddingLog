package kr.jm.weddingledger.feature.onboarding.domain.model

sealed interface OnboardingResult<out T> {
    data class Success<T>(val data: T) : OnboardingResult<T>
    data class Error(val message: String) : OnboardingResult<Nothing>
}


package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class FetchGuestSettingsUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke() = repository.fetchGuestSettings()
}

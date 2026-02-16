package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class AddGuestUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke(guest: NewGuest) = repository.addGuest(guest)
}

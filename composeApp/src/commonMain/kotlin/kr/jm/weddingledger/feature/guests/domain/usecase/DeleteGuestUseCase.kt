package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class DeleteGuestUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke(id: String): GuestsResult<Unit> {
        if (id.isBlank()) return GuestsResult.Error("삭제할 하객을 찾지 못했습니다.")
        return repository.deleteGuest(id)
    }
}

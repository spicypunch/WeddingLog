package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class UpdateGuestUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke(id: String, guest: NewGuest): GuestsResult<Unit> {
        if (id.isBlank()) return GuestsResult.Error("수정할 하객을 찾지 못했습니다.")
        if (guest.name.isBlank()) return GuestsResult.Error("이름을 입력해주세요.")
        return repository.updateGuest(id, guest)
    }
}

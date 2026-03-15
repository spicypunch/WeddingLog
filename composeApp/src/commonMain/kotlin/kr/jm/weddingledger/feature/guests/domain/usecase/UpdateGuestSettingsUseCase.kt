package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class UpdateGuestSettingsUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke(guaranteedGuestCount: Int): GuestsResult<Unit> {
        if (guaranteedGuestCount < 0) return GuestsResult.Error("보증인원은 0명 이상이어야 합니다.")
        return repository.updateGuestSettings(GuestSettings(guaranteedGuestCount = guaranteedGuestCount))
    }
}

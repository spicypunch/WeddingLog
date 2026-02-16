package kr.jm.weddingledger.feature.guests.domain.usecase

import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class SetGuestAttendanceUseCase(
    private val repository: GuestRepository,
) {
    suspend operator fun invoke(id: String, attendance: GuestAttendance): GuestsResult<Unit> {
        if (id.isBlank()) return GuestsResult.Error("하객을 찾지 못했습니다.")
        return repository.setAttendance(id = id, attendance = attendance)
    }
}

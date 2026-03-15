package kr.jm.weddingledger.feature.guests.domain.repository

import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest

interface GuestRepository {
    suspend fun fetchGuests(): GuestsResult<List<Guest>>
    suspend fun fetchGuestSettings(): GuestsResult<GuestSettings>
    suspend fun updateGuestSettings(settings: GuestSettings): GuestsResult<Unit>
    suspend fun addGuest(guest: NewGuest): GuestsResult<Unit>
    suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit>
    suspend fun deleteGuest(id: String): GuestsResult<Unit>
    suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit>
}

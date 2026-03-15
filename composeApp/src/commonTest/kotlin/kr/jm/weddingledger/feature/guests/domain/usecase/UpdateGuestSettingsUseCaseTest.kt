package kr.jm.weddingledger.feature.guests.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class UpdateGuestSettingsUseCaseTest {

    @Test
    fun `returns error when guaranteed guest count is negative`() = runBlocking {
        val repository = FakeGuestSettingsRepository()
        val useCase = UpdateGuestSettingsUseCase(repository)

        val result = useCase(-1)

        val error = assertIs<GuestsResult.Error>(result)
        assertEquals("보증인원은 0명 이상이어야 합니다.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `delegates guaranteed guest count to repository`() = runBlocking {
        val repository = FakeGuestSettingsRepository()
        val useCase = UpdateGuestSettingsUseCase(repository)

        val result = useCase(180)

        assertIs<GuestsResult.Success<Unit>>(result)
        assertEquals(1, repository.updateCallCount)
        assertEquals(180, repository.lastSettings?.guaranteedGuestCount)
    }
}

private class FakeGuestSettingsRepository : GuestRepository {
    var updateCallCount: Int = 0
    var lastSettings: GuestSettings? = null

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> = GuestsResult.Success(emptyList())

    override suspend fun fetchGuestSettings(): GuestsResult<GuestSettings> = GuestsResult.Success(GuestSettings(0))

    override suspend fun updateGuestSettings(settings: GuestSettings): GuestsResult<Unit> {
        updateCallCount += 1
        lastSettings = settings
        return GuestsResult.Success(Unit)
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun deleteGuest(id: String): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit> =
        GuestsResult.Success(Unit)
}

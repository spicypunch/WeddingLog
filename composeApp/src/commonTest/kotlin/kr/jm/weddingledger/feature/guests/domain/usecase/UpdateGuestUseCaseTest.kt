package kr.jm.weddingledger.feature.guests.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class UpdateGuestUseCaseTest {

    @Test
    fun `returns error when id is blank`() = runBlocking {
        val repository = FakeGuestRepository()
        val useCase = UpdateGuestUseCase(repository)

        val result = useCase(
            id = " ",
            guest = sampleGuest(name = "하객"),
        )

        val error = assertIs<GuestsResult.Error>(result)
        assertEquals("수정할 하객을 찾지 못했습니다.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `returns error when name is blank`() = runBlocking {
        val repository = FakeGuestRepository()
        val useCase = UpdateGuestUseCase(repository)

        val result = useCase(
            id = "guest-1",
            guest = sampleGuest(name = " "),
        )

        val error = assertIs<GuestsResult.Error>(result)
        assertEquals("이름을 입력해주세요.", error.message)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `delegates to repository when payload is valid`() = runBlocking {
        val repository = FakeGuestRepository()
        val useCase = UpdateGuestUseCase(repository)
        val expected = sampleGuest(name = "민지")

        val result = useCase(id = "guest-1", guest = expected)

        assertIs<GuestsResult.Success<Unit>>(result)
        assertEquals(1, repository.updateCallCount)
        assertEquals("guest-1", repository.lastUpdatedId)
        assertEquals(expected, repository.lastUpdatedGuest)
    }
}

private class FakeGuestRepository : GuestRepository {
    var updateCallCount: Int = 0
    var lastUpdatedId: String? = null
    var lastUpdatedGuest: NewGuest? = null

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> = GuestsResult.Success(emptyList())

    override suspend fun fetchGuestSettings(): GuestsResult<GuestSettings> = GuestsResult.Success(GuestSettings(0))

    override suspend fun updateGuestSettings(settings: GuestSettings): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit> {
        updateCallCount += 1
        lastUpdatedId = id
        lastUpdatedGuest = guest
        return GuestsResult.Success(Unit)
    }

    override suspend fun deleteGuest(id: String): GuestsResult<Unit> = GuestsResult.Success(Unit)

    override suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit> =
        GuestsResult.Success(Unit)
}

private fun sampleGuest(name: String): NewGuest = NewGuest(
    name = name,
    side = GuestSideType.Bride,
    relationshipScore = 2,
    attendance = GuestAttendance.Attending,
    companionCount = 1,
    actualAttended = true,
    actualGiftMoney = 100_000L,
    note = "친구",
)

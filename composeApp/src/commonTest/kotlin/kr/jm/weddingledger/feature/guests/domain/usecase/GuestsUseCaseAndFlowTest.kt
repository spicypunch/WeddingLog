package kr.jm.weddingledger.feature.guests.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository

class GuestsUseCaseAndFlowTest {

    @Test
    fun `add guest delegates payload to repository`() = runBlocking {
        val repository = InMemoryGuestRepository()
        val useCase = AddGuestUseCase(repository)
        val payload = newGuest(name = "김민수")

        val result = useCase(payload)

        assertIs<GuestsResult.Success<Unit>>(result)
        assertEquals(1, repository.addCallCount)
        assertEquals(payload, repository.lastAddedGuest)
    }

    @Test
    fun `fetch guests returns repository data`() = runBlocking {
        val repository = InMemoryGuestRepository()
        repository.addGuest(newGuest(name = "신부 친구"))
        val useCase = FetchGuestsUseCase(repository)

        val result = useCase()

        val success = assertIs<GuestsResult.Success<List<Guest>>>(result)
        assertEquals(1, success.data.size)
        assertEquals("신부 친구", success.data.first().name)
    }

    @Test
    fun `delete guest validates blank id`() = runBlocking {
        val repository = InMemoryGuestRepository()
        val useCase = DeleteGuestUseCase(repository)

        val result = useCase(" ")

        val error = assertIs<GuestsResult.Error>(result)
        assertEquals("삭제할 하객을 찾지 못했습니다.", error.message)
        assertEquals(0, repository.deleteCallCount)
    }

    @Test
    fun `set attendance validates blank id`() = runBlocking {
        val repository = InMemoryGuestRepository()
        val useCase = SetGuestAttendanceUseCase(repository)

        val result = useCase(" ", GuestAttendance.Attending)

        val error = assertIs<GuestsResult.Error>(result)
        assertEquals("하객을 찾지 못했습니다.", error.message)
        assertEquals(0, repository.setAttendanceCallCount)
    }

    @Test
    fun `integration flow add update attendance delete guest`() = runBlocking {
        val repository = InMemoryGuestRepository()
        val addGuestUseCase = AddGuestUseCase(repository)
        val fetchGuestsUseCase = FetchGuestsUseCase(repository)
        val updateGuestUseCase = UpdateGuestUseCase(repository)
        val setGuestAttendanceUseCase = SetGuestAttendanceUseCase(repository)
        val deleteGuestUseCase = DeleteGuestUseCase(repository)

        addGuestUseCase(newGuest(name = "신랑 친구"))
        addGuestUseCase(newGuest(name = "신부 친구"))

        val created = assertIs<GuestsResult.Success<List<Guest>>>(fetchGuestsUseCase()).data
        assertEquals(2, created.size)

        val firstGuestId = created.first().id
        updateGuestUseCase(
            id = firstGuestId,
            guest = newGuest(name = "신랑 절친", attendance = GuestAttendance.Undecided),
        )
        setGuestAttendanceUseCase(firstGuestId, GuestAttendance.NotAttending)

        val updated = assertIs<GuestsResult.Success<List<Guest>>>(fetchGuestsUseCase()).data
            .first { it.id == firstGuestId }
        assertEquals("신랑 절친", updated.name)
        assertEquals(GuestAttendance.NotAttending, updated.attendance)

        deleteGuestUseCase(firstGuestId)

        val afterDelete = assertIs<GuestsResult.Success<List<Guest>>>(fetchGuestsUseCase()).data
        assertEquals(1, afterDelete.size)
    }
}

private class InMemoryGuestRepository : GuestRepository {
    private val guests = linkedMapOf<String, Guest>()
    private var seed = 1

    var addCallCount: Int = 0
    var deleteCallCount: Int = 0
    var setAttendanceCallCount: Int = 0
    var lastAddedGuest: NewGuest? = null

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> {
        return GuestsResult.Success(guests.values.toList())
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> {
        addCallCount += 1
        lastAddedGuest = guest
        val id = "guest-${seed++}"
        guests[id] = Guest(
            id = id,
            name = guest.name,
            side = guest.side,
            relationshipScore = guest.relationshipScore,
            expectedGiftMoney = guest.expectedGiftMoney,
            attendance = guest.attendance,
            note = guest.note,
        )
        return GuestsResult.Success(Unit)
    }

    override suspend fun updateGuest(id: String, guest: NewGuest): GuestsResult<Unit> {
        val current = guests[id] ?: return GuestsResult.Error("하객을 찾지 못했습니다.")
        guests[id] = current.copy(
            name = guest.name,
            side = guest.side,
            relationshipScore = guest.relationshipScore,
            expectedGiftMoney = guest.expectedGiftMoney,
            attendance = guest.attendance,
            note = guest.note,
        )
        return GuestsResult.Success(Unit)
    }

    override suspend fun deleteGuest(id: String): GuestsResult<Unit> {
        deleteCallCount += 1
        guests.remove(id)
        return GuestsResult.Success(Unit)
    }

    override suspend fun setAttendance(id: String, attendance: GuestAttendance): GuestsResult<Unit> {
        setAttendanceCallCount += 1
        val current = guests[id] ?: return GuestsResult.Error("하객을 찾지 못했습니다.")
        guests[id] = current.copy(attendance = attendance)
        return GuestsResult.Success(Unit)
    }
}

private fun newGuest(
    name: String,
    attendance: GuestAttendance = GuestAttendance.Attending,
): NewGuest {
    return NewGuest(
        name = name,
        side = GuestSideType.Groom,
        relationshipScore = 2,
        expectedGiftMoney = 100_000L,
        attendance = attendance,
        note = null,
    )
}

package kr.jm.weddingledger.feature.guests.presentation

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository
import kr.jm.weddingledger.feature.guests.domain.usecase.AddGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.DeleteGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.SetGuestAttendanceUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class GuestsViewModelTest {

    @Test
    fun `initial load calculates summary texts and section count`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김하나", side = GuestSideType.Groom, attendance = GuestAttendance.Attending, expectedGift = 100_000L),
                guest(id = "g2", name = "박둘", side = GuestSideType.Bride, attendance = GuestAttendance.NotAttending, expectedGift = 50_000L),
                guest(id = "g3", name = "Alice", side = GuestSideType.Both, attendance = GuestAttendance.Undecided, expectedGift = 100_000L, relationship = 2),
            )
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("3명", state.totalGuestsText)
        assertEquals("₩250,000", state.expectedGiftTotalText)
        assertEquals("2명 예상", state.expectedAttendanceText)
        assertEquals("₩176,000", state.expectedAttendingGiftText)
        assertEquals(4, state.filters.size)
        assertTrue(state.sections.isNotEmpty())
    }

    @Test
    fun `search and side filter reduce visible guests`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김민수", side = GuestSideType.Groom, note = "회사 친구"),
                guest(id = "g2", name = "이수진", side = GuestSideType.Bride, note = "신부 가족"),
                guest(id = "g3", name = "공통지인", side = GuestSideType.Both, note = "대학 동기"),
            )
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(GuestsAction.OnSearchQueryChange("친구"))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.sections.sumOf { it.items.size })
        assertEquals("김민수", state.sections.first().items.first().name)

        val groomFilterId = state.filters.first { it.label == "신랑측" }.id
        viewModel.onAction(GuestsAction.OnSelectFilter(groomFilterId))
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(1, state.sections.sumOf { it.items.size })
        assertEquals("김민수", state.sections.first().items.first().name)
    }

    @Test
    fun `create guest with blank name shows validation message`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(initialGuests = emptyList())
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        viewModel.onAction(
            GuestsAction.OnCreateGuest(
                name = " ",
                side = GuestSide.Bride,
                relationship = GuestRelationshipLevel.Normal,
                expectedGiftAmount = null,
                attendance = GuestAttendanceStatus.Undecided,
                note = null,
            )
        )

        val state = viewModel.state.value
        assertEquals("이름을 입력해주세요.", state.message)
        assertEquals(0, repository.addCallCount)
    }

    @Test
    fun `quick set attendance updates repository and reflects ui`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김민수", side = GuestSideType.Groom, attendance = GuestAttendance.Undecided),
            )
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        viewModel.onAction(
            GuestsAction.OnQuickSetAttendance(
                id = "g1",
                value = GuestAttendanceStatus.NotAttending,
            )
        )
        advanceUntilIdle()

        val status = viewModel.state.value.sections.first().items.first().status
        assertEquals(GuestAttendanceStatus.NotAttending, status)
        assertEquals(1, repository.setAttendanceCallCount)
    }
}

private fun createViewModel(repository: FakeGuestRepositoryForViewModel): GuestsViewModel {
    return GuestsViewModel(
        fetchGuestsUseCase = FetchGuestsUseCase(repository),
        addGuestUseCase = AddGuestUseCase(repository),
        updateGuestUseCase = UpdateGuestUseCase(repository),
        deleteGuestUseCase = DeleteGuestUseCase(repository),
        setGuestAttendanceUseCase = SetGuestAttendanceUseCase(repository),
    )
}

private class FakeGuestRepositoryForViewModel(
    initialGuests: List<Guest>,
) : GuestRepository {
    private val guests = linkedMapOf<String, Guest>().apply {
        initialGuests.forEach { guest -> put(guest.id, guest) }
    }

    var addCallCount: Int = 0
    var setAttendanceCallCount: Int = 0

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> {
        return GuestsResult.Success(guests.values.toList())
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> {
        addCallCount += 1
        val id = "guest-${guests.size + 1}"
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

private fun guest(
    id: String,
    name: String,
    side: GuestSideType,
    attendance: GuestAttendance = GuestAttendance.Attending,
    expectedGift: Long? = 100_000L,
    relationship: Int = 1,
    note: String? = null,
): Guest {
    return Guest(
        id = id,
        name = name,
        side = side,
        relationshipScore = relationship,
        expectedGiftMoney = expectedGift,
        attendance = attendance,
        note = note,
    )
}

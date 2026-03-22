package kr.jm.weddingledger.feature.guests.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.feature.guests.domain.model.Guest
import kr.jm.weddingledger.feature.guests.domain.model.GuestAttendance
import kr.jm.weddingledger.feature.guests.domain.model.GuestSettings
import kr.jm.weddingledger.feature.guests.domain.model.GuestSideType
import kr.jm.weddingledger.feature.guests.domain.model.GuestsResult
import kr.jm.weddingledger.feature.guests.domain.model.NewGuest
import kr.jm.weddingledger.feature.guests.domain.repository.GuestRepository
import kr.jm.weddingledger.feature.guests.domain.usecase.AddGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.DeleteGuestUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.FetchGuestsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.SetGuestAttendanceUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestSettingsUseCase
import kr.jm.weddingledger.feature.guests.domain.usecase.UpdateGuestUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class GuestsViewModelTest {

    @Test
    fun `initial load calculates expected and gift summaries`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(
                    id = "g1",
                    name = "김하나",
                    side = GuestSideType.Groom,
                    attendance = GuestAttendance.Attending,
                    companionCount = 1,
                    actualAttended = true,
                    actualGiftMoney = 100_000L,
                ),
                guest(
                    id = "g2",
                    name = "박둘",
                    side = GuestSideType.Bride,
                    attendance = GuestAttendance.NotAttending,
                    actualAttended = false,
                ),
                guest(
                    id = "g3",
                    name = "공통셋",
                    side = GuestSideType.Both,
                    attendance = GuestAttendance.Undecided,
                    companionCount = 1,
                ),
            ),
            initialSettings = GuestSettings(guaranteedGuestCount = 3),
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("3명", state.expected.totalExpectedCountText)
        assertEquals("3명", state.expected.guaranteedGuestCountText)
        assertEquals("보증인원과 같아요", state.expected.guaranteedDiffText)
        assertEquals("₩100,000", state.giftLedger.totalGiftText)
        assertEquals("1명 참석", state.giftLedger.actualAttendingCountText)
        assertEquals("2명 입력 완료", state.giftLedger.completedEntryCountText)
        assertEquals("1명 미입력", state.giftLedger.pendingEntryCountText)
    }

    @Test
    fun `expected search and side filter reduce visible guests`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김민수", side = GuestSideType.Groom, note = "회사 친구"),
                guest(id = "g2", name = "이수진", side = GuestSideType.Bride, note = "신부 가족"),
                guest(id = "g3", name = "공통지인", side = GuestSideType.Both, note = "대학 동기"),
            )
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(GuestsAction.OnExpectedSearchQueryChange("친구"))
        advanceUntilIdle()

        var state = viewModel.state.value
        assertEquals(1, state.expected.sections.sumOf { it.items.size })
        assertEquals("김민수", state.expected.sections.first().items.first().name)

        val groomFilterId = state.expected.sideFilters.first { it.label == "신랑측" }.id
        viewModel.onAction(GuestsAction.OnSelectExpectedSideFilter(groomFilterId))
        advanceUntilIdle()

        state = viewModel.state.value
        assertEquals(1, state.expected.sections.sumOf { it.items.size })
        assertEquals("김민수", state.expected.sections.first().items.first().name)
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
                expectedStatus = GuestExpectedStatus.Maybe,
                companionCount = 0,
                actualAttended = GiftAttendanceState.Unknown,
                actualGiftAmount = null,
                note = null,
            )
        )

        val state = viewModel.state.value
        assertEquals("이름을 입력해주세요.", state.message)
        assertEquals(0, repository.addCallCount)
    }

    @Test
    fun `quick set expected status updates repository and reflects ui`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김민수", side = GuestSideType.Groom, attendance = GuestAttendance.Undecided),
            )
        )
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        viewModel.onAction(
            GuestsAction.OnQuickSetExpectedStatus(
                id = "g1",
                value = GuestExpectedStatus.NotAttending,
            )
        )
        advanceUntilIdle()

        val status = viewModel.state.value.expected.sections.first().items.first().status
        assertEquals(GuestExpectedStatus.NotAttending, status)
        assertEquals(1, repository.setAttendanceCallCount)
    }

    @Test
    fun `update guaranteed count saves and refreshes summary`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(
            initialGuests = listOf(
                guest(id = "g1", name = "김민수", side = GuestSideType.Groom, attendance = GuestAttendance.Attending),
            ),
            initialSettings = GuestSettings(guaranteedGuestCount = 10),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(GuestsAction.OnUpdateGuaranteedCount(1))
        advanceUntilIdle()

        assertEquals(1, repository.updateSettingsCallCount)
        assertEquals("보증인원과 같아요", viewModel.state.value.expected.guaranteedDiffText)
        assertEquals("1명", viewModel.state.value.expected.guaranteedGuestCountText)
    }

    @Test
    fun `loads current user role into ui state`() = runMainDispatcherTest {
        val repository = FakeGuestRepositoryForViewModel(initialGuests = emptyList())
        val viewModel = createViewModel(repository, userRole = UserRole.Groom)

        advanceUntilIdle()

        assertEquals(UserRole.Groom, viewModel.state.value.currentUserRole)
    }

    @Test
    fun `guest editor default side follows current user role`() {
        assertEquals(GuestSide.Groom, AddGuestUiState.expectedSample(UserRole.Groom.toDefaultGuestSide()).side)
        assertEquals(GuestSide.Bride, AddGuestUiState.giftSample(UserRole.Bride.toDefaultGuestSide()).side)
        assertEquals(GuestSide.Both, AddGuestUiState.expectedSample(null.toDefaultGuestSide()).side)
    }
}

private fun createViewModel(
    repository: FakeGuestRepositoryForViewModel,
    userRole: UserRole = UserRole.Bride,
): GuestsViewModel {
    return GuestsViewModel(
        fetchGuestsUseCase = FetchGuestsUseCase(repository),
        fetchMyProfileUseCase = FetchMyProfileUseCase(FakeOnboardingRepositoryForGuests(userRole)),
        fetchGuestSettingsUseCase = FetchGuestSettingsUseCase(repository),
        updateGuestSettingsUseCase = UpdateGuestSettingsUseCase(repository),
        addGuestUseCase = AddGuestUseCase(repository),
        updateGuestUseCase = UpdateGuestUseCase(repository),
        deleteGuestUseCase = DeleteGuestUseCase(repository),
        setGuestAttendanceUseCase = SetGuestAttendanceUseCase(repository),
    )
}

private class FakeOnboardingRepositoryForGuests(
    private val role: UserRole,
) : OnboardingRepository {
    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> = OnboardingResult.Success(
        MyProfile(
            id = "profile-1",
            email = null,
            nickname = "테스트",
            role = role,
            weddingId = "wedding-1",
            weddingDate = null,
            isWeddingDateUndecided = false,
            inviteCode = null,
            onboardingCompleted = true,
        )
    )

    override suspend fun updateNickname(nickname: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun updateMyProfile(
        nickname: String,
        role: UserRole,
        weddingDate: LocalDate?,
        isWeddingDateUndecided: Boolean,
    ): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> = OnboardingResult.Success("123456")

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun completeOnboarding(): OnboardingResult<Unit> = OnboardingResult.Success(Unit)
}

private class FakeGuestRepositoryForViewModel(
    initialGuests: List<Guest>,
    initialSettings: GuestSettings = GuestSettings(guaranteedGuestCount = 0),
) : GuestRepository {
    private val guests = linkedMapOf<String, Guest>().apply {
        initialGuests.forEach { guest -> put(guest.id, guest) }
    }

    private var settings: GuestSettings = initialSettings

    var addCallCount: Int = 0
    var setAttendanceCallCount: Int = 0
    var updateSettingsCallCount: Int = 0

    override suspend fun fetchGuests(): GuestsResult<List<Guest>> {
        return GuestsResult.Success(guests.values.toList())
    }

    override suspend fun fetchGuestSettings(): GuestsResult<GuestSettings> {
        return GuestsResult.Success(settings)
    }

    override suspend fun updateGuestSettings(settings: GuestSettings): GuestsResult<Unit> {
        updateSettingsCallCount += 1
        this.settings = settings
        return GuestsResult.Success(Unit)
    }

    override suspend fun addGuest(guest: NewGuest): GuestsResult<Unit> {
        addCallCount += 1
        val id = "guest-${guests.size + 1}"
        guests[id] = Guest(
            id = id,
            name = guest.name,
            side = guest.side,
            relationshipScore = guest.relationshipScore,
            attendance = guest.attendance,
            companionCount = guest.companionCount,
            actualAttended = guest.actualAttended,
            actualGiftMoney = guest.actualGiftMoney,
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
            attendance = guest.attendance,
            companionCount = guest.companionCount,
            actualAttended = guest.actualAttended,
            actualGiftMoney = guest.actualGiftMoney,
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
    companionCount: Int = 0,
    actualAttended: Boolean? = null,
    actualGiftMoney: Long? = null,
    relationship: Int = 1,
    note: String? = null,
): Guest {
    return Guest(
        id = id,
        name = name,
        side = side,
        relationshipScore = relationship,
        attendance = attendance,
        companionCount = companionCount,
        actualAttended = actualAttended,
        actualGiftMoney = actualGiftMoney,
        note = note,
    )
}

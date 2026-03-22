package kr.jm.weddingledger.feature.home.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.BudgetItem
import kr.jm.weddingledger.feature.money.domain.model.Expense
import kr.jm.weddingledger.feature.money.domain.model.MoneyCategory
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.NewScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.repository.ChecklistRepository
import kr.jm.weddingledger.feature.schedule.domain.repository.ScheduleEventRepository
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchScheduleEventsUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class HomeViewModelTest {

    @Test
    fun `refresh maps dashboard summary breakdown and upcoming items`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weddingDate = today.plus(10, DateTimeUnit.DAY)

        val moneyRepository = FakeMoneyRepositoryForHome(
            result = MoneyResult.Success(
                MoneySnapshot(
                    categories = listOf(
                        MoneyCategory(id = "cat-1", name = "웨딩홀", isDefault = true),
                        MoneyCategory(id = "cat-2", name = "신혼여행", isDefault = true),
                    ),
                    budgetItems = listOf(
                        BudgetItem(
                            id = "budget-1",
                            categoryId = "cat-1",
                            name = "홀 대관료",
                            targetAmount = 1_000_000,
                            memo = null,
                        ),
                        BudgetItem(
                            id = "budget-2",
                            categoryId = "cat-2",
                            name = "여행 예약",
                            targetAmount = 2_000_000,
                            memo = null,
                        ),
                    ),
                    expenses = listOf(
                        Expense(
                            id = "exp-1",
                            categoryId = "cat-1",
                            budgetItemId = "budget-1",
                            amount = 300_000,
                            date = today,
                            paymentMethod = PaymentMethod.Card,
                            payer = Payer.Groom,
                            vendorName = "웨딩홀",
                            memo = null,
                            imageUrl = null,
                        ),
                        Expense(
                            id = "exp-2",
                            categoryId = "cat-2",
                            budgetItemId = "budget-2",
                            amount = 500_000,
                            date = today.plus(-1, DateTimeUnit.DAY),
                            paymentMethod = PaymentMethod.Cash,
                            payer = Payer.Bride,
                            vendorName = "허니문",
                            memo = null,
                            imageUrl = null,
                        ),
                    ),
                )
            )
        )
        val onboardingRepository = FakeOnboardingRepositoryForHome(
            result = OnboardingResult.Success(
                MyProfile(
                    id = "profile-1",
                    email = "user@weddinglog.app",
                    nickname = "신랑",
                    role = UserRole.Groom,
                    weddingId = "wedding-1",
                    weddingDate = weddingDate,
                    isWeddingDateUndecided = false,
                    inviteCode = "123456",
                    onboardingCompleted = true,
                )
            )
        )
        val scheduleRepository = FakeScheduleEventRepositoryForHome(
            result = ScheduleResult.Success(
                listOf(
                    ScheduleEvent(
                        id = "event-1",
                        title = "스튜디오 촬영",
                        eventDate = today.plus(3, DateTimeUnit.DAY),
                        startTime = "14:00",
                        location = "강남 스튜디오",
                        memo = null,
                    )
                )
            )
        )
        val checklistRepository = FakeChecklistRepositoryForHome(
            result = ScheduleResult.Success(
                listOf(
                    ChecklistTask(
                        id = "task-1",
                        title = "청첩장 주문",
                        dTag = "D-30",
                        groupTitle = "하객",
                        isChecked = false,
                        targetDate = today.plus(1, DateTimeUnit.DAY),
                        memo = null,
                    )
                )
            )
        )

        val viewModel = HomeViewModel(
            fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(moneyRepository),
            fetchMyProfileUseCase = FetchMyProfileUseCase(onboardingRepository),
            fetchScheduleEventsUseCase = FetchScheduleEventsUseCase(scheduleRepository),
            fetchChecklistUseCase = FetchChecklistUseCase(checklistRepository),
        )
        advanceUntilIdle()

        val expectedDday = "우리 결혼 D-${today.daysUntil(weddingDate)}"
        val state = viewModel.state.value

        assertEquals(expectedDday, state.dDayTitle)
        assertEquals("₩2,200,000", state.overview.remainingBudgetText)
        assertEquals("총 지출 ₩800,000", state.overview.totalSpentText)
        assertEquals(3, state.payers.size)
        assertEquals("₩300,000", state.payers.first().amountText)
        assertEquals(2, state.categories.size)
        assertEquals(2, state.upcoming.size)
        assertTrue(state.summaryLine.contains("다가오는 일정 2건"))
        assertEquals(2, state.recentExpenses.size)
    }

    @Test
    fun `onboarding and repositories error fall back to empty dashboard`() = runMainDispatcherTest {
        val viewModel = HomeViewModel(
            fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(
                FakeMoneyRepositoryForHome(result = MoneyResult.Error("스냅샷 로드 실패"))
            ),
            fetchMyProfileUseCase = FetchMyProfileUseCase(
                FakeOnboardingRepositoryForHome(result = OnboardingResult.Error("프로필 로드 실패"))
            ),
            fetchScheduleEventsUseCase = FetchScheduleEventsUseCase(
                FakeScheduleEventRepositoryForHome(result = ScheduleResult.Error("일정 로드 실패"))
            ),
            fetchChecklistUseCase = FetchChecklistUseCase(
                FakeChecklistRepositoryForHome(result = ScheduleResult.Error("체크리스트 로드 실패"))
            ),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("우리 결혼 준비 중", state.dDayTitle)
        assertEquals("₩0", state.overview.remainingBudgetText)
        assertTrue(state.categories.isEmpty())
        assertTrue(state.upcoming.isEmpty())
    }

    @Test
    fun `recent expense falls back to category label when budget item is missing`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val viewModel = HomeViewModel(
            fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(
                FakeMoneyRepositoryForHome(
                    result = MoneyResult.Success(
                        MoneySnapshot(
                            categories = listOf(MoneyCategory(id = "cat-1", name = "웨딩홀", isDefault = true)),
                            budgetItems = emptyList(),
                            expenses = listOf(
                                Expense(
                                    id = "exp-1",
                                    categoryId = "cat-1",
                                    budgetItemId = null,
                                    amount = 150_000,
                                    date = today,
                                    paymentMethod = PaymentMethod.Card,
                                    payer = Payer.Bride,
                                    vendorName = null,
                                    memo = null,
                                    imageUrl = null,
                                )
                            ),
                        )
                    )
                )
            ),
            fetchMyProfileUseCase = FetchMyProfileUseCase(
                FakeOnboardingRepositoryForHome(result = OnboardingResult.Error("프로필 로드 실패"))
            ),
            fetchScheduleEventsUseCase = FetchScheduleEventsUseCase(
                FakeScheduleEventRepositoryForHome(result = ScheduleResult.Success(emptyList()))
            ),
            fetchChecklistUseCase = FetchChecklistUseCase(
                FakeChecklistRepositoryForHome(result = ScheduleResult.Success(emptyList()))
            ),
        )
        advanceUntilIdle()

        val recent = viewModel.state.value.recentExpenses.single()
        assertEquals("웨딩홀 지출", recent.title)
        assertTrue(recent.noteText.contains("웨딩홀"))
        assertTrue(recent.noteText.contains("신부"))
    }
}

private class FakeMoneyRepositoryForHome(
    private val result: MoneyResult<MoneySnapshot>,
) : MoneyRepository {
    override suspend fun addCategory(name: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> = result

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun updateCategory(id: String, name: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteCategory(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteBudgetItem(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteExpense(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)
}

private class FakeOnboardingRepositoryForHome(
    private val result: OnboardingResult<MyProfile>,
) : OnboardingRepository {
    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> = result

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

private class FakeScheduleEventRepositoryForHome(
    private val result: ScheduleResult<List<ScheduleEvent>>,
) : ScheduleEventRepository {
    override suspend fun fetchEvents(): ScheduleResult<List<ScheduleEvent>> = result

    override suspend fun createEvent(event: NewScheduleEvent): ScheduleResult<Unit> = ScheduleResult.Success(Unit)

    override suspend fun updateEvent(id: String, event: NewScheduleEvent): ScheduleResult<Unit> = ScheduleResult.Success(Unit)

    override suspend fun deleteEvent(id: String): ScheduleResult<Unit> = ScheduleResult.Success(Unit)
}

private class FakeChecklistRepositoryForHome(
    private val result: ScheduleResult<List<ChecklistTask>>,
) : ChecklistRepository {
    override suspend fun fetchChecklist(): ScheduleResult<List<ChecklistTask>> = result

    override suspend fun createChecklist(task: NewChecklistTask): ScheduleResult<Unit> = ScheduleResult.Success(Unit)

    override suspend fun updateChecklist(id: String, task: NewChecklistTask): ScheduleResult<Unit> = ScheduleResult.Success(Unit)

    override suspend fun setChecked(id: String, checked: Boolean): ScheduleResult<Unit> = ScheduleResult.Success(Unit)

    override suspend fun deleteChecklist(id: String): ScheduleResult<Unit> = ScheduleResult.Success(Unit)
}

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
import kr.jm.weddingledger.test.runMainDispatcherTest

class HomeViewModelTest {

    @Test
    fun `refresh maps money snapshot and dday title`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weddingDate = today.plus(10, DateTimeUnit.DAY)

        val moneyRepository = FakeMoneyRepositoryForHome(
            result = MoneyResult.Success(
                MoneySnapshot(
                    categories = listOf(MoneyCategory(id = "cat-1", name = "웨딩홀", isDefault = true)),
                    budgetItems = listOf(BudgetItem(id = "budget-1", categoryId = "cat-1", name = "홀 계약금", targetAmount = 1_000_000, memo = null)),
                    expenses = listOf(
                        Expense(
                            id = "exp-1",
                            budgetItemId = "budget-1",
                            amount = 300_000,
                            date = today,
                            paymentMethod = PaymentMethod.Card,
                            payer = Payer.Groom,
                            memo = null,
                            imageUrl = null,
                        )
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
                    inviteCode = "W-1234",
                    onboardingCompleted = true,
                )
            )
        )

        val viewModel = HomeViewModel(
            fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(moneyRepository),
            fetchMyProfileUseCase = FetchMyProfileUseCase(onboardingRepository),
        )
        advanceUntilIdle()

        val expectedDday = "우리 결혼 D-${today.daysUntil(weddingDate)}"
        val state = viewModel.state.value
        assertEquals(expectedDday, state.dDayTitle)
        assertEquals("₩700,000", state.remainingBudgetKrw)
        assertEquals("지출 ₩300,000", state.spentKrw)
        assertEquals(1, state.categories.size)
    }

    @Test
    fun `onboarding error falls back to default dday title`() = runMainDispatcherTest {
        val moneyRepository = FakeMoneyRepositoryForHome(
            result = MoneyResult.Error("스냅샷 로드 실패")
        )
        val onboardingRepository = FakeOnboardingRepositoryForHome(
            result = OnboardingResult.Error("프로필 로드 실패")
        )

        val viewModel = HomeViewModel(
            fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(moneyRepository),
            fetchMyProfileUseCase = FetchMyProfileUseCase(onboardingRepository),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("우리 결혼 준비 중", state.dDayTitle)
        assertEquals("₩0", state.remainingBudgetKrw)
        assertTrue(state.categories.isEmpty())
    }
}

private class FakeMoneyRepositoryForHome(
    private val result: MoneyResult<MoneySnapshot>,
) : MoneyRepository {
    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> = result

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> = MoneyResult.Success(Unit)

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

    override suspend fun getOrCreateInviteCode(): OnboardingResult<String> = OnboardingResult.Success("W-1234")

    override suspend fun connectPartner(inviteCode: String): OnboardingResult<Unit> = OnboardingResult.Success(Unit)

    override suspend fun completeOnboarding(): OnboardingResult<Unit> = OnboardingResult.Success(Unit)
}

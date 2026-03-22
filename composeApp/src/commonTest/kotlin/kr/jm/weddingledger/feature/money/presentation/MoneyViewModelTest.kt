package kr.jm.weddingledger.feature.money.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.BudgetItem
import kr.jm.weddingledger.feature.money.domain.model.Expense
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPresetPreference
import kr.jm.weddingledger.feature.money.domain.model.MoneyCategory
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.money.domain.repository.MoneyFilterPreferenceRepository
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.money.domain.usecase.AddCategoryUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteCategoryUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.SaveExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateCategoryUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateExpenseUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole
import kr.jm.weddingledger.feature.onboarding.domain.repository.OnboardingRepository
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.test.runMainDispatcherTest

class MoneyViewModelTest {

    @Test
    fun `init restores filters from preference and applies snapshot`() = runMainDispatcherTest {
        val repository = FakeMoneyRepositoryForViewModel(snapshot = sampleSnapshot())
        val preferenceRepository = FakeMoneyPreferenceRepository(
            initialState = ExpenseFilterPreferenceState(
                selectedCategoryId = "cat-hall",
                selectedPeriodFilterName = ExpensePeriodFilterOption.Last3Months.name,
                selectedStatusFilterName = ExpenseStatusFilterOption.HasMemo.name,
                selectedPresetId = "preset-1",
                presetSeed = 2,
                overBudgetOnly = false,
                presets = listOf(
                    ExpenseFilterPresetPreference(
                        id = "preset-1",
                        label = "웨딩홀 · 최근 3개월 · 메모 있음",
                        categoryId = "cat-hall",
                        periodFilterName = ExpensePeriodFilterOption.Last3Months.name,
                        statusFilterName = ExpenseStatusFilterOption.HasMemo.name,
                    )
                ),
            )
        )

        val viewModel = createViewModel(repository, preferenceRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.expenses.categoryFilters.first { it.id == "cat-hall" }.selected)
        assertTrue(state.expenses.periodFilters.first { it.option == ExpensePeriodFilterOption.Last3Months }.selected)
        assertTrue(state.expenses.statusFilters.first { it.option == ExpenseStatusFilterOption.HasMemo }.selected)
        assertEquals(1, state.expenses.presets.size)
        val hallCategory = state.budget.categories.first { it.id == "cat-hall" }
        assertEquals("지출 1건", hallCategory.expenseCountText)
        assertEquals("₩200,000 초과", hallCategory.remainingText)
        assertEquals(1, hallCategory.recentExpenses.size)
        assertEquals("신랑", state.budget.payerSummaries.first().label)
        assertEquals("예정 결제", state.budget.insightItems[2].label)
        assertTrue(state.budget.pendingPayments.isNotEmpty())
    }

    @Test
    fun `save expense filter preset updates ui and persists preference`() = runMainDispatcherTest {
        val repository = FakeMoneyRepositoryForViewModel(snapshot = sampleSnapshot())
        val preferenceRepository = FakeMoneyPreferenceRepository(initialState = null)
        val viewModel = createViewModel(repository, preferenceRepository)
        advanceUntilIdle()

        viewModel.onAction(MoneyAction.OnSelectExpenseFilter("cat-hall"))
        viewModel.onAction(MoneyAction.OnSelectExpenseStatusFilter(ExpenseStatusFilterOption.HasMemo))
        viewModel.onAction(MoneyAction.OnSaveExpenseFilterPreset)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.expenses.presets.size)
        assertEquals("필터 프리셋을 저장했습니다.", state.message)
        assertEquals(1, preferenceRepository.savedStates.last().presets.size)
    }

    @Test
    fun `toggle over budget filter narrows categories and persists flag`() = runMainDispatcherTest {
        val repository = FakeMoneyRepositoryForViewModel(snapshot = sampleSnapshot())
        val preferenceRepository = FakeMoneyPreferenceRepository(initialState = null)
        val viewModel = createViewModel(repository, preferenceRepository)
        advanceUntilIdle()

        viewModel.onAction(MoneyAction.OnToggleBudgetFilter)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(true, state.budget.isOverBudgetOnly)
        assertEquals(1, state.budget.categories.size)
        assertEquals("cat-hall", state.budget.categories.first().id)
        assertEquals(true, preferenceRepository.savedStates.last().overBudgetOnly)
    }

    @Test
    fun `pending payments prefer earlier due date and expose due text`() = runMainDispatcherTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val repository = FakeMoneyRepositoryForViewModel(
            snapshot = MoneySnapshot(
                categories = listOf(MoneyCategory(id = "cat-hall", name = "웨딩홀", isDefault = true)),
                budgetItems = listOf(
                    BudgetItem(
                        id = "budget-final",
                        categoryId = "cat-hall",
                        name = "웨딩홀 대관",
                        targetAmount = 3_000_000,
                        dueDate = today.plus(10, DateTimeUnit.DAY),
                        memo = null,
                    ),
                    BudgetItem(
                        id = "budget-mid",
                        categoryId = "cat-hall",
                        name = "식대",
                        targetAmount = 1_000_000,
                        dueDate = today.plus(3, DateTimeUnit.DAY),
                        memo = null,
                    ),
                ),
                expenses = emptyList(),
            )
        )
        val viewModel = createViewModel(repository, FakeMoneyPreferenceRepository(initialState = null))
        advanceUntilIdle()

        val pendingPayments = viewModel.state.value.budget.pendingPayments
        assertEquals("budget-mid", pendingPayments.first().id)
        assertEquals("D-3", pendingPayments.first().priorityText)
        assertTrue(pendingPayments.first().dueDateText?.contains("예정") == true)
    }

    @Test
    fun `category-only expense counts in category total but not in budget sub item progress`() = runMainDispatcherTest {
        val repository = FakeMoneyRepositoryForViewModel(
            snapshot = MoneySnapshot(
                categories = listOf(MoneyCategory(id = "cat-hall", name = "웨딩홀", isDefault = true)),
                budgetItems = listOf(
                    BudgetItem(
                        id = "budget-1",
                        categoryId = "cat-hall",
                        name = "홀 대관료",
                        targetAmount = 1_000_000,
                        memo = null,
                    )
                ),
                expenses = listOf(
                    Expense(
                        id = "expense-1",
                        categoryId = "cat-hall",
                        budgetItemId = "budget-1",
                        amount = 300_000,
                        date = LocalDate.parse("2026-03-10"),
                        paymentMethod = PaymentMethod.Card,
                        payer = Payer.Groom,
                        vendorName = null,
                        memo = null,
                        imageUrl = null,
                    ),
                    Expense(
                        id = "expense-2",
                        categoryId = "cat-hall",
                        budgetItemId = null,
                        amount = 200_000,
                        date = LocalDate.parse("2026-03-11"),
                        paymentMethod = PaymentMethod.Cash,
                        payer = Payer.Bride,
                        vendorName = "현장 추가금",
                        memo = null,
                        imageUrl = null,
                    ),
                ),
            )
        )
        val viewModel = createViewModel(repository, FakeMoneyPreferenceRepository(initialState = null))
        advanceUntilIdle()

        val category = viewModel.state.value.budget.categories.first()
        val budgetItem = category.subItems.first()

        assertEquals("₩500,000 사용", category.usedText)
        assertEquals("₩300,000 사용", budgetItem.spentText)
        assertEquals("현장 추가금", viewModel.state.value.expenses.sections.first().items.first().title)
    }

    @Test
    fun `create expense allows category-only payload`() = runMainDispatcherTest {
        val repository = FakeMoneyRepositoryForViewModel(snapshot = sampleSnapshot())
        val viewModel = createViewModel(repository, FakeMoneyPreferenceRepository(initialState = null))
        advanceUntilIdle()

        viewModel.onAction(
            MoneyAction.OnCreateExpense(
                categoryId = "cat-hall",
                budgetItemId = null,
                amount = 180_000,
                date = LocalDate.parse("2026-03-12"),
                paymentMethod = PaymentMethod.Card,
                payer = Payer.Bride,
                vendorName = "현장 결제",
                memo = null,
            )
        )
        advanceUntilIdle()

        assertEquals("cat-hall", repository.lastAddedExpense?.categoryId)
        assertEquals(null, repository.lastAddedExpense?.budgetItemId)
        assertEquals("현장 결제", repository.lastAddedExpense?.vendorName)
    }
}

private fun createViewModel(
    repository: FakeMoneyRepositoryForViewModel,
    preferenceRepository: FakeMoneyPreferenceRepository,
): MoneyViewModel {
    return MoneyViewModel(
        fetchMoneySnapshotUseCase = FetchMoneySnapshotUseCase(repository),
        fetchMyProfileUseCase = FetchMyProfileUseCase(FakeOnboardingRepositoryForMoney()),
        fetchExpenseFilterPreferenceUseCase = FetchExpenseFilterPreferenceUseCase(preferenceRepository),
        saveExpenseFilterPreferenceUseCase = SaveExpenseFilterPreferenceUseCase(preferenceRepository),
        addCategoryUseCase = AddCategoryUseCase(repository),
        addBudgetItemUseCase = AddBudgetItemUseCase(repository),
        addExpenseUseCase = AddExpenseUseCase(repository),
        updateCategoryUseCase = UpdateCategoryUseCase(repository),
        deleteCategoryUseCase = DeleteCategoryUseCase(repository),
        updateBudgetItemUseCase = UpdateBudgetItemUseCase(repository),
        updateExpenseUseCase = UpdateExpenseUseCase(repository),
        deleteBudgetItemUseCase = DeleteBudgetItemUseCase(repository),
        deleteExpenseUseCase = DeleteExpenseUseCase(repository),
    )
}

private class FakeOnboardingRepositoryForMoney : OnboardingRepository {
    override suspend fun fetchMyProfile(): OnboardingResult<MyProfile> = OnboardingResult.Success(
        MyProfile(
            id = "profile-1",
            email = null,
            nickname = "신부",
            role = UserRole.Bride,
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

private class FakeMoneyRepositoryForViewModel(
    private var snapshot: MoneySnapshot,
) : MoneyRepository {

    var lastAddedExpense: NewExpense? = null

    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> = MoneyResult.Success(snapshot)

    override suspend fun addCategory(name: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> {
        lastAddedExpense = expense
        return MoneyResult.Success(Unit)
    }

    override suspend fun updateCategory(id: String, name: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteCategory(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteBudgetItem(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)

    override suspend fun deleteExpense(id: String): MoneyResult<Unit> = MoneyResult.Success(Unit)
}

private class FakeMoneyPreferenceRepository(
    initialState: ExpenseFilterPreferenceState?,
) : MoneyFilterPreferenceRepository {
    private var state: ExpenseFilterPreferenceState? = initialState
    val savedStates = mutableListOf<ExpenseFilterPreferenceState>()

    override suspend fun loadExpenseFilterState(): ExpenseFilterPreferenceState? = state

    override suspend fun saveExpenseFilterState(state: ExpenseFilterPreferenceState) {
        this.state = state
        savedStates += state
    }
}

private fun sampleSnapshot(): MoneySnapshot {
    val categories = listOf(
        MoneyCategory(id = "cat-hall", name = "웨딩홀", isDefault = true),
        MoneyCategory(id = "cat-studio", name = "스튜디오", isDefault = true),
    )

    val budgetItems = listOf(
        BudgetItem(
            id = "budget-1",
            categoryId = "cat-hall",
            name = "홀 대관료",
            targetAmount = 1_000_000,
            memo = null,
        ),
        BudgetItem(
            id = "budget-2",
            categoryId = "cat-studio",
            name = "스튜디오",
            targetAmount = 1_000_000,
            memo = null,
        ),
    )

    val expenses = listOf(
        Expense(
            id = "expense-1",
            categoryId = "cat-hall",
            budgetItemId = "budget-1",
            amount = 1_200_000,
            date = LocalDate.parse("2026-03-10"),
            paymentMethod = PaymentMethod.Card,
            payer = Payer.Groom,
            vendorName = null,
            memo = "추가 옵션 포함",
            imageUrl = null,
        ),
        Expense(
            id = "expense-2",
            categoryId = "cat-studio",
            budgetItemId = "budget-2",
            amount = 300_000,
            date = LocalDate.parse("2026-03-11"),
            paymentMethod = PaymentMethod.Cash,
            payer = Payer.Bride,
            vendorName = null,
            memo = null,
            imageUrl = null,
        ),
    )

    return MoneySnapshot(
        categories = categories,
        budgetItems = budgetItems,
        expenses = expenses,
    )
}

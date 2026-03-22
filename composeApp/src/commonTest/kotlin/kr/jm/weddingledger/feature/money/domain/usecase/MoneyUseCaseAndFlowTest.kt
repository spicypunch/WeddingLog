package kr.jm.weddingledger.feature.money.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

class MoneyUseCaseAndFlowTest {

    @Test
    fun `fetch snapshot delegates to repository`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        repository.addBudgetItem(newBudgetItem(name = "웨딩홀 대관료", targetAmount = 1_000_000))

        val result = FetchMoneySnapshotUseCase(repository)()

        val success = assertIs<MoneyResult.Success<MoneySnapshot>>(result)
        assertEquals(1, success.data.budgetItems.size)
        assertEquals(1, repository.fetchCallCount)
    }

    @Test
    fun `add and update usecases delegate payload`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        val addBudgetItemUseCase = AddBudgetItemUseCase(repository)
        val addExpenseUseCase = AddExpenseUseCase(repository)
        val updateBudgetItemUseCase = UpdateBudgetItemUseCase(repository)
        val updateExpenseUseCase = UpdateExpenseUseCase(repository)

        addBudgetItemUseCase(newBudgetItem(name = "드레스", targetAmount = 1_500_000))
        addExpenseUseCase(newExpense(categoryId = "wedding_hall", budgetItemId = "budget-1", amount = 200_000))
        updateBudgetItemUseCase("budget-1", newBudgetItem(name = "드레스 수정", targetAmount = 1_700_000))
        updateExpenseUseCase("expense-1", newExpense(categoryId = "wedding_hall", budgetItemId = "budget-1", amount = 250_000))

        assertEquals(1, repository.addBudgetItemCallCount)
        assertEquals(1, repository.addExpenseCallCount)
        assertEquals(1, repository.updateBudgetItemCallCount)
        assertEquals(1, repository.updateExpenseCallCount)
    }

    @Test
    fun `delete usecases remove entities from repository`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        repository.addBudgetItem(newBudgetItem(name = "스튜디오", targetAmount = 900_000))
        repository.addExpense(newExpense(categoryId = "wedding_hall", budgetItemId = "budget-1", amount = 150_000))

        val deleteBudgetItemUseCase = DeleteBudgetItemUseCase(repository)
        val deleteExpenseUseCase = DeleteExpenseUseCase(repository)

        deleteExpenseUseCase("expense-1")
        deleteBudgetItemUseCase("budget-1")

        val snapshot = assertIs<MoneyResult.Success<MoneySnapshot>>(FetchMoneySnapshotUseCase(repository)()).data
        assertEquals(0, snapshot.expenses.size)
        assertEquals(0, snapshot.budgetItems.size)
        assertEquals(1, repository.deleteExpenseCallCount)
        assertEquals(1, repository.deleteBudgetItemCallCount)
    }

    @Test
    fun `delete category usecase removes empty category`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        val deleteCategoryUseCase = DeleteCategoryUseCase(repository)

        deleteCategoryUseCase("studio")

        val snapshot = assertIs<MoneyResult.Success<MoneySnapshot>>(FetchMoneySnapshotUseCase(repository)()).data
        assertEquals(1, snapshot.categories.size)
        assertEquals("웨딩홀", snapshot.categories.single().name)
        assertEquals(1, repository.deleteCategoryCallCount)
    }

    @Test
    fun `delete category usecase blocks category with direct expenses`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        repository.addExpense(newExpense(categoryId = "studio", budgetItemId = null, amount = 80_000))
        val deleteCategoryUseCase = DeleteCategoryUseCase(repository)

        val result = deleteCategoryUseCase("studio")

        assertIs<MoneyResult.Error>(result)
        assertEquals(0, repository.deleteCategoryCallCount)
    }

    @Test
    fun `integration flow budget and expense lifecycle`() = runBlocking {
        val repository = InMemoryMoneyRepository()
        val addBudgetItemUseCase = AddBudgetItemUseCase(repository)
        val addExpenseUseCase = AddExpenseUseCase(repository)
        val fetchSnapshotUseCase = FetchMoneySnapshotUseCase(repository)
        val updateBudgetItemUseCase = UpdateBudgetItemUseCase(repository)

        addBudgetItemUseCase(newBudgetItem(name = "웨딩홀", targetAmount = 2_000_000))
        addExpenseUseCase(newExpense(categoryId = "wedding_hall", budgetItemId = "budget-1", amount = 300_000))
        addExpenseUseCase(newExpense(categoryId = "wedding_hall", budgetItemId = "budget-1", amount = 250_000))

        updateBudgetItemUseCase("budget-1", newBudgetItem(name = "웨딩홀 계약", targetAmount = 2_500_000))

        val snapshot = assertIs<MoneyResult.Success<MoneySnapshot>>(fetchSnapshotUseCase()).data
        val budgetItem = snapshot.budgetItems.first()
        assertEquals("웨딩홀 계약", budgetItem.name)
        assertEquals(2_500_000, budgetItem.targetAmount)
        assertEquals(550_000, snapshot.expenses.sumOf { it.amount })
    }

    @Test
    fun `filter preference usecases persist and restore state`() = runBlocking {
        val repository = InMemoryMoneyFilterPreferenceRepository()
        val fetchUseCase = FetchExpenseFilterPreferenceUseCase(repository)
        val saveUseCase = SaveExpenseFilterPreferenceUseCase(repository)
        val state = ExpenseFilterPreferenceState(
            selectedCategoryId = "wedding_hall",
            selectedPeriodFilterName = "Last3Months",
            selectedStatusFilterName = "HasMemo",
            selectedPresetId = "preset-1",
            presetSeed = 2,
            overBudgetOnly = true,
            presets = listOf(
                ExpenseFilterPresetPreference(
                    id = "preset-1",
                    label = "웨딩홀 · 최근 3개월 · 메모 있음",
                    categoryId = "wedding_hall",
                    periodFilterName = "Last3Months",
                    statusFilterName = "HasMemo",
                )
            ),
        )

        saveUseCase(state)
        val restored = fetchUseCase()

        assertEquals(state, restored)
        assertEquals(1, repository.saveCallCount)
        assertEquals(1, repository.loadCallCount)
    }
}

private class InMemoryMoneyRepository : MoneyRepository {
    private val categories = mutableListOf(
        MoneyCategory(id = "wedding_hall", name = "웨딩홀", isDefault = true),
        MoneyCategory(id = "studio", name = "스튜디오", isDefault = true),
    )

    private val budgetItems = linkedMapOf<String, BudgetItem>()
    private val expenses = linkedMapOf<String, Expense>()

    private var budgetSeed = 1
    private var expenseSeed = 1

    var fetchCallCount: Int = 0
    var addBudgetItemCallCount: Int = 0
    var addExpenseCallCount: Int = 0
    var updateBudgetItemCallCount: Int = 0
    var updateExpenseCallCount: Int = 0
    var deleteCategoryCallCount: Int = 0
    var deleteBudgetItemCallCount: Int = 0
    var deleteExpenseCallCount: Int = 0

    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> {
        fetchCallCount += 1
        return MoneyResult.Success(
            MoneySnapshot(
                categories = categories,
                budgetItems = budgetItems.values.toList(),
                expenses = expenses.values.toList(),
            )
        )
    }

    override suspend fun addCategory(name: String): MoneyResult<Unit> {
        categories += MoneyCategory(
            id = "category-${categories.size + 1}",
            name = name,
            isDefault = false,
        )
        return MoneyResult.Success(Unit)
    }

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> {
        addBudgetItemCallCount += 1
        val id = "budget-${budgetSeed++}"
        budgetItems[id] = BudgetItem(
            id = id,
            categoryId = item.categoryId,
            name = item.name,
            targetAmount = item.targetAmount,
            dueDate = item.dueDate,
            memo = item.memo,
        )
        return MoneyResult.Success(Unit)
    }

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> {
        addExpenseCallCount += 1
        val id = "expense-${expenseSeed++}"
        expenses[id] = Expense(
            id = id,
            categoryId = expense.categoryId,
            budgetItemId = expense.budgetItemId,
            amount = expense.amount,
            date = expense.date,
            paymentMethod = expense.paymentMethod,
            payer = expense.payer,
            vendorName = expense.vendorName,
            memo = expense.memo,
            imageUrl = null,
        )
        return MoneyResult.Success(Unit)
    }

    override suspend fun updateCategory(id: String, name: String): MoneyResult<Unit> {
        val index = categories.indexOfFirst { it.id == id }
        if (index < 0) return MoneyResult.Error("카테고리를 찾지 못했습니다.")
        categories[index] = categories[index].copy(name = name)
        return MoneyResult.Success(Unit)
    }

    override suspend fun deleteCategory(id: String): MoneyResult<Unit> {
        if (budgetItems.values.any { it.categoryId == id } || expenses.values.any { it.categoryId == id }) {
            return MoneyResult.Error("지출이나 예산 항목이 남아 있는 카테고리는 삭제할 수 없어요.")
        }
        deleteCategoryCallCount += 1
        categories.removeAll { it.id == id }
        return MoneyResult.Success(Unit)
    }

    override suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit> {
        updateBudgetItemCallCount += 1
        val current = budgetItems[id] ?: return MoneyResult.Error("예산 항목을 찾지 못했습니다.")
        budgetItems[id] = current.copy(
            categoryId = item.categoryId,
            name = item.name,
            targetAmount = item.targetAmount,
            dueDate = item.dueDate,
            memo = item.memo,
        )
        return MoneyResult.Success(Unit)
    }

    override suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit> {
        updateExpenseCallCount += 1
        val current = expenses[id] ?: return MoneyResult.Error("지출 항목을 찾지 못했습니다.")
        expenses[id] = current.copy(
            categoryId = expense.categoryId,
            budgetItemId = expense.budgetItemId,
            amount = expense.amount,
            date = expense.date,
            paymentMethod = expense.paymentMethod,
            payer = expense.payer,
            vendorName = expense.vendorName,
            memo = expense.memo,
        )
        return MoneyResult.Success(Unit)
    }

    override suspend fun deleteBudgetItem(id: String): MoneyResult<Unit> {
        deleteBudgetItemCallCount += 1
        budgetItems.remove(id)
        val linkedExpenseIds = expenses
            .filterValues { expense -> expense.budgetItemId == id }
            .keys
            .toList()
        linkedExpenseIds.forEach { expenseId -> expenses.remove(expenseId) }
        return MoneyResult.Success(Unit)
    }

    override suspend fun deleteExpense(id: String): MoneyResult<Unit> {
        deleteExpenseCallCount += 1
        expenses.remove(id)
        return MoneyResult.Success(Unit)
    }
}

private class InMemoryMoneyFilterPreferenceRepository : MoneyFilterPreferenceRepository {
    var state: ExpenseFilterPreferenceState? = null
    var loadCallCount: Int = 0
    var saveCallCount: Int = 0

    override suspend fun loadExpenseFilterState(): ExpenseFilterPreferenceState? {
        loadCallCount += 1
        return state
    }

    override suspend fun saveExpenseFilterState(state: ExpenseFilterPreferenceState) {
        saveCallCount += 1
        this.state = state
    }
}

private fun newBudgetItem(
    categoryId: String = "wedding_hall",
    name: String,
    targetAmount: Int,
): NewBudgetItem {
    return NewBudgetItem(
        categoryId = categoryId,
        name = name,
        targetAmount = targetAmount,
        memo = null,
    )
}

private fun newExpense(
    categoryId: String,
    budgetItemId: String? = null,
    amount: Int,
): NewExpense {
    return NewExpense(
        categoryId = categoryId,
        budgetItemId = budgetItemId,
        amount = amount,
        date = LocalDate.parse("2026-03-15"),
        paymentMethod = PaymentMethod.Card,
        payer = Payer.Groom,
        vendorName = null,
        memo = null,
    )
}

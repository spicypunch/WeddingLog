package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Print
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.usecase.AddBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase

class MoneyViewModel(
    private val fetchMoneySnapshotUseCase: FetchMoneySnapshotUseCase,
    private val addBudgetItemUseCase: AddBudgetItemUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
) : ViewModel() {

    private val expandedCategoryIds = linkedSetOf<String>()
    private var selectedExpenseFilterCategoryId: String? = null
    private var latestSnapshot: MoneySnapshot? = null

    private val _state = MutableStateFlow(
        MoneyUiState(
            selectedTab = MoneyTab.Expenses,
            expenses = ExpensesUiState.empty(),
            budget = BudgetUiState.empty(),
            isLoading = false,
            message = null,
        )
    )
    val state: StateFlow<MoneyUiState> = _state.asStateFlow()

    init {
        refresh(showLoading = true)
    }

    fun onAction(action: MoneyAction) {
        when (action) {
            is MoneyAction.OnSelectTab -> _state.update { it.copy(selectedTab = action.tab) }
            is MoneyAction.OnSelectExpenseFilter -> selectExpenseFilter(action.categoryId)
            is MoneyAction.OnToggleBudgetCategory -> toggleBudgetCategory(action.id)
            MoneyAction.OnRefresh -> refresh(showLoading = true)

            is MoneyAction.OnCreateBudgetItem -> createBudgetItem(action)
            is MoneyAction.OnCreateExpense -> createExpense(action)

            MoneyAction.OnClickAdd -> Unit // handled in Route (bottom sheets)
            is MoneyAction.OnClickBudgetAddItem -> Unit // handled in Route (bottom sheet)
            is MoneyAction.OnClickExpense -> Unit // TODO: detail screen
        }
    }

    private fun refresh(showLoading: Boolean) {
        _state.update { it.copy(isLoading = showLoading, message = null) }

        viewModelScope.launch {
            when (val result = fetchMoneySnapshotUseCase()) {
                is MoneyResult.Success -> {
                    latestSnapshot = result.data
                    applySnapshot(result.data)
                }
                is MoneyResult.Error -> {
                    _state.update { it.copy(isLoading = false, message = result.message) }
                }
            }
        }
    }

    private fun applySnapshot(snapshot: MoneySnapshot) {
        if (expandedCategoryIds.isEmpty() && snapshot.categories.isNotEmpty()) {
            expandedCategoryIds.add(snapshot.categories.first().id)
        }

        val budgetUi = snapshot.toBudgetUiState(expandedCategoryIds)
        val expensesUi = snapshot.toExpensesUiState(selectedExpenseFilterCategoryId)

        _state.update {
            it.copy(
                budget = budgetUi,
                expenses = expensesUi,
                isLoading = false,
                message = null,
            )
        }
    }

    private fun selectExpenseFilter(categoryId: String?) {
        selectedExpenseFilterCategoryId = categoryId
        val snapshot = latestSnapshot ?: return
        _state.update { it.copy(expenses = snapshot.toExpensesUiState(categoryId)) }
    }

    private fun toggleBudgetCategory(id: String) {
        if (expandedCategoryIds.contains(id)) expandedCategoryIds.remove(id) else expandedCategoryIds.add(id)
        val snapshot = latestSnapshot ?: return
        _state.update { it.copy(budget = snapshot.toBudgetUiState(expandedCategoryIds)) }
    }

    private fun createBudgetItem(action: MoneyAction.OnCreateBudgetItem) {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            val item = NewBudgetItem(
                categoryId = action.categoryId,
                name = action.name,
                targetAmount = action.targetAmount,
                memo = action.memo,
            )

            when (val result = addBudgetItemUseCase(item)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }

    private fun createExpense(action: MoneyAction.OnCreateExpense) {
        _state.update { it.copy(isLoading = true, message = null) }

        viewModelScope.launch {
            val expense = NewExpense(
                budgetItemId = action.budgetItemId,
                amount = action.amount,
                date = action.date,
                paymentMethod = action.paymentMethod,
                payer = action.payer,
                memo = action.memo,
            )

            when (val result = addExpenseUseCase(expense)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, message = result.message) }
            }
        }
    }
}

private fun MoneySnapshot.toExpensesUiState(
    selectedCategoryId: String?,
): ExpensesUiState {
    val budgetItemsById = budgetItems.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }

    val selectedFilteredBudgetItemIds = if (selectedCategoryId == null) {
        null
    } else {
        budgetItems
            .asSequence()
            .filter { it.categoryId == selectedCategoryId }
            .map { it.id }
            .toSet()
    }

    val filteredExpenses = expenses
        .asSequence()
        .filter { selectedFilteredBudgetItemIds?.contains(it.budgetItemId) ?: true }
        .toList()

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val thisMonthTotal = filteredExpenses
        .asSequence()
        .filter { it.date.year == today.year && it.date.month == today.month }
        .sumOf { it.amount }

    val filters = buildList {
        add(ExpenseFilterChip(id = null, label = "전체", selected = selectedCategoryId == null))
        categories.forEach { category ->
            add(
                ExpenseFilterChip(
                    id = category.id,
                    label = category.name,
                    selected = category.id == selectedCategoryId,
                )
            )
        }
    }

    val groupedByMonth = filteredExpenses.groupBy { it.date.year to it.date.monthNumber }
    val sections = groupedByMonth
        .entries
        .sortedWith { a, b ->
            val (ay, am) = a.key
            val (by, bm) = b.key
            when {
                ay != by -> by - ay
                else -> bm - am
            }
        }
        .map { entry ->
            val (_, monthNumber) = entry.key
            val exps = entry.value
            val monthLabel = "${monthNumber}월"

            ExpenseSection(
                title = monthLabel,
                items = exps
                    .sortedWith(
                        compareByDescending<kr.jm.weddingledger.feature.money.domain.model.Expense> { it.date }
                            .thenByDescending { it.amount }
                    )
                    .map { exp ->
                        val budgetItem = budgetItemsById[exp.budgetItemId]
                        val category = budgetItem?.let { categoriesById[it.categoryId] }
                        val visuals = category?.name.visualsOrDefault()

                        ExpenseListItem(
                            id = exp.id,
                            title = budgetItem?.name ?: "지출",
                            dateText = "${exp.date.monthNumber}월 ${exp.date.dayOfMonth}일",
                            amountText = "-${formatNumber(exp.amount)}",
                            icon = visuals.icon,
                            accent = visuals.accent,
                            accentContainer = visuals.accentContainer,
                        )
                    }
            )
        }

    val budgetItemOptions = budgetItems.map { item ->
        val categoryName = categoriesById[item.categoryId]?.name ?: "기타"
        BudgetItemOption(
            id = item.id,
            label = "$categoryName · ${item.name}",
        )
    }

    return ExpensesUiState(
        monthTitle = "이번 달 지출",
        totalSpentKrw = formatNumber(thisMonthTotal),
        filters = filters,
        sections = sections,
        budgetItemOptions = budgetItemOptions,
    )
}

private fun MoneySnapshot.toBudgetUiState(
    expandedCategoryIds: Set<String>,
): BudgetUiState {
    val expensesByBudgetItemId = expenses.groupBy { it.budgetItemId }

    val budgetItemsByCategory = budgetItems.groupBy { it.categoryId }
    val totalTarget = budgetItems.sumOf { it.targetAmount }
    val totalUsed = expenses.sumOf { it.amount }
    val remaining = totalTarget - totalUsed

    val usedProgress = if (totalTarget <= 0) 0f else totalUsed.toFloat() / totalTarget.toFloat()
    val usedPercent = if (totalTarget <= 0) 0 else floor(usedProgress * 100).toInt()

    val categoriesUi = categories.map { category ->
        val visuals = category.name.visualsOrDefault()
        val items = budgetItemsByCategory[category.id].orEmpty()

        val target = items.sumOf { it.targetAmount }
        val used = items.sumOf { item ->
            expensesByBudgetItemId[item.id].orEmpty().sumOf { it.amount }
        }

        val progress = if (target <= 0) 0f else used.toFloat() / target.toFloat()

        BudgetCategoryItem(
            id = category.id,
            name = category.name,
            itemCountText = "${items.size}개 항목",
            usedText = "${formatKrw(used)} 사용",
            targetText = "${formatKrw(target)} 목표",
            progress = progress.coerceIn(0f, 1f),
            icon = visuals.icon,
            accent = visuals.accent,
            accentContainer = visuals.accentContainer,
            isExpanded = expandedCategoryIds.contains(category.id),
            subItems = items.map { item ->
                val spent = expensesByBudgetItemId[item.id].orEmpty().sumOf { it.amount }
                BudgetSubItem(
                    label = item.name,
                    valueText = if (spent <= 0) "미지출" else formatKrw(spent),
                    isEmpty = spent <= 0,
                )
            },
            isOverBudget = target > 0 && used > target,
        )
    }

    return BudgetUiState(
        totalBudgetText = formatKrw(totalTarget),
        usedPercentText = "${usedPercent}% 사용",
        usedAmountText = formatKrw(totalUsed),
        remainingAmountText = "${formatKrw(remaining)} 남음",
        usedProgress = usedProgress.coerceIn(0f, 1f),
        categories = categoriesUi,
    )
}

private data class CategoryVisuals(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val accentContainer: androidx.compose.ui.graphics.Color,
)

private fun String?.visualsOrDefault(): CategoryVisuals {
    val name = this?.trim().orEmpty()
    return when {
        name.contains("웨딩홀") || name.contains("예식장") -> CategoryVisuals(
            icon = Icons.Rounded.Gavel,
            accent = androidx.compose.ui.graphics.Color(0xFFEF3948),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFFDE9EB),
        )
        name.contains("스드메") -> CategoryVisuals(
            icon = Icons.Rounded.CameraAlt,
            accent = androidx.compose.ui.graphics.Color(0xFFF97316),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFFFEDD5),
        )
        name.contains("청첩장") -> CategoryVisuals(
            icon = Icons.Rounded.Print,
            accent = androidx.compose.ui.graphics.Color(0xFF3B82F6),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFDBEAFE),
        )
        name.contains("신혼여행") || name.contains("허니문") -> CategoryVisuals(
            icon = Icons.Rounded.FlightTakeoff,
            accent = androidx.compose.ui.graphics.Color(0xFF10B981),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFD1FAE5),
        )
        name.contains("예물") || name.contains("예단") -> CategoryVisuals(
            icon = Icons.Rounded.Diamond,
            accent = androidx.compose.ui.graphics.Color(0xFFA855F7),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFF3E8FF),
        )
        name.contains("드레스") -> CategoryVisuals(
            icon = Icons.Rounded.Checkroom,
            accent = androidx.compose.ui.graphics.Color(0xFFEF3948),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFFDE9EB),
        )
        else -> CategoryVisuals(
            icon = Icons.Rounded.AttachMoney,
            accent = androidx.compose.ui.graphics.Color(0xFF64748B),
            accentContainer = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
        )
    }
}

private fun formatKrw(amount: Int): String {
    val sign = if (amount < 0) "-" else ""
    return "${sign}₩${formatNumber(amount.toLong().absoluteValue)}"
}

private fun formatNumber(value: Int): String = formatNumber(value.toLong())

private fun formatNumber(value: Long): String = value
    .absoluteValue
    .toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

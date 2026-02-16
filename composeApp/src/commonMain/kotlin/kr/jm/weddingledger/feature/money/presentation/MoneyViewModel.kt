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
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPreferenceState
import kr.jm.weddingledger.feature.money.domain.model.ExpenseFilterPresetPreference
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.usecase.AddBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.AddExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.DeleteExpenseUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.SaveExpenseFilterPreferenceUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateBudgetItemUseCase
import kr.jm.weddingledger.feature.money.domain.usecase.UpdateExpenseUseCase

class MoneyViewModel(
    private val fetchMoneySnapshotUseCase: FetchMoneySnapshotUseCase,
    private val fetchExpenseFilterPreferenceUseCase: FetchExpenseFilterPreferenceUseCase,
    private val saveExpenseFilterPreferenceUseCase: SaveExpenseFilterPreferenceUseCase,
    private val addBudgetItemUseCase: AddBudgetItemUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateBudgetItemUseCase: UpdateBudgetItemUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteBudgetItemUseCase: DeleteBudgetItemUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
) : ViewModel() {

    private val expandedCategoryIds = linkedSetOf<String>()
    private var selectedExpenseFilterCategoryId: String? = null
    private var selectedExpensePeriodFilter: ExpensePeriodFilterOption = ExpensePeriodFilterOption.ThisMonth
    private var selectedExpenseStatusFilter: ExpenseStatusFilterOption = ExpenseStatusFilterOption.All
    private val savedExpensePresets = mutableListOf<SavedExpensePreset>()
    private var selectedExpensePresetId: String? = null
    private var expensePresetSeed: Int = 1
    private var overBudgetOnly: Boolean = false
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
        viewModelScope.launch {
            restoreFilterPreferenceState()
            refresh(showLoading = true)
        }
    }

    fun onAction(action: MoneyAction) {
        when (action) {
            is MoneyAction.OnSelectTab -> _state.update { it.copy(selectedTab = action.tab) }
            is MoneyAction.OnSelectExpenseFilter -> selectExpenseFilter(action.categoryId)
            is MoneyAction.OnSelectExpensePeriodFilter -> selectExpensePeriodFilter(action.option)
            is MoneyAction.OnSelectExpenseStatusFilter -> selectExpenseStatusFilter(action.option)
            MoneyAction.OnSaveExpenseFilterPreset -> saveCurrentExpenseFilterPreset()
            is MoneyAction.OnSelectExpenseFilterPreset -> selectExpenseFilterPreset(action.id)
            MoneyAction.OnToggleBudgetFilter -> toggleBudgetFilter()
            is MoneyAction.OnToggleBudgetCategory -> toggleBudgetCategory(action.id)
            MoneyAction.OnRefresh -> refresh(showLoading = false, isRefreshing = true)

            is MoneyAction.OnCreateBudgetItem -> createBudgetItem(action)
            is MoneyAction.OnCreateExpense -> createExpense(action)
            is MoneyAction.OnUpdateBudgetItem -> updateBudgetItem(action)
            is MoneyAction.OnUpdateExpense -> updateExpense(action)
            is MoneyAction.OnDeleteBudgetItem -> deleteBudgetItem(action)
            is MoneyAction.OnDeleteExpense -> deleteExpense(action)

            MoneyAction.OnClickAdd -> Unit // handled in Route (bottom sheets)
            MoneyAction.OnClickNotifications -> Unit // handled in Route (navigation)
            MoneyAction.OnClickSettings -> Unit // handled in Route (navigation)
            is MoneyAction.OnClickBudgetAddItem -> Unit // handled in Route (bottom sheet)
            is MoneyAction.OnClickBudgetItem -> Unit // handled in Route (bottom sheet)
            is MoneyAction.OnClickExpense -> Unit // handled in Route (bottom sheet)
        }
    }

    private fun refresh(showLoading: Boolean, isRefreshing: Boolean = false) {
        _state.update { it.copy(isLoading = showLoading, isRefreshing = isRefreshing, message = null) }

        viewModelScope.launch {
            when (val result = fetchMoneySnapshotUseCase()) {
                is MoneyResult.Success -> {
                    latestSnapshot = result.data
                    applySnapshot(result.data)
                }
                is MoneyResult.Error -> {
                    _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
                }
            }
        }
    }

    private fun applySnapshot(snapshot: MoneySnapshot) {
        if (expandedCategoryIds.isEmpty() && snapshot.categories.isNotEmpty()) {
            expandedCategoryIds.add(snapshot.categories.first().id)
        }

        val budgetUi = snapshot.toBudgetUiState(
            expandedCategoryIds = expandedCategoryIds,
            overBudgetOnly = overBudgetOnly,
        )
        val expensesUi = snapshot.toExpensesUiState(
            selectedCategoryId = selectedExpenseFilterCategoryId,
            selectedPeriodFilter = selectedExpensePeriodFilter,
            selectedStatusFilter = selectedExpenseStatusFilter,
            presets = savedExpensePresets,
            selectedPresetId = selectedExpensePresetId,
        )

        _state.update {
            it.copy(
                budget = budgetUi,
                expenses = expensesUi,
                isLoading = false,
                isRefreshing = false,
                message = null,
            )
        }
    }

    private fun selectExpenseFilter(categoryId: String?) {
        selectedExpenseFilterCategoryId = categoryId
        selectedExpensePresetId = matchingPresetId()
        persistExpenseFilterPreference()
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                expenses = snapshot.toExpensesUiState(
                    selectedCategoryId = selectedExpenseFilterCategoryId,
                    selectedPeriodFilter = selectedExpensePeriodFilter,
                    selectedStatusFilter = selectedExpenseStatusFilter,
                    presets = savedExpensePresets,
                    selectedPresetId = selectedExpensePresetId,
                )
            )
        }
    }

    private fun selectExpensePeriodFilter(option: ExpensePeriodFilterOption) {
        selectedExpensePeriodFilter = option
        selectedExpensePresetId = matchingPresetId()
        persistExpenseFilterPreference()
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                expenses = snapshot.toExpensesUiState(
                    selectedCategoryId = selectedExpenseFilterCategoryId,
                    selectedPeriodFilter = selectedExpensePeriodFilter,
                    selectedStatusFilter = selectedExpenseStatusFilter,
                    presets = savedExpensePresets,
                    selectedPresetId = selectedExpensePresetId,
                )
            )
        }
    }

    private fun selectExpenseStatusFilter(option: ExpenseStatusFilterOption) {
        selectedExpenseStatusFilter = option
        selectedExpensePresetId = matchingPresetId()
        persistExpenseFilterPreference()
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                expenses = snapshot.toExpensesUiState(
                    selectedCategoryId = selectedExpenseFilterCategoryId,
                    selectedPeriodFilter = selectedExpensePeriodFilter,
                    selectedStatusFilter = selectedExpenseStatusFilter,
                    presets = savedExpensePresets,
                    selectedPresetId = selectedExpensePresetId,
                )
            )
        }
    }

    private fun saveCurrentExpenseFilterPreset() {
        val existing = savedExpensePresets.firstOrNull {
            it.categoryId == selectedExpenseFilterCategoryId &&
                it.periodFilter == selectedExpensePeriodFilter &&
                it.statusFilter == selectedExpenseStatusFilter
        }
        if (existing != null) {
            selectedExpensePresetId = existing.id
            persistExpenseFilterPreference()
            val snapshot = latestSnapshot ?: return
            _state.update {
                it.copy(
                    expenses = snapshot.toExpensesUiState(
                        selectedCategoryId = selectedExpenseFilterCategoryId,
                        selectedPeriodFilter = selectedExpensePeriodFilter,
                        selectedStatusFilter = selectedExpenseStatusFilter,
                        presets = savedExpensePresets,
                        selectedPresetId = selectedExpensePresetId,
                    ),
                    message = "이미 저장된 프리셋입니다.",
                )
            }
            return
        }

        if (savedExpensePresets.size >= MAX_SAVED_PRESET_COUNT) {
            savedExpensePresets.removeAt(0)
        }
        val preset = SavedExpensePreset(
            id = "preset-${expensePresetSeed++}",
            label = buildPresetLabel(
                categoryId = selectedExpenseFilterCategoryId,
                periodFilter = selectedExpensePeriodFilter,
                statusFilter = selectedExpenseStatusFilter,
                latestSnapshot = latestSnapshot,
            ),
            categoryId = selectedExpenseFilterCategoryId,
            periodFilter = selectedExpensePeriodFilter,
            statusFilter = selectedExpenseStatusFilter,
        )
        savedExpensePresets += preset
        selectedExpensePresetId = preset.id
        persistExpenseFilterPreference()

        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                expenses = snapshot.toExpensesUiState(
                    selectedCategoryId = selectedExpenseFilterCategoryId,
                    selectedPeriodFilter = selectedExpensePeriodFilter,
                    selectedStatusFilter = selectedExpenseStatusFilter,
                    presets = savedExpensePresets,
                    selectedPresetId = selectedExpensePresetId,
                ),
                message = "필터 프리셋을 저장했습니다.",
            )
        }
    }

    private fun selectExpenseFilterPreset(id: String) {
        val preset = savedExpensePresets.firstOrNull { it.id == id } ?: return
        selectedExpenseFilterCategoryId = preset.categoryId
        selectedExpensePeriodFilter = preset.periodFilter
        selectedExpenseStatusFilter = preset.statusFilter
        selectedExpensePresetId = id
        persistExpenseFilterPreference()
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                expenses = snapshot.toExpensesUiState(
                    selectedCategoryId = selectedExpenseFilterCategoryId,
                    selectedPeriodFilter = selectedExpensePeriodFilter,
                    selectedStatusFilter = selectedExpenseStatusFilter,
                    presets = savedExpensePresets,
                    selectedPresetId = selectedExpensePresetId,
                )
            )
        }
    }

    private fun toggleBudgetCategory(id: String) {
        if (expandedCategoryIds.contains(id)) expandedCategoryIds.remove(id) else expandedCategoryIds.add(id)
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                budget = snapshot.toBudgetUiState(
                    expandedCategoryIds = expandedCategoryIds,
                    overBudgetOnly = overBudgetOnly,
                )
            )
        }
    }

    private fun toggleBudgetFilter() {
        overBudgetOnly = !overBudgetOnly
        persistExpenseFilterPreference()
        val snapshot = latestSnapshot ?: return
        _state.update {
            it.copy(
                budget = snapshot.toBudgetUiState(
                    expandedCategoryIds = expandedCategoryIds,
                    overBudgetOnly = overBudgetOnly,
                )
            )
        }
    }

    private fun createBudgetItem(action: MoneyAction.OnCreateBudgetItem) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            val item = NewBudgetItem(
                categoryId = action.categoryId,
                name = action.name,
                targetAmount = action.targetAmount,
                memo = action.memo,
            )

            when (val result = addBudgetItemUseCase(item)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun createExpense(action: MoneyAction.OnCreateExpense) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

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
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun updateBudgetItem(action: MoneyAction.OnUpdateBudgetItem) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            val item = NewBudgetItem(
                categoryId = action.categoryId,
                name = action.name,
                targetAmount = action.targetAmount,
                memo = action.memo,
            )

            when (val result = updateBudgetItemUseCase(action.id, item)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun updateExpense(action: MoneyAction.OnUpdateExpense) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            val expense = NewExpense(
                budgetItemId = action.budgetItemId,
                amount = action.amount,
                date = action.date,
                paymentMethod = action.paymentMethod,
                payer = action.payer,
                memo = action.memo,
            )

            when (val result = updateExpenseUseCase(action.id, expense)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun deleteBudgetItem(action: MoneyAction.OnDeleteBudgetItem) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            when (val result = deleteBudgetItemUseCase(action.id)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private fun deleteExpense(action: MoneyAction.OnDeleteExpense) {
        _state.update { it.copy(isLoading = true, isRefreshing = false, message = null) }

        viewModelScope.launch {
            when (val result = deleteExpenseUseCase(action.id)) {
                is MoneyResult.Success -> refresh(showLoading = false)
                is MoneyResult.Error -> _state.update { it.copy(isLoading = false, isRefreshing = false, message = result.message) }
            }
        }
    }

    private suspend fun restoreFilterPreferenceState() {
        val state = fetchExpenseFilterPreferenceUseCase() ?: return

        selectedExpenseFilterCategoryId = state.selectedCategoryId
        selectedExpensePeriodFilter = state.selectedPeriodFilterName.toPeriodFilterOrDefault()
        selectedExpenseStatusFilter = state.selectedStatusFilterName.toStatusFilterOrDefault()
        selectedExpensePresetId = state.selectedPresetId
        expensePresetSeed = state.presetSeed.coerceAtLeast(1)
        overBudgetOnly = state.overBudgetOnly

        savedExpensePresets.clear()
        savedExpensePresets += state.presets
            .takeLast(MAX_SAVED_PRESET_COUNT)
            .mapNotNull { preset ->
                val periodFilter = preset.periodFilterName.toPeriodFilterOrNull() ?: return@mapNotNull null
                val statusFilter = preset.statusFilterName.toStatusFilterOrNull() ?: return@mapNotNull null
                SavedExpensePreset(
                    id = preset.id,
                    label = preset.label,
                    categoryId = preset.categoryId,
                    periodFilter = periodFilter,
                    statusFilter = statusFilter,
                )
            }
        if (expensePresetSeed <= savedExpensePresets.size) {
            expensePresetSeed = savedExpensePresets.size + 1
        }
        selectedExpensePresetId = selectedExpensePresetId
            ?.takeIf { selectedId -> savedExpensePresets.any { it.id == selectedId } }
            ?: matchingPresetId()
    }

    private fun persistExpenseFilterPreference() {
        val state = ExpenseFilterPreferenceState(
            selectedCategoryId = selectedExpenseFilterCategoryId,
            selectedPeriodFilterName = selectedExpensePeriodFilter.name,
            selectedStatusFilterName = selectedExpenseStatusFilter.name,
            selectedPresetId = selectedExpensePresetId,
            presetSeed = expensePresetSeed.coerceAtLeast(1),
            overBudgetOnly = overBudgetOnly,
            presets = savedExpensePresets
                .takeLast(MAX_SAVED_PRESET_COUNT)
                .map { preset ->
                    ExpenseFilterPresetPreference(
                        id = preset.id,
                        label = preset.label,
                        categoryId = preset.categoryId,
                        periodFilterName = preset.periodFilter.name,
                        statusFilterName = preset.statusFilter.name,
                    )
                },
        )
        viewModelScope.launch {
            saveExpenseFilterPreferenceUseCase(state)
        }
    }

    private fun matchingPresetId(): String? {
        return savedExpensePresets.firstOrNull {
            it.categoryId == selectedExpenseFilterCategoryId &&
                it.periodFilter == selectedExpensePeriodFilter &&
                it.statusFilter == selectedExpenseStatusFilter
        }?.id
    }

    private fun buildPresetLabel(
        categoryId: String?,
        periodFilter: ExpensePeriodFilterOption,
        statusFilter: ExpenseStatusFilterOption,
        latestSnapshot: MoneySnapshot?,
    ): String {
        val categoryLabel = latestSnapshot
            ?.categories
            ?.firstOrNull { it.id == categoryId }
            ?.name
            ?: "전체"
        return "${categoryLabel} · ${periodFilter.label} · ${statusFilter.label}"
    }
}

private data class SavedExpensePreset(
    val id: String,
    val label: String,
    val categoryId: String?,
    val periodFilter: ExpensePeriodFilterOption,
    val statusFilter: ExpenseStatusFilterOption,
)

private fun MoneySnapshot.toExpensesUiState(
    selectedCategoryId: String?,
    selectedPeriodFilter: ExpensePeriodFilterOption,
    selectedStatusFilter: ExpenseStatusFilterOption,
    presets: List<SavedExpensePreset>,
    selectedPresetId: String?,
): ExpensesUiState {
    val budgetItemsById = budgetItems.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

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
        .filter { expense -> selectedPeriodFilter.matches(expense.date, today) }
        .filter { expense -> selectedStatusFilter.matches(expense) }
        .toList()

    val total = filteredExpenses.sumOf { it.amount }

    val categoryFilters = buildList {
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
    val periodFilters = ExpensePeriodFilterOption.entries.map { option ->
        ExpensePeriodFilterChip(
            option = option,
            selected = option == selectedPeriodFilter,
        )
    }
    val statusFilters = ExpenseStatusFilterOption.entries.map { option ->
        ExpenseStatusFilterChip(
            option = option,
            selected = option == selectedStatusFilter,
        )
    }
    val presetFilters = presets.map { preset ->
        ExpenseFilterPresetChip(
            id = preset.id,
            label = preset.label,
            selected = preset.id == selectedPresetId,
        )
    }

    val groupedByMonth = filteredExpenses.groupBy { it.date.year to it.date.month.number }
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
                            budgetItemId = exp.budgetItemId,
                            title = budgetItem?.name ?: "지출",
                            dateText = "${exp.date.month.number}월 ${exp.date.day}일",
                            amountText = "-${formatNumber(exp.amount)}",
                            amount = exp.amount,
                            date = exp.date,
                            paymentMethod = exp.paymentMethod,
                            payer = exp.payer,
                            memo = exp.memo,
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
        monthTitle = "${selectedPeriodFilter.label} 지출",
        totalSpentKrw = formatNumber(total),
        categoryFilters = categoryFilters,
        periodFilters = periodFilters,
        statusFilters = statusFilters,
        presets = presetFilters,
        sections = sections,
        budgetItemOptions = budgetItemOptions,
    )
}

private fun ExpensePeriodFilterOption.matches(
    date: kotlinx.datetime.LocalDate,
    today: kotlinx.datetime.LocalDate,
): Boolean {
    return when (this) {
        ExpensePeriodFilterOption.All -> true
        ExpensePeriodFilterOption.ThisMonth -> date.year == today.year && date.month == today.month
        ExpensePeriodFilterOption.Last3Months -> {
            val diff = (today.year - date.year) * 12 + (today.month.number - date.month.number)
            diff in 0..2
        }
    }
}

private fun ExpenseStatusFilterOption.matches(
    expense: kr.jm.weddingledger.feature.money.domain.model.Expense,
): Boolean {
    return when (this) {
        ExpenseStatusFilterOption.All -> true
        ExpenseStatusFilterOption.HighAmount -> expense.amount >= 500_000
        ExpenseStatusFilterOption.HasMemo -> !expense.memo.isNullOrBlank()
    }
}

private fun MoneySnapshot.toBudgetUiState(
    expandedCategoryIds: Set<String>,
    overBudgetOnly: Boolean,
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
                    id = item.id,
                    label = item.name,
                    valueText = if (spent <= 0) "미지출" else formatKrw(spent),
                    isEmpty = spent <= 0,
                    targetAmount = item.targetAmount,
                    memo = item.memo,
                )
            },
            isOverBudget = target > 0 && used > target,
        )
    }.filter { item ->
        if (!overBudgetOnly) true else item.isOverBudget
    }

    return BudgetUiState(
        totalBudgetText = formatKrw(totalTarget),
        usedPercentText = "${usedPercent}% 사용",
        usedAmountText = formatKrw(totalUsed),
        remainingAmountText = "${formatKrw(remaining)} 남음",
        usedProgress = usedProgress.coerceIn(0f, 1f),
        isOverBudgetOnly = overBudgetOnly,
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

private fun String.toPeriodFilterOrDefault(): ExpensePeriodFilterOption {
    return toPeriodFilterOrNull() ?: ExpensePeriodFilterOption.ThisMonth
}

private fun String.toStatusFilterOrDefault(): ExpenseStatusFilterOption {
    return toStatusFilterOrNull() ?: ExpenseStatusFilterOption.All
}

private fun String.toPeriodFilterOrNull(): ExpensePeriodFilterOption? {
    return runCatching { ExpensePeriodFilterOption.valueOf(this) }.getOrNull()
}

private fun String.toStatusFilterOrNull(): ExpenseStatusFilterOption? {
    return runCatching { ExpenseStatusFilterOption.valueOf(this) }.getOrNull()
}

private const val MAX_SAVED_PRESET_COUNT: Int = 5

package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun MoneyRoute() {
    var selectedTab by remember { mutableStateOf(MoneyTab.Expenses) }
    var expensesState by remember { mutableStateOf(ExpensesUiState.sample()) }
    var budgetState by remember { mutableStateOf(BudgetUiState.sample()) }

    var activeSheet by remember { mutableStateOf<MoneySheet?>(null) }
    var addExpenseState by remember { mutableStateOf(AddExpenseUiState.sample()) }
    var addBudgetItemState by remember { mutableStateOf(AddBudgetItemUiState.sample()) }

    MoneyScreen(
        selectedTab = selectedTab,
        expensesState = expensesState,
        budgetState = budgetState,
        onAction = { action ->
            when (action) {
                is MoneyAction.OnSelectTab -> selectedTab = action.tab
                is MoneyAction.OnSelectExpenseFilter -> {
                    expensesState = expensesState.copy(
                        filters = expensesState.filters.map { it.copy(selected = it.label == action.label) }
                    )
                }
                is MoneyAction.OnToggleBudgetCategory -> {
                    budgetState = budgetState.copy(
                        categories = budgetState.categories.map { category ->
                            if (category.id == action.id) category.copy(isExpanded = !category.isExpanded) else category
                        }
                    )
                }
                MoneyAction.OnClickAdd -> {
                    activeSheet = when (selectedTab) {
                        MoneyTab.Expenses -> MoneySheet.AddExpense
                        MoneyTab.Budget -> {
                            val budgetCategories = budgetState.categories.map { it.name }
                            if (budgetCategories.isNotEmpty()) {
                                addBudgetItemState = addBudgetItemState.copy(
                                    categoryLabels = budgetCategories,
                                    selectedCategoryLabel = budgetCategories.first()
                                )
                            }
                            MoneySheet.AddBudgetItem(categoryId = null)
                        }
                    }
                }
                is MoneyAction.OnClickBudgetAddItem -> {
                    val budgetCategories = budgetState.categories.map { it.name }
                    addBudgetItemState = addBudgetItemState.copy(
                        categoryLabels = if (budgetCategories.isNotEmpty()) budgetCategories else addBudgetItemState.categoryLabels,
                        selectedCategoryLabel = budgetState.categories.firstOrNull { it.id == action.categoryId }?.name
                            ?: addBudgetItemState.selectedCategoryLabel
                    )
                    activeSheet = MoneySheet.AddBudgetItem(categoryId = action.categoryId)
                }
                is MoneyAction.OnClickExpense -> Unit // TODO
            }
        }
    )

    when (val sheet = activeSheet) {
        null -> Unit
        MoneySheet.AddExpense -> {
            AddExpenseBottomSheet(
                state = addExpenseState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddExpenseAction.OnClickClose -> activeSheet = null
                        AddExpenseAction.OnClickSave -> activeSheet = null // TODO: persist + sync
                        AddExpenseAction.OnClickDate -> Unit // TODO: date picker
                        is AddExpenseAction.OnAmountChange -> addExpenseState = addExpenseState.copy(amount = act.value)
                        is AddExpenseAction.OnMemoChange -> addExpenseState = addExpenseState.copy(memo = act.value)
                        is AddExpenseAction.OnSelectCategory -> addExpenseState = addExpenseState.copy(selectedCategoryLabel = act.label)
                        is AddExpenseAction.OnSelectPaymentMethod -> addExpenseState = addExpenseState.copy(paymentMethod = act.method)
                        is AddExpenseAction.OnSelectPayer -> addExpenseState = addExpenseState.copy(payer = act.payer)
                    }
                }
            )
        }
        is MoneySheet.AddBudgetItem -> {
            AddBudgetItemBottomSheet(
                state = addBudgetItemState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddBudgetItemAction.OnClickClose -> activeSheet = null
                        AddBudgetItemAction.OnClickSave -> activeSheet = null // TODO: persist + sync
                        is AddBudgetItemAction.OnSelectCategory -> addBudgetItemState = addBudgetItemState.copy(selectedCategoryLabel = act.label)
                        is AddBudgetItemAction.OnNameChange -> addBudgetItemState = addBudgetItemState.copy(name = act.value)
                        is AddBudgetItemAction.OnTargetAmountChange -> addBudgetItemState = addBudgetItemState.copy(targetAmount = act.value)
                        is AddBudgetItemAction.OnMemoChange -> addBudgetItemState = addBudgetItemState.copy(memo = act.value)
                    }
                }
            )
        }
    }
}

private sealed interface MoneySheet {
    data object AddExpense : MoneySheet
    data class AddBudgetItem(val categoryId: String?) : MoneySheet
}

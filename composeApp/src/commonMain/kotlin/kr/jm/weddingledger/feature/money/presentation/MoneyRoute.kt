package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MoneyRoute() {
    val viewModel: MoneyViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<MoneySheet?>(null) }
    var addExpenseState by remember { mutableStateOf(AddExpenseUiState.sample()) }
    var addBudgetItemState by remember { mutableStateOf(AddBudgetItemUiState.sample()) }

    MoneyScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                MoneyAction.OnClickAdd -> {
                    when (uiState.selectedTab) {
                        MoneyTab.Expenses -> {
                            activeSheet = MoneySheet.AddExpense
                            addExpenseState = newAddExpenseState(uiState)
                        }
                        MoneyTab.Budget -> {
                            activeSheet = MoneySheet.AddBudgetItem
                            addBudgetItemState = newAddBudgetItemState(
                                uiState = uiState,
                                preselectedCategoryId = uiState.budget.categories.firstOrNull()?.id,
                            )
                        }
                    }
                }
                is MoneyAction.OnClickBudgetAddItem -> {
                    activeSheet = MoneySheet.AddBudgetItem
                    addBudgetItemState = newAddBudgetItemState(
                        uiState = uiState,
                        preselectedCategoryId = action.categoryId,
                    )
                }
                else -> viewModel.onAction(action)
            }
        }
    )

    when (activeSheet) {
        null -> Unit
        MoneySheet.AddExpense -> {
            AddExpenseBottomSheet(
                state = addExpenseState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddExpenseAction.OnClickClose -> activeSheet = null
                        AddExpenseAction.OnClickSave -> {
                            val budgetItemId = addExpenseState.selectedBudgetItemId ?: return@AddExpenseBottomSheet
                            val amount = parseAmountOrNull(addExpenseState.amount) ?: return@AddExpenseBottomSheet

                            viewModel.onAction(
                                MoneyAction.OnCreateExpense(
                                    budgetItemId = budgetItemId,
                                    amount = amount,
                                    date = addExpenseState.date,
                                    paymentMethod = addExpenseState.paymentMethod,
                                    payer = addExpenseState.payer,
                                    memo = addExpenseState.memo,
                                )
                            )

                            activeSheet = null
                            addExpenseState = newAddExpenseState(uiState)
                        }
                        AddExpenseAction.OnClickDate -> Unit // TODO: date picker
                        is AddExpenseAction.OnAmountChange -> addExpenseState = addExpenseState.copy(amount = act.value)
                        is AddExpenseAction.OnMemoChange -> addExpenseState = addExpenseState.copy(memo = act.value)
                        is AddExpenseAction.OnSelectBudgetItem -> addExpenseState = addExpenseState.copy(selectedBudgetItemId = act.id)
                        is AddExpenseAction.OnSelectPaymentMethod -> addExpenseState = addExpenseState.copy(paymentMethod = act.method)
                        is AddExpenseAction.OnSelectPayer -> addExpenseState = addExpenseState.copy(payer = act.payer)
                    }
                }
            )
        }
        MoneySheet.AddBudgetItem -> {
            AddBudgetItemBottomSheet(
                state = addBudgetItemState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddBudgetItemAction.OnClickClose -> activeSheet = null
                        AddBudgetItemAction.OnClickSave -> {
                            val categoryId = addBudgetItemState.selectedCategoryId ?: return@AddBudgetItemBottomSheet
                            val target = parseAmountOrNull(addBudgetItemState.targetAmount) ?: return@AddBudgetItemBottomSheet

                            viewModel.onAction(
                                MoneyAction.OnCreateBudgetItem(
                                    categoryId = categoryId,
                                    name = addBudgetItemState.name,
                                    targetAmount = target,
                                    memo = addBudgetItemState.memo,
                                )
                            )

                            activeSheet = null
                            addBudgetItemState = newAddBudgetItemState(
                                uiState = uiState,
                                preselectedCategoryId = uiState.budget.categories.firstOrNull()?.id,
                            )
                        }
                        is AddBudgetItemAction.OnSelectCategory -> addBudgetItemState = addBudgetItemState.copy(selectedCategoryId = act.id)
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
    data object AddBudgetItem : MoneySheet
}

private fun newAddExpenseState(uiState: MoneyUiState): AddExpenseUiState {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return AddExpenseUiState(
        amount = "",
        date = today,
        dateText = today.toKoreanDateText(),
        budgetItemOptions = uiState.expenses.budgetItemOptions,
        selectedBudgetItemId = null,
        paymentMethod = kr.jm.weddingledger.feature.money.domain.model.PaymentMethod.Card,
        payer = kr.jm.weddingledger.feature.money.domain.model.Payer.Bride,
        memo = "",
    )
}

private fun newAddBudgetItemState(
    uiState: MoneyUiState,
    preselectedCategoryId: String?,
): AddBudgetItemUiState {
    val options = uiState.budget.categories.map { CategoryOption(id = it.id, label = it.name) }
    val selectedId = preselectedCategoryId ?: options.firstOrNull()?.id
    return AddBudgetItemUiState(
        categoryOptions = options,
        selectedCategoryId = selectedId,
        name = "",
        targetAmount = "",
        memo = "",
        helperText = null,
    )
}

private fun parseAmountOrNull(value: String): Int? {
    val digits = value.filter { it.isDigit() }
    return digits.toIntOrNull()
}

private fun kotlinx.datetime.LocalDate.toKoreanDateText(): String {
    val mm = monthNumber.toString().padStart(2, '0')
    val dd = dayOfMonth.toString().padStart(2, '0')
    return "${year}.$mm.$dd (${dayOfWeek.toKoreanShort()})"
}

private fun DayOfWeek.toKoreanShort(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

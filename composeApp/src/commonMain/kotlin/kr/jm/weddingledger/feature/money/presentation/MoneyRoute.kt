package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Instant
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyRoute() {
    val viewModel: MoneyViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<MoneySheet?>(null) }
    var addExpenseState by remember { mutableStateOf(AddExpenseUiState.sample()) }
    var addBudgetItemState by remember { mutableStateOf(AddBudgetItemUiState.sample()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    LaunchedEffect(activeSheet) {
        if (activeSheet == null) showDatePicker = false
    }

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
                is MoneyAction.OnClickBudgetItem -> {
                    val found = findBudgetItem(uiState, action.id) ?: return@MoneyScreen
                    activeSheet = MoneySheet.EditBudgetItem(action.id)
                    addBudgetItemState = newEditBudgetItemState(
                        uiState = uiState,
                        categoryId = found.categoryId,
                        item = found.item,
                    )
                }
                is MoneyAction.OnClickExpense -> {
                    val item = findExpense(uiState, action.id) ?: return@MoneyScreen
                    activeSheet = MoneySheet.EditExpense(action.id)
                    addExpenseState = newEditExpenseState(uiState, item)
                }
                else -> viewModel.onAction(action)
            }
        }
    )

    when (val sheet = activeSheet) {
        null -> Unit
        MoneySheet.AddExpense,
        is MoneySheet.EditExpense,
        -> {
            val editingId = (sheet as? MoneySheet.EditExpense)?.expenseId
            AddExpenseBottomSheet(
                title = if (editingId == null) "지출 추가" else "지출 수정",
                state = addExpenseState,
                showDelete = editingId != null,
                onDismiss = {
                    activeSheet = null
                    showDatePicker = false
                },
                onAction = { act ->
                    when (act) {
                        AddExpenseAction.OnClickClose -> {
                            activeSheet = null
                            showDatePicker = false
                        }
                        AddExpenseAction.OnClickDelete -> {
                            val id = editingId ?: return@AddExpenseBottomSheet
                            deleteTarget = DeleteTarget.Expense(id)
                        }
                        AddExpenseAction.OnClickSave -> {
                            val budgetItemId = addExpenseState.selectedBudgetItemId ?: return@AddExpenseBottomSheet
                            val amount = parseAmountOrNull(addExpenseState.amount) ?: return@AddExpenseBottomSheet

                            if (editingId == null) {
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
                            } else {
                                viewModel.onAction(
                                    MoneyAction.OnUpdateExpense(
                                        id = editingId,
                                        budgetItemId = budgetItemId,
                                        amount = amount,
                                        date = addExpenseState.date,
                                        paymentMethod = addExpenseState.paymentMethod,
                                        payer = addExpenseState.payer,
                                        memo = addExpenseState.memo,
                                    )
                                )
                            }

                            activeSheet = null
                            showDatePicker = false
                            addExpenseState = newAddExpenseState(uiState)
                        }
                        AddExpenseAction.OnClickDate -> showDatePicker = true
                        is AddExpenseAction.OnAmountChange -> addExpenseState = addExpenseState.copy(amount = act.value)
                        is AddExpenseAction.OnMemoChange -> addExpenseState = addExpenseState.copy(memo = act.value)
                        is AddExpenseAction.OnSelectBudgetItem -> addExpenseState = addExpenseState.copy(selectedBudgetItemId = act.id)
                        is AddExpenseAction.OnSelectPaymentMethod -> addExpenseState = addExpenseState.copy(paymentMethod = act.method)
                        is AddExpenseAction.OnSelectPayer -> addExpenseState = addExpenseState.copy(payer = act.payer)
                    }
                }
            )

            if (showDatePicker) {
                // Material3 DatePicker works with UTC milliseconds (date at 00:00 UTC).
                val timeZone = TimeZone.UTC
                val initialMillis = addExpenseState.date
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val date = Instant
                                        .fromEpochMilliseconds(millis)
                                        .toLocalDateTime(timeZone)
                                        .date
                                    addExpenseState = addExpenseState.copy(
                                        date = date,
                                        dateText = date.toKoreanDateText(),
                                    )
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text(text = "확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = "취소")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
        MoneySheet.AddBudgetItem,
        is MoneySheet.EditBudgetItem,
        -> {
            val editingId = (sheet as? MoneySheet.EditBudgetItem)?.budgetItemId
            AddBudgetItemBottomSheet(
                title = if (editingId == null) "예산 항목 추가" else "예산 항목 수정",
                state = addBudgetItemState,
                showDelete = editingId != null,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        AddBudgetItemAction.OnClickClose -> activeSheet = null
                        AddBudgetItemAction.OnClickDelete -> {
                            val id = editingId ?: return@AddBudgetItemBottomSheet
                            deleteTarget = DeleteTarget.BudgetItem(id)
                        }
                        AddBudgetItemAction.OnClickSave -> {
                            val categoryId = addBudgetItemState.selectedCategoryId ?: return@AddBudgetItemBottomSheet
                            val target = parseAmountOrNull(addBudgetItemState.targetAmount) ?: return@AddBudgetItemBottomSheet

                            if (editingId == null) {
                                viewModel.onAction(
                                    MoneyAction.OnCreateBudgetItem(
                                        categoryId = categoryId,
                                        name = addBudgetItemState.name,
                                        targetAmount = target,
                                        memo = addBudgetItemState.memo,
                                    )
                                )
                            } else {
                                viewModel.onAction(
                                    MoneyAction.OnUpdateBudgetItem(
                                        id = editingId,
                                        categoryId = categoryId,
                                        name = addBudgetItemState.name,
                                        targetAmount = target,
                                        memo = addBudgetItemState.memo,
                                    )
                                )
                            }

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

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "삭제할까요?") },
            text = {
                Text(
                    text = when (target) {
                        is DeleteTarget.Expense -> "이 지출을 삭제할까요?"
                        is DeleteTarget.BudgetItem -> "이 예산 항목을 삭제할까요?\n연결된 지출도 함께 삭제됩니다."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (target) {
                            is DeleteTarget.Expense -> viewModel.onAction(MoneyAction.OnDeleteExpense(target.id))
                            is DeleteTarget.BudgetItem -> viewModel.onAction(MoneyAction.OnDeleteBudgetItem(target.id))
                        }

                        deleteTarget = null
                        activeSheet = null
                        showDatePicker = false
                        addExpenseState = newAddExpenseState(uiState)
                        addBudgetItemState = newAddBudgetItemState(
                            uiState = uiState,
                            preselectedCategoryId = uiState.budget.categories.firstOrNull()?.id,
                        )
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = "취소")
                }
            },
        )
    }
}

private sealed interface MoneySheet {
    data object AddExpense : MoneySheet
    data class EditExpense(val expenseId: String) : MoneySheet
    data object AddBudgetItem : MoneySheet
    data class EditBudgetItem(val budgetItemId: String) : MoneySheet
}

private sealed interface DeleteTarget {
    data class Expense(val id: String) : DeleteTarget
    data class BudgetItem(val id: String) : DeleteTarget
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

private fun newEditExpenseState(
    uiState: MoneyUiState,
    item: ExpenseListItem,
): AddExpenseUiState {
    return AddExpenseUiState(
        amount = formatNumber(item.amount),
        date = item.date,
        dateText = item.date.toKoreanDateText(),
        budgetItemOptions = uiState.expenses.budgetItemOptions,
        selectedBudgetItemId = item.budgetItemId,
        paymentMethod = item.paymentMethod,
        payer = item.payer,
        memo = item.memo.orEmpty(),
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

private fun newEditBudgetItemState(
    uiState: MoneyUiState,
    categoryId: String,
    item: BudgetSubItem,
): AddBudgetItemUiState {
    val options = uiState.budget.categories.map { CategoryOption(id = it.id, label = it.name) }
    return AddBudgetItemUiState(
        categoryOptions = options,
        selectedCategoryId = categoryId,
        name = item.label,
        targetAmount = formatNumber(item.targetAmount),
        memo = item.memo.orEmpty(),
        helperText = null,
    )
}

private fun parseAmountOrNull(value: String): Int? {
    val digits = value.filter { it.isDigit() }
    return digits.toIntOrNull()
}

private fun formatNumber(value: Int): String = value
    .coerceAtLeast(0)
    .toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

private data class FoundBudgetItem(
    val categoryId: String,
    val item: BudgetSubItem,
)

private fun findBudgetItem(uiState: MoneyUiState, budgetItemId: String): FoundBudgetItem? {
    uiState.budget.categories.forEach { category ->
        category.subItems.firstOrNull { it.id == budgetItemId }?.let { item ->
            return FoundBudgetItem(categoryId = category.id, item = item)
        }
    }
    return null
}

private fun findExpense(uiState: MoneyUiState, expenseId: String): ExpenseListItem? {
    return uiState.expenses.sections
        .asSequence()
        .flatMap { it.items.asSequence() }
        .firstOrNull { it.id == expenseId }
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

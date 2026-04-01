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
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Instant
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyRoute() {
    MoneyRoute(
        onOpenNotifications = {},
        onOpenSettings = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyRoute(
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: MoneyViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<MoneySheet?>(null) }
    var addExpenseState by remember { mutableStateOf(AddExpenseUiState.empty()) }
    var addBudgetItemState by remember { mutableStateOf(AddBudgetItemUiState.empty()) }
    var manageCategoriesState by remember { mutableStateOf(ManageCategoriesUiState.empty()) }
    var showExpenseDatePicker by remember { mutableStateOf(false) }
    var showBudgetDueDatePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
    var pendingSave by remember { mutableStateOf<MoneyPendingSave?>(null) }

    LaunchedEffect(activeSheet) {
        if (activeSheet == null) {
            showExpenseDatePicker = false
            showBudgetDueDatePicker = false
            pendingSave = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MoneyEffect.ExpenseSaved -> {
                    pendingSave = null
                    activeSheet = null
                    showExpenseDatePicker = false
                }

                MoneyEffect.ExpenseSaveFailed -> {
                    if (pendingSave == MoneyPendingSave.Expense) {
                        pendingSave = null
                    }
                }

                MoneyEffect.BudgetItemSaved -> {
                    pendingSave = null
                    activeSheet = null
                    showBudgetDueDatePicker = false
                }

                MoneyEffect.BudgetItemSaveFailed -> {
                    if (pendingSave == MoneyPendingSave.BudgetItem) {
                        pendingSave = null
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.budget.categories, activeSheet) {
        if (activeSheet == MoneySheet.ManageCategories) {
            manageCategoriesState = manageCategoriesState.copy(
                categories = uiState.budget.categories.map { EditableCategoryItem(id = it.id, name = it.name) }
            )
        }
    }

    MoneyScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                MoneyAction.OnClickAdd -> {
                    when {
                        uiState.budget.categories.isEmpty() -> {
                            activeSheet = MoneySheet.ManageCategories
                            manageCategoriesState = newManageCategoriesState(uiState)
                        }
                        else -> {
                            activeSheet = MoneySheet.AddExpense
                            addExpenseState = newAddExpenseState(uiState)
                        }
                    }
                }
                MoneyAction.OnClickAddBudgetItemEntry -> {
                    if (uiState.budget.categories.isEmpty()) {
                        activeSheet = MoneySheet.ManageCategories
                        manageCategoriesState = newManageCategoriesState(uiState)
                    } else {
                        activeSheet = MoneySheet.AddBudgetItem
                        addBudgetItemState = newAddBudgetItemState(
                            uiState = uiState,
                            preselectedCategoryId = null,
                        )
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
                MoneyAction.OnClickManageCategories -> {
                    activeSheet = MoneySheet.ManageCategories
                    manageCategoriesState = newManageCategoriesState(uiState)
                }
                MoneyAction.OnClickNotifications -> onOpenNotifications()
                MoneyAction.OnClickSettings -> onOpenSettings()
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
            val isSubmitting = pendingSave == MoneyPendingSave.Expense
            AddExpenseBottomSheet(
                title = if (editingId == null) "지출 추가" else "지출 수정",
                state = addExpenseState,
                showDelete = editingId != null,
                isSubmitting = isSubmitting,
                onDismiss = {
                    if (!isSubmitting) {
                        activeSheet = null
                        showExpenseDatePicker = false
                    }
                },
                onAction = { act ->
                    when (act) {
                        AddExpenseAction.OnClickClose -> {
                            if (!isSubmitting) {
                                activeSheet = null
                                showExpenseDatePicker = false
                            }
                        }
                        AddExpenseAction.OnClickDelete -> {
                            val id = editingId ?: return@AddExpenseBottomSheet
                            deleteTarget = DeleteTarget.Expense(id)
                        }
                        AddExpenseAction.OnClickSave -> {
                            val categoryId = addExpenseState.selectedCategoryId ?: return@AddExpenseBottomSheet
                            val amount = parseAmountOrNull(addExpenseState.amount) ?: return@AddExpenseBottomSheet
                            val budgetItemId = addExpenseState.selectedBudgetItemId
                                ?.takeIf { selectedId ->
                                    addExpenseState.filteredBudgetItemOptions.any { it.id == selectedId }
                                }

                            if (editingId == null) {
                                viewModel.onAction(
                                    MoneyAction.OnCreateExpense(
                                        categoryId = categoryId,
                                        budgetItemId = budgetItemId,
                                        amount = amount,
                                        date = addExpenseState.date,
                                        paymentMethod = addExpenseState.paymentMethod,
                                        payer = addExpenseState.payer,
                                        vendorName = addExpenseState.vendorName,
                                        memo = addExpenseState.memo,
                                    )
                                )
                            } else {
                                viewModel.onAction(
                                    MoneyAction.OnUpdateExpense(
                                        id = editingId,
                                        categoryId = categoryId,
                                        budgetItemId = budgetItemId,
                                        amount = amount,
                                        date = addExpenseState.date,
                                        paymentMethod = addExpenseState.paymentMethod,
                                        payer = addExpenseState.payer,
                                        vendorName = addExpenseState.vendorName,
                                        memo = addExpenseState.memo,
                                    )
                                )
                            }
                            pendingSave = MoneyPendingSave.Expense
                        }
                        AddExpenseAction.OnClickDate -> {
                            if (!isSubmitting) {
                                showExpenseDatePicker = true
                            }
                        }
                        is AddExpenseAction.OnAmountChange -> addExpenseState = addExpenseState.copy(amount = act.value)
                        is AddExpenseAction.OnVendorNameChange -> addExpenseState = addExpenseState.copy(vendorName = act.value)
                        is AddExpenseAction.OnMemoChange -> addExpenseState = addExpenseState.copy(memo = act.value)
                        is AddExpenseAction.OnSelectCategory -> {
                            val preservedBudgetItemId = addExpenseState.selectedBudgetItemId
                                ?.takeIf { selectedId ->
                                    addExpenseState.budgetItemOptions.any { option ->
                                        option.id == selectedId && option.categoryId == act.id
                                    }
                                }
                            addExpenseState = addExpenseState.copy(
                                selectedCategoryId = act.id,
                                selectedBudgetItemId = preservedBudgetItemId,
                            )
                        }
                        is AddExpenseAction.OnSelectBudgetItem -> addExpenseState = addExpenseState.copy(selectedBudgetItemId = act.id)
                        is AddExpenseAction.OnSelectPaymentMethod -> {
                            addExpenseState = when (act.method) {
                                PaymentMethod.JointAccount -> {
                                    addExpenseState.copy(
                                        paymentMethod = PaymentMethod.JointAccount,
                                        payer = Payer.Joint,
                                    )
                                }

                                else -> {
                                    addExpenseState.copy(
                                        paymentMethod = act.method,
                                        payer = addExpenseState.payer,
                                    )
                                }
                            }
                        }
                        is AddExpenseAction.OnSelectPayer -> addExpenseState = addExpenseState.copy(payer = act.payer)
                    }
                }
            )

            if (showExpenseDatePicker) {
                // Material3 DatePicker works with UTC milliseconds (date at 00:00 UTC).
                val timeZone = TimeZone.UTC
                val initialMillis = addExpenseState.date
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showExpenseDatePicker = false },
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
                                showExpenseDatePicker = false
                            }
                        ) {
                            Text(text = "확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExpenseDatePicker = false }) {
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
            val isSubmitting = pendingSave == MoneyPendingSave.BudgetItem
            AddBudgetItemBottomSheet(
                title = if (editingId == null) "예산 항목 추가" else "예산 항목 수정",
                state = addBudgetItemState,
                showDelete = editingId != null,
                isSubmitting = isSubmitting,
                onDismiss = {
                    if (!isSubmitting) {
                        activeSheet = null
                    }
                },
                onAction = { act ->
                    when (act) {
                        AddBudgetItemAction.OnClickClose -> {
                            if (!isSubmitting) {
                                activeSheet = null
                            }
                        }
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
                                        dueDate = addBudgetItemState.dueDate,
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
                                        dueDate = addBudgetItemState.dueDate,
                                        memo = addBudgetItemState.memo,
                                    )
                                )
                            }
                            pendingSave = MoneyPendingSave.BudgetItem
                        }
                        AddBudgetItemAction.OnClickDueDate -> {
                            if (!isSubmitting) {
                                showBudgetDueDatePicker = true
                            }
                        }
                        AddBudgetItemAction.OnClickClearDueDate -> {
                            addBudgetItemState = addBudgetItemState.copy(
                                dueDate = null,
                                dueDateText = null,
                            )
                        }
                        is AddBudgetItemAction.OnSelectCategory -> addBudgetItemState = addBudgetItemState.copy(selectedCategoryId = act.id)
                        is AddBudgetItemAction.OnNameChange -> addBudgetItemState = addBudgetItemState.copy(name = act.value)
                        is AddBudgetItemAction.OnTargetAmountChange -> addBudgetItemState = addBudgetItemState.copy(targetAmount = act.value)
                        is AddBudgetItemAction.OnMemoChange -> addBudgetItemState = addBudgetItemState.copy(memo = act.value)
                    }
                }
            )

            if (showBudgetDueDatePicker) {
                val timeZone = TimeZone.UTC
                val initialDate = addBudgetItemState.dueDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
                val initialMillis = initialDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showBudgetDueDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val date = Instant
                                        .fromEpochMilliseconds(millis)
                                        .toLocalDateTime(timeZone)
                                        .date
                                    addBudgetItemState = addBudgetItemState.copy(
                                        dueDate = date,
                                        dueDateText = date.toKoreanDateText(),
                                    )
                                }
                                showBudgetDueDatePicker = false
                            }
                        ) {
                            Text(text = "확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBudgetDueDatePicker = false }) {
                            Text(text = "취소")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
        MoneySheet.ManageCategories -> {
            ManageCategoriesBottomSheet(
                state = manageCategoriesState,
                onDismiss = { activeSheet = null },
                onAction = { act ->
                    when (act) {
                        ManageCategoriesAction.OnClickClose -> activeSheet = null
                        is ManageCategoriesAction.OnNewCategoryNameChange -> {
                            manageCategoriesState = manageCategoriesState.copy(newCategoryName = act.value)
                        }
                        ManageCategoriesAction.OnClickAddCategory -> {
                            val newName = manageCategoriesState.newCategoryName.trim()
                            if (newName.isEmpty()) return@ManageCategoriesBottomSheet
                            viewModel.onAction(MoneyAction.OnCreateCategory(name = newName))
                            manageCategoriesState = manageCategoriesState.copy(newCategoryName = "")
                        }
                        is ManageCategoriesAction.OnClickDeleteCategory -> {
                            deleteTarget = DeleteTarget.Category(act.id)
                        }
                        is ManageCategoriesAction.OnStartEditCategory -> {
                            val target = manageCategoriesState.categories.firstOrNull { it.id == act.id } ?: return@ManageCategoriesBottomSheet
                            manageCategoriesState = manageCategoriesState.copy(
                                editingCategoryId = target.id,
                                editingCategoryName = target.name,
                            )
                        }
                        is ManageCategoriesAction.OnEditCategoryNameChange -> {
                            manageCategoriesState = manageCategoriesState.copy(editingCategoryName = act.value)
                        }
                        ManageCategoriesAction.OnClickCancelCategoryEdit -> {
                            manageCategoriesState = manageCategoriesState.copy(
                                editingCategoryId = null,
                                editingCategoryName = "",
                            )
                        }
                        ManageCategoriesAction.OnClickSaveCategoryEdit -> {
                            val id = manageCategoriesState.editingCategoryId ?: return@ManageCategoriesBottomSheet
                            val editedName = manageCategoriesState.editingCategoryName.trim()
                            if (editedName.isEmpty()) return@ManageCategoriesBottomSheet
                            viewModel.onAction(
                                MoneyAction.OnUpdateCategory(
                                    id = id,
                                    name = editedName,
                                )
                            )
                            manageCategoriesState = manageCategoriesState.copy(
                                editingCategoryId = null,
                                editingCategoryName = "",
                            )
                        }
                    }
                },
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
                        is DeleteTarget.Category -> "이 카테고리를 삭제할까요?\n지출이나 예산 항목이 남아 있으면 삭제되지 않습니다."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (target) {
                            is DeleteTarget.Category -> viewModel.onAction(MoneyAction.OnDeleteCategory(target.id))
                            is DeleteTarget.Expense -> viewModel.onAction(MoneyAction.OnDeleteExpense(target.id))
                            is DeleteTarget.BudgetItem -> viewModel.onAction(MoneyAction.OnDeleteBudgetItem(target.id))
                        }

                        deleteTarget = null
                        activeSheet = null
                        showExpenseDatePicker = false
                        showBudgetDueDatePicker = false
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
    data object ManageCategories : MoneySheet
}

private enum class MoneyPendingSave {
    Expense,
    BudgetItem,
}

private sealed interface DeleteTarget {
    data class Category(val id: String) : DeleteTarget
    data class Expense(val id: String) : DeleteTarget
    data class BudgetItem(val id: String) : DeleteTarget
}

private fun newAddExpenseState(uiState: MoneyUiState): AddExpenseUiState {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val categoryOptions = uiState.budget.categories.map { CategoryOption(id = it.id, label = it.name) }
    return AddExpenseUiState(
        categoryOptions = categoryOptions,
        selectedCategoryId = categoryOptions.firstOrNull()?.id,
        amount = "",
        date = today,
        dateText = today.toKoreanDateText(),
        budgetItemOptions = uiState.expenses.budgetItemOptions,
        selectedBudgetItemId = null,
        paymentMethod = kr.jm.weddingledger.feature.money.domain.model.PaymentMethod.Card,
        payer = uiState.currentUserRole.toDefaultExpensePayer(),
        vendorName = "",
        memo = "",
    )
}

private fun newEditExpenseState(
    uiState: MoneyUiState,
    item: ExpenseListItem,
): AddExpenseUiState {
    val categoryOptions = uiState.budget.categories.map { CategoryOption(id = it.id, label = it.name) }
    return AddExpenseUiState(
        categoryOptions = categoryOptions,
        selectedCategoryId = item.categoryId,
        amount = formatNumber(item.amount),
        date = item.date,
        dateText = item.date.toKoreanDateText(),
        budgetItemOptions = uiState.expenses.budgetItemOptions,
        selectedBudgetItemId = item.budgetItemId,
        paymentMethod = item.paymentMethod,
        payer = item.payer,
        vendorName = item.vendorName.orEmpty(),
        memo = item.memo.orEmpty(),
    )
}

private fun newManageCategoriesState(uiState: MoneyUiState): ManageCategoriesUiState {
    return ManageCategoriesUiState(
        categories = uiState.budget.categories.map { EditableCategoryItem(id = it.id, name = it.name) },
        newCategoryName = "",
        editingCategoryId = null,
        editingCategoryName = "",
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
        dueDate = null,
        dueDateText = null,
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
        dueDate = item.dueDate,
        dueDateText = item.dueDate?.toKoreanDateText(),
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
    return (
        uiState.expenses.sections
        .asSequence()
        .flatMap { it.items.asSequence() }
            + uiState.budget.categories
            .asSequence()
            .flatMap { it.recentExpenses.asSequence() }
        )
        .firstOrNull { it.id == expenseId }
}

private fun kotlinx.datetime.LocalDate.toKoreanDateText(): String {
    val mm = month.number.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
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

package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

enum class MoneyTab(val label: String) {
    Budget("예산"),
    Expenses("지출"),
}

data class MoneyUiState(
    val selectedTab: MoneyTab,
    val currentUserRole: UserRole? = null,
    val expenses: ExpensesUiState,
    val budget: BudgetUiState,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

data class ExpensesUiState(
    val monthTitle: String,
    val totalSpentKrw: String,
    val categoryFilters: List<ExpenseFilterChip>,
    val periodFilters: List<ExpensePeriodFilterChip>,
    val statusFilters: List<ExpenseStatusFilterChip>,
    val presets: List<ExpenseFilterPresetChip>,
    val sections: List<ExpenseSection>,
    val budgetItemOptions: List<BudgetItemOption>,
) {
    companion object {
        fun empty(): ExpensesUiState = ExpensesUiState(
            monthTitle = "전체 기간 지출",
            totalSpentKrw = "0",
            categoryFilters = listOf(ExpenseFilterChip(id = null, label = "전체", selected = true)),
            periodFilters = listOf(ExpensePeriodFilterChip(option = ExpensePeriodFilterOption.All, selected = true)),
            statusFilters = listOf(ExpenseStatusFilterChip(option = ExpenseStatusFilterOption.All, selected = true)),
            presets = emptyList(),
            sections = emptyList(),
            budgetItemOptions = emptyList(),
        )
    }
}

data class ExpenseFilterChip(
    val id: String?,
    val label: String,
    val selected: Boolean,
)

enum class ExpensePeriodFilterOption(val label: String) {
    All("전체 기간"),
    ThisMonth("이번 달"),
    Last3Months("최근 3개월"),
}

data class ExpensePeriodFilterChip(
    val option: ExpensePeriodFilterOption,
    val selected: Boolean,
)

enum class ExpenseStatusFilterOption(val label: String) {
    All("전체"),
    HighAmount("50만원 이상"),
    HasMemo("메모 있음"),
}

data class ExpenseStatusFilterChip(
    val option: ExpenseStatusFilterOption,
    val selected: Boolean,
)

data class ExpenseFilterPresetChip(
    val id: String,
    val label: String,
    val selected: Boolean,
)

data class ExpenseSection(
    val title: String,
    val items: List<ExpenseListItem>,
)

data class ExpenseListItem(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val budgetItemId: String?,
    val title: String,
    val dateText: String,
    val amountText: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val vendorName: String?,
    val memo: String?,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
)

data class BudgetUiState(
    val totalBudgetText: String,
    val usedPercentText: String,
    val usedAmountText: String,
    val remainingAmountText: String,
    val usedProgress: Float,
    val insightItems: List<BudgetInsightItem>,
    val payerSummaries: List<PayerSummaryItem>,
    val pendingPayments: List<PendingPaymentItem>,
    val isOverBudgetOnly: Boolean = false,
    val categories: List<BudgetCategoryItem>,
) {
    companion object {
        fun empty(): BudgetUiState = BudgetUiState(
            totalBudgetText = "₩0",
            usedPercentText = "0% 사용",
            usedAmountText = "₩0",
            remainingAmountText = "₩0 남음",
            usedProgress = 0f,
            insightItems = emptyList(),
            payerSummaries = emptyList(),
            pendingPayments = emptyList(),
            isOverBudgetOnly = false,
            categories = emptyList(),
        )
    }
}

data class BudgetInsightItem(
    val label: String,
    val value: String,
    val emphasis: BudgetInsightEmphasis = BudgetInsightEmphasis.Neutral,
)

enum class BudgetInsightEmphasis {
    Neutral,
    Highlight,
    Caution,
}

data class PayerSummaryItem(
    val label: String,
    val amountText: String,
    val ratioText: String,
    val progress: Float,
)

data class PendingPaymentItem(
    val id: String,
    val categoryName: String,
    val title: String,
    val dueDate: LocalDate? = null,
    val dueDateText: String? = null,
    val amountText: String,
    val paidText: String,
    val targetText: String,
    val priorityText: String? = null,
)

data class BudgetCategoryItem(
    val id: String,
    val name: String,
    val itemCountText: String,
    val usedText: String,
    val targetText: String,
    val remainingText: String,
    val progress: Float,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
    val isExpanded: Boolean,
    val subItems: List<BudgetSubItem>,
    val expenseCountText: String = "지출 기록 없음",
    val recentExpenses: List<ExpenseListItem> = emptyList(),
    val isOverBudget: Boolean = false,
)

data class BudgetSubItem(
    val id: String,
    val label: String,
    val valueText: String,
    val isEmpty: Boolean = false,
    val targetAmount: Int,
    val memo: String? = null,
    val spentText: String = valueText,
    val targetText: String = valueText,
    val remainingText: String = valueText,
    val progress: Float = 0f,
    val isOverBudget: Boolean = false,
    val dueDate: LocalDate? = null,
    val dueDateText: String? = null,
    val expenseCountText: String = "지출 기록 없음",
)

sealed interface MoneyAction {
    data class OnSelectTab(val tab: MoneyTab) : MoneyAction
    data class OnSelectExpenseFilter(val categoryId: String?) : MoneyAction
    data class OnSelectExpensePeriodFilter(val option: ExpensePeriodFilterOption) : MoneyAction
    data class OnSelectExpenseStatusFilter(val option: ExpenseStatusFilterOption) : MoneyAction
    data object OnSaveExpenseFilterPreset : MoneyAction
    data class OnSelectExpenseFilterPreset(val id: String) : MoneyAction
    data object OnClickAdd : MoneyAction
    data object OnClickAddBudgetItemEntry : MoneyAction
    data object OnClickManageCategories : MoneyAction
    data object OnClickNotifications : MoneyAction
    data object OnClickSettings : MoneyAction
    data object OnToggleBudgetFilter : MoneyAction
    data class OnToggleBudgetCategory(val id: String) : MoneyAction
    data class OnClickBudgetAddItem(val categoryId: String) : MoneyAction
    data class OnClickBudgetItem(val id: String) : MoneyAction
    data class OnClickExpense(val id: String) : MoneyAction

    data object OnRefresh : MoneyAction

    data class OnCreateBudgetItem(
        val categoryId: String,
        val name: String,
        val targetAmount: Int,
        val dueDate: LocalDate?,
        val memo: String?,
    ) : MoneyAction

    data class OnCreateCategory(
        val name: String,
    ) : MoneyAction

    data class OnCreateExpense(
        val categoryId: String,
        val budgetItemId: String?,
        val amount: Int,
        val date: LocalDate,
        val paymentMethod: PaymentMethod,
        val payer: Payer,
        val vendorName: String?,
        val memo: String?,
    ) : MoneyAction

    data class OnUpdateBudgetItem(
        val id: String,
        val categoryId: String,
        val name: String,
        val targetAmount: Int,
        val dueDate: LocalDate?,
        val memo: String?,
    ) : MoneyAction

    data class OnUpdateCategory(
        val id: String,
        val name: String,
    ) : MoneyAction

    data class OnDeleteCategory(val id: String) : MoneyAction

    data class OnUpdateExpense(
        val id: String,
        val categoryId: String,
        val budgetItemId: String?,
        val amount: Int,
        val date: LocalDate,
        val paymentMethod: PaymentMethod,
        val payer: Payer,
        val vendorName: String?,
        val memo: String?,
    ) : MoneyAction

    data class OnDeleteBudgetItem(val id: String) : MoneyAction
    data class OnDeleteExpense(val id: String) : MoneyAction
}

sealed interface MoneyEffect {
    data object ExpenseSaved : MoneyEffect
    data object ExpenseSaveFailed : MoneyEffect
    data object BudgetItemSaved : MoneyEffect
    data object BudgetItemSaveFailed : MoneyEffect
}

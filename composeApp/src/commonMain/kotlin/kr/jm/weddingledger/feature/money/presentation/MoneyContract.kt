package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Print
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod

enum class MoneyTab(val label: String) {
    Expenses("지출"),
    Budget("예산"),
}

data class MoneyUiState(
    val selectedTab: MoneyTab,
    val expenses: ExpensesUiState,
    val budget: BudgetUiState,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null,
) {
    companion object {
        fun sample(): MoneyUiState = MoneyUiState(
            selectedTab = MoneyTab.Expenses,
            expenses = ExpensesUiState.sample(),
            budget = BudgetUiState.sample()
        )
    }
}

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
            monthTitle = "이번 달 지출",
            totalSpentKrw = "0",
            categoryFilters = listOf(ExpenseFilterChip(id = null, label = "전체", selected = true)),
            periodFilters = listOf(ExpensePeriodFilterChip(option = ExpensePeriodFilterOption.All, selected = true)),
            statusFilters = listOf(ExpenseStatusFilterChip(option = ExpenseStatusFilterOption.All, selected = true)),
            presets = emptyList(),
            sections = emptyList(),
            budgetItemOptions = emptyList(),
        )

        fun sample(): ExpensesUiState = ExpensesUiState(
            monthTitle = "이번 달 지출",
            totalSpentKrw = "2,450,000",
            categoryFilters = listOf(
                ExpenseFilterChip(id = null, label = "전체", selected = true),
                ExpenseFilterChip(id = "food", label = "식비", selected = false),
                ExpenseFilterChip(id = "contract", label = "계약금", selected = false),
                ExpenseFilterChip(id = "dress", label = "드레스", selected = false),
                ExpenseFilterChip(id = "studio", label = "스튜디오", selected = false),
            ),
            periodFilters = ExpensePeriodFilterOption.entries.map {
                ExpensePeriodFilterChip(option = it, selected = it == ExpensePeriodFilterOption.ThisMonth)
            },
            statusFilters = ExpenseStatusFilterOption.entries.map {
                ExpenseStatusFilterChip(option = it, selected = it == ExpenseStatusFilterOption.All)
            },
            presets = listOf(
                ExpenseFilterPresetChip(id = "preset-1", label = "이번 달 고액", selected = false),
                ExpenseFilterPresetChip(id = "preset-2", label = "드레스 메모", selected = false),
            ),
            sections = listOf(
                ExpenseSection(
                    title = "3월",
                    items = listOf(
                        ExpenseListItem(
                            id = "exp-1",
                            budgetItemId = "budget-2",
                            title = "드레스 대여",
                            dateText = "3월 15일",
                            amountText = "-1,200,000",
                            amount = 1_200_000,
                            date = LocalDate.parse("2026-03-15"),
                            paymentMethod = PaymentMethod.Card,
                            payer = Payer.Bride,
                            memo = null,
                            icon = Icons.Rounded.Checkroom,
                            accent = Color(0xFFEF3948),
                            accentContainer = Color(0xFFFDE9EB),
                        ),
                        ExpenseListItem(
                            id = "exp-2",
                            budgetItemId = "budget-1",
                            title = "웨딩홀 계약금",
                            dateText = "3월 10일",
                            amountText = "-1,000,000",
                            amount = 1_000_000,
                            date = LocalDate.parse("2026-03-10"),
                            paymentMethod = PaymentMethod.Card,
                            payer = Payer.Groom,
                            memo = null,
                            icon = Icons.Rounded.Gavel,
                            accent = Color(0xFFF97316),
                            accentContainer = Color(0xFFFFEDD5),
                        ),
                        ExpenseListItem(
                            id = "exp-3",
                            budgetItemId = "budget-1",
                            title = "청첩장 인쇄",
                            dateText = "3월 05일",
                            amountText = "-250,000",
                            amount = 250_000,
                            date = LocalDate.parse("2026-03-05"),
                            paymentMethod = PaymentMethod.Cash,
                            payer = Payer.Bride,
                            memo = null,
                            icon = Icons.Rounded.Print,
                            accent = Color(0xFF3B82F6),
                            accentContainer = Color(0xFFDBEAFE),
                        ),
                    )
                ),
                ExpenseSection(
                    title = "2월",
                    items = listOf(
                        ExpenseListItem(
                            id = "exp-4",
                            budgetItemId = "budget-2",
                            title = "스냅 사진 예약",
                            dateText = "2월 28일",
                            amountText = "-500,000",
                            amount = 500_000,
                            date = LocalDate.parse("2026-02-28"),
                            paymentMethod = PaymentMethod.Card,
                            payer = Payer.Bride,
                            memo = null,
                            icon = Icons.Rounded.CameraAlt,
                            accent = Color(0xFFA855F7),
                            accentContainer = Color(0xFFF3E8FF),
                        )
                    )
                ),
            ),
            budgetItemOptions = listOf(
                BudgetItemOption(id = "budget-1", label = "웨딩홀 · 계약금"),
                BudgetItemOption(id = "budget-2", label = "스드메 · 드레스"),
            ),
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
    val budgetItemId: String,
    val title: String,
    val dateText: String,
    val amountText: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
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
            isOverBudgetOnly = false,
            categories = emptyList(),
        )

        fun sample(): BudgetUiState = BudgetUiState(
            totalBudgetText = "3,000만원",
            usedPercentText = "41% 사용",
            usedAmountText = "1,250만원",
            remainingAmountText = "1,750만원 남음",
            usedProgress = 0.41f,
            isOverBudgetOnly = false,
            categories = listOf(
                BudgetCategoryItem(
                    id = "wedding_hall",
                    name = "웨딩홀",
                    itemCountText = "3개 항목",
                    usedText = "300만원 사용",
                    targetText = "500만원 목표",
                    progress = 0.60f,
                    icon = Icons.Rounded.Gavel,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                    isExpanded = true,
                    subItems = listOf(
                        BudgetSubItem(id = "budget-11", label = "대관료", valueText = "200만원", targetAmount = 2_000_000),
                        BudgetSubItem(id = "budget-12", label = "식대 (계약금)", valueText = "100만원", targetAmount = 1_000_000),
                        BudgetSubItem(id = "budget-13", label = "부대비용", valueText = "미지출", isEmpty = true, targetAmount = 0),
                    )
                ),
                BudgetCategoryItem(
                    id = "sdme",
                    name = "스드메",
                    itemCountText = "2개 항목",
                    usedText = "250만원 사용",
                    targetText = "300만원 목표",
                    progress = 0.83f,
                    icon = Icons.Rounded.CameraAlt,
                    accent = Color(0xFFF97316),
                    accentContainer = Color(0xFFFFEDD5),
                    isExpanded = false,
                    subItems = emptyList()
                ),
                BudgetCategoryItem(
                    id = "honeymoon",
                    name = "신혼여행",
                    itemCountText = "1개 항목",
                    usedText = "420만원 사용",
                    targetText = "400만원 목표",
                    progress = 1.0f,
                    icon = Icons.Rounded.Checkroom,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                    isExpanded = false,
                    subItems = emptyList(),
                    isOverBudget = true
                ),
            )
        )
    }
}

data class BudgetCategoryItem(
    val id: String,
    val name: String,
    val itemCountText: String,
    val usedText: String,
    val targetText: String,
    val progress: Float,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
    val isExpanded: Boolean,
    val subItems: List<BudgetSubItem>,
    val isOverBudget: Boolean = false,
)

data class BudgetSubItem(
    val id: String,
    val label: String,
    val valueText: String,
    val isEmpty: Boolean = false,
    val targetAmount: Int,
    val memo: String? = null,
)

sealed interface MoneyAction {
    data class OnSelectTab(val tab: MoneyTab) : MoneyAction
    data class OnSelectExpenseFilter(val categoryId: String?) : MoneyAction
    data class OnSelectExpensePeriodFilter(val option: ExpensePeriodFilterOption) : MoneyAction
    data class OnSelectExpenseStatusFilter(val option: ExpenseStatusFilterOption) : MoneyAction
    data object OnSaveExpenseFilterPreset : MoneyAction
    data class OnSelectExpenseFilterPreset(val id: String) : MoneyAction
    data object OnClickAdd : MoneyAction
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
        val memo: String?,
    ) : MoneyAction

    data class OnCreateExpense(
        val budgetItemId: String,
        val amount: Int,
        val date: LocalDate,
        val paymentMethod: PaymentMethod,
        val payer: Payer,
        val memo: String?,
    ) : MoneyAction

    data class OnUpdateBudgetItem(
        val id: String,
        val categoryId: String,
        val name: String,
        val targetAmount: Int,
        val memo: String?,
    ) : MoneyAction

    data class OnUpdateExpense(
        val id: String,
        val budgetItemId: String,
        val amount: Int,
        val date: LocalDate,
        val paymentMethod: PaymentMethod,
        val payer: Payer,
        val memo: String?,
    ) : MoneyAction

    data class OnDeleteBudgetItem(val id: String) : MoneyAction
    data class OnDeleteExpense(val id: String) : MoneyAction
}

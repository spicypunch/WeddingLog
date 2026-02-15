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
    val filters: List<ExpenseFilterChip>,
    val sections: List<ExpenseSection>,
    val budgetItemOptions: List<BudgetItemOption>,
) {
    companion object {
        fun empty(): ExpensesUiState = ExpensesUiState(
            monthTitle = "이번 달 지출",
            totalSpentKrw = "0",
            filters = listOf(ExpenseFilterChip(id = null, label = "전체", selected = true)),
            sections = emptyList(),
            budgetItemOptions = emptyList(),
        )

        fun sample(): ExpensesUiState = ExpensesUiState(
            monthTitle = "이번 달 지출",
            totalSpentKrw = "2,450,000",
            filters = listOf(
                ExpenseFilterChip(id = null, label = "전체", selected = true),
                ExpenseFilterChip(id = "food", label = "식비", selected = false),
                ExpenseFilterChip(id = "contract", label = "계약금", selected = false),
                ExpenseFilterChip(id = "dress", label = "드레스", selected = false),
                ExpenseFilterChip(id = "studio", label = "스튜디오", selected = false),
            ),
            sections = listOf(
                ExpenseSection(
                    title = "3월",
                    items = listOf(
                        ExpenseListItem(
                            id = "exp-1",
                            title = "드레스 대여",
                            dateText = "3월 15일",
                            amountText = "-1,200,000",
                            icon = Icons.Rounded.Checkroom,
                            accent = Color(0xFFEF3948),
                            accentContainer = Color(0xFFFDE9EB),
                        ),
                        ExpenseListItem(
                            id = "exp-2",
                            title = "웨딩홀 계약금",
                            dateText = "3월 10일",
                            amountText = "-1,000,000",
                            icon = Icons.Rounded.Gavel,
                            accent = Color(0xFFF97316),
                            accentContainer = Color(0xFFFFEDD5),
                        ),
                        ExpenseListItem(
                            id = "exp-3",
                            title = "청첩장 인쇄",
                            dateText = "3월 05일",
                            amountText = "-250,000",
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
                            title = "스냅 사진 예약",
                            dateText = "2월 28일",
                            amountText = "-500,000",
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

data class ExpenseSection(
    val title: String,
    val items: List<ExpenseListItem>,
)

data class ExpenseListItem(
    val id: String,
    val title: String,
    val dateText: String,
    val amountText: String,
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
    val categories: List<BudgetCategoryItem>,
) {
    companion object {
        fun empty(): BudgetUiState = BudgetUiState(
            totalBudgetText = "₩0",
            usedPercentText = "0% 사용",
            usedAmountText = "₩0",
            remainingAmountText = "₩0 남음",
            usedProgress = 0f,
            categories = emptyList(),
        )

        fun sample(): BudgetUiState = BudgetUiState(
            totalBudgetText = "3,000만원",
            usedPercentText = "41% 사용",
            usedAmountText = "1,250만원",
            remainingAmountText = "1,750만원 남음",
            usedProgress = 0.41f,
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
                        BudgetSubItem(label = "대관료", valueText = "200만원"),
                        BudgetSubItem(label = "식대 (계약금)", valueText = "100만원"),
                        BudgetSubItem(label = "부대비용", valueText = "미지출", isEmpty = true),
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
    val label: String,
    val valueText: String,
    val isEmpty: Boolean = false,
)

sealed interface MoneyAction {
    data class OnSelectTab(val tab: MoneyTab) : MoneyAction
    data class OnSelectExpenseFilter(val categoryId: String?) : MoneyAction
    data object OnClickAdd : MoneyAction
    data class OnToggleBudgetCategory(val id: String) : MoneyAction
    data class OnClickBudgetAddItem(val categoryId: String) : MoneyAction
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
}

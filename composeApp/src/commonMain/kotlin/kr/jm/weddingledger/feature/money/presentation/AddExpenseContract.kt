package kr.jm.weddingledger.feature.money.presentation

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

data class BudgetItemOption(
    val id: String,
    val categoryId: String,
    val label: String,
)

data class AddExpenseUiState(
    val amount: String,
    val date: LocalDate,
    val dateText: String,
    val categoryOptions: List<CategoryOption>,
    val selectedCategoryId: String?,
    val budgetItemOptions: List<BudgetItemOption>,
    val selectedBudgetItemId: String?,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val vendorName: String,
    val memo: String,
) {
    val filteredBudgetItemOptions: List<BudgetItemOption>
        get() = selectedCategoryId
            ?.let { categoryId -> budgetItemOptions.filter { it.categoryId == categoryId } }
            .orEmpty()

    val isSaveEnabled: Boolean
        get() = amount.trim().isNotEmpty() && selectedCategoryId != null

    companion object {
        fun sample(): AddExpenseUiState = AddExpenseUiState(
            amount = "",
            date = LocalDate.parse("2023-10-25"),
            dateText = "2023.10.25 (수)",
            categoryOptions = listOf(
                CategoryOption(id = "cat-1", label = "웨딩홀"),
                CategoryOption(id = "cat-2", label = "스드메"),
            ),
            selectedCategoryId = "cat-1",
            budgetItemOptions = listOf(
                BudgetItemOption(id = "budget-1", categoryId = "cat-1", label = "대관료"),
                BudgetItemOption(id = "budget-2", categoryId = "cat-2", label = "드레스"),
            ),
            selectedBudgetItemId = null,
            paymentMethod = PaymentMethod.Card,
            payer = Payer.Joint,
            vendorName = "",
            memo = "",
        )
    }
}

internal fun UserRole?.toDefaultExpensePayer(): Payer {
    return when (this) {
        UserRole.Groom -> Payer.Groom
        UserRole.Bride -> Payer.Bride
        null -> Payer.Joint
    }
}

sealed interface AddExpenseAction {
    data object OnClickClose : AddExpenseAction
    data object OnClickSave : AddExpenseAction
    data object OnClickDelete : AddExpenseAction
    data object OnClickDate : AddExpenseAction
    data class OnAmountChange(val value: String) : AddExpenseAction
    data class OnSelectCategory(val id: String) : AddExpenseAction
    data class OnSelectBudgetItem(val id: String?) : AddExpenseAction
    data class OnSelectPaymentMethod(val method: PaymentMethod) : AddExpenseAction
    data class OnSelectPayer(val payer: Payer) : AddExpenseAction
    data class OnVendorNameChange(val value: String) : AddExpenseAction
    data class OnMemoChange(val value: String) : AddExpenseAction
}

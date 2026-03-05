package kr.jm.weddingledger.feature.money.presentation

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod

data class BudgetItemOption(
    val id: String,
    val label: String,
)

data class AddExpenseUiState(
    val amount: String,
    val date: LocalDate,
    val dateText: String,
    val budgetItemOptions: List<BudgetItemOption>,
    val selectedBudgetItemId: String?,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val vendorName: String,
    val memo: String,
) {
    val isSaveEnabled: Boolean
        get() = amount.trim().isNotEmpty() && selectedBudgetItemId != null

    companion object {
        fun sample(): AddExpenseUiState = AddExpenseUiState(
            amount = "",
            date = LocalDate.parse("2023-10-25"),
            dateText = "2023.10.25 (수)",
            budgetItemOptions = listOf(
                BudgetItemOption(id = "budget-1", label = "웨딩홀 · 계약금"),
                BudgetItemOption(id = "budget-2", label = "스드메 · 드레스"),
            ),
            selectedBudgetItemId = null,
            paymentMethod = PaymentMethod.Card,
            payer = Payer.Bride,
            vendorName = "",
            memo = "",
        )
    }
}

sealed interface AddExpenseAction {
    data object OnClickClose : AddExpenseAction
    data object OnClickSave : AddExpenseAction
    data object OnClickDelete : AddExpenseAction
    data object OnClickDate : AddExpenseAction
    data class OnAmountChange(val value: String) : AddExpenseAction
    data class OnSelectBudgetItem(val id: String) : AddExpenseAction
    data class OnSelectPaymentMethod(val method: PaymentMethod) : AddExpenseAction
    data class OnSelectPayer(val payer: Payer) : AddExpenseAction
    data class OnVendorNameChange(val value: String) : AddExpenseAction
    data class OnMemoChange(val value: String) : AddExpenseAction
}

package kr.jm.weddingledger.feature.money.presentation

enum class PaymentMethod(val label: String) {
    Card("카드"),
    Cash("현금"),
}

enum class Payer(val label: String) {
    Groom("신랑"),
    Bride("신부"),
}

data class AddExpenseUiState(
    val amount: String,
    val dateText: String,
    val categoryOptions: List<String>,
    val selectedCategoryLabel: String?,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val memo: String,
) {
    val isSaveEnabled: Boolean
        get() = amount.trim().isNotEmpty() && selectedCategoryLabel != null

    companion object {
        fun sample(): AddExpenseUiState = AddExpenseUiState(
            amount = "",
            dateText = "2023.10.25 (수)",
            categoryOptions = listOf(
                "웨딩홀 (대관료, 식대)",
                "스드메 (스튜디오, 드레스)",
                "혼수 / 예물",
                "허니문",
            ),
            selectedCategoryLabel = null,
            paymentMethod = PaymentMethod.Card,
            payer = Payer.Bride,
            memo = "",
        )
    }
}

sealed interface AddExpenseAction {
    data object OnClickClose : AddExpenseAction
    data object OnClickSave : AddExpenseAction
    data object OnClickDate : AddExpenseAction
    data class OnAmountChange(val value: String) : AddExpenseAction
    data class OnSelectCategory(val label: String) : AddExpenseAction
    data class OnSelectPaymentMethod(val method: PaymentMethod) : AddExpenseAction
    data class OnSelectPayer(val payer: Payer) : AddExpenseAction
    data class OnMemoChange(val value: String) : AddExpenseAction
}


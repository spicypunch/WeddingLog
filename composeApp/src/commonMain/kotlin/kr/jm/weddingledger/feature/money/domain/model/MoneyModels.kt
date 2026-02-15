package kr.jm.weddingledger.feature.money.domain.model

import kotlinx.datetime.LocalDate

data class MoneyCategory(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

data class BudgetItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val targetAmount: Int,
    val memo: String?,
)

enum class PaymentMethod(val label: String) {
    Card("카드"),
    Cash("현금"),
}

enum class Payer(val label: String) {
    Groom("신랑"),
    Bride("신부"),
}

data class Expense(
    val id: String,
    val budgetItemId: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val memo: String?,
    val imageUrl: String?,
)

data class MoneySnapshot(
    val categories: List<MoneyCategory>,
    val budgetItems: List<BudgetItem>,
    val expenses: List<Expense>,
)

data class NewBudgetItem(
    val categoryId: String,
    val name: String,
    val targetAmount: Int,
    val memo: String?,
)

data class NewExpense(
    val budgetItemId: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val memo: String?,
)


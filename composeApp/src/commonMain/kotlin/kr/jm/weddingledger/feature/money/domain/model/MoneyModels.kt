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
    val stage: BudgetStageType,
    val dueDate: LocalDate? = null,
    val memo: String?,
)

enum class BudgetStageType(
    val label: String,
    val priority: Int,
) {
    Deposit(label = "계약금", priority = 2),
    Interim(label = "중도금", priority = 1),
    Final(label = "잔금", priority = 0),
    General(label = "기타", priority = 3),
    ;

    companion object {
        fun fromStoredValueOrNull(value: String?): BudgetStageType? {
            val normalized = value?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == normalized }
        }

        fun inferFromName(name: String): BudgetStageType {
            val normalized = name.trim().lowercase()
            return when {
                "잔금" in normalized || "balance" in normalized || "final" in normalized -> Final
                "중도금" in normalized || "중도" in normalized || "interim" in normalized -> Interim
                "계약금" in normalized || "예약금" in normalized || "deposit" in normalized -> Deposit
                else -> General
            }
        }
    }
}

enum class PaymentMethod(val label: String) {
    Card("카드"),
    Cash("현금"),
    JointAccount("공동계좌"),
}

enum class Payer(val label: String) {
    Groom("신랑"),
    Bride("신부"),
    Joint("공동"),
}

data class Expense(
    val id: String,
    val budgetItemId: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val vendorName: String?,
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
    val stage: BudgetStageType,
    val dueDate: LocalDate? = null,
    val memo: String?,
)

data class NewExpense(
    val budgetItemId: String,
    val amount: Int,
    val date: LocalDate,
    val paymentMethod: PaymentMethod,
    val payer: Payer,
    val vendorName: String?,
    val memo: String?,
)

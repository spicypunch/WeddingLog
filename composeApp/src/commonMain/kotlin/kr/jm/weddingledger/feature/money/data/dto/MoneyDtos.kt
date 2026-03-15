package kr.jm.weddingledger.feature.money.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    val name: String,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class CategoryInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    val name: String,
    @SerialName("is_default")
    val isDefault: Boolean = false,
)

@Serializable
data class BudgetItemRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("category_id")
    val categoryId: String,
    val name: String,
    @SerialName("target_amount")
    val targetAmount: Int,
    val stage: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    val memo: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class BudgetItemInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("category_id")
    val categoryId: String,
    val name: String,
    @SerialName("target_amount")
    val targetAmount: Int,
    val stage: String,
    @SerialName("due_date")
    val dueDate: String? = null,
    val memo: String? = null,
)

@Serializable
data class ExpenseRowDto(
    val id: String,
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("budget_item_id")
    val budgetItemId: String,
    val amount: Int,
    val date: String,
    @SerialName("payment_method")
    val paymentMethod: String,
    val payer: String,
    @SerialName("vendor_name")
    val vendorName: String? = null,
    val memo: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class ExpenseInsertDto(
    @SerialName("wedding_id")
    val weddingId: String,
    @SerialName("budget_item_id")
    val budgetItemId: String,
    val amount: Int,
    val date: String,
    @SerialName("payment_method")
    val paymentMethod: String,
    val payer: String,
    @SerialName("vendor_name")
    val vendorName: String? = null,
    val memo: String? = null,
)

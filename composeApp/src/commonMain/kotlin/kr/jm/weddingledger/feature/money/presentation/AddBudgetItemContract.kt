package kr.jm.weddingledger.feature.money.presentation

import kotlinx.datetime.LocalDate

data class CategoryOption(
    val id: String,
    val label: String,
)

data class AddBudgetItemUiState(
    val categoryOptions: List<CategoryOption>,
    val selectedCategoryId: String?,
    val name: String,
    val targetAmount: String,
    val dueDate: LocalDate?,
    val dueDateText: String?,
    val memo: String,
    val helperText: String?,
) {
    val selectedCategoryLabel: String?
        get() = categoryOptions.firstOrNull { it.id == selectedCategoryId }?.label

    val isSaveEnabled: Boolean
        get() = selectedCategoryId != null && name.trim().isNotEmpty() && targetAmount.trim().isNotEmpty()

    companion object {
        fun empty(): AddBudgetItemUiState = AddBudgetItemUiState(
            categoryOptions = emptyList(),
            selectedCategoryId = null,
            name = "",
            targetAmount = "",
            dueDate = null,
            dueDateText = null,
            memo = "",
            helperText = null,
        )
    }
}

sealed interface AddBudgetItemAction {
    data object OnClickClose : AddBudgetItemAction
    data object OnClickSave : AddBudgetItemAction
    data object OnClickDelete : AddBudgetItemAction
    data object OnClickDueDate : AddBudgetItemAction
    data object OnClickClearDueDate : AddBudgetItemAction

    data class OnSelectCategory(val id: String) : AddBudgetItemAction
    data class OnNameChange(val value: String) : AddBudgetItemAction
    data class OnTargetAmountChange(val value: String) : AddBudgetItemAction
    data class OnMemoChange(val value: String) : AddBudgetItemAction
}

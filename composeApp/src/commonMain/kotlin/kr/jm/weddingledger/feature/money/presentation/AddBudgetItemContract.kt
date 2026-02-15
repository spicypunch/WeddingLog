package kr.jm.weddingledger.feature.money.presentation

data class CategoryOption(
    val id: String,
    val label: String,
)

data class AddBudgetItemUiState(
    val categoryOptions: List<CategoryOption>,
    val selectedCategoryId: String?,
    val name: String,
    val targetAmount: String,
    val memo: String,
    val helperText: String?,
) {
    val selectedCategoryLabel: String?
        get() = categoryOptions.firstOrNull { it.id == selectedCategoryId }?.label

    val isSaveEnabled: Boolean
        get() = selectedCategoryId != null && name.trim().isNotEmpty() && targetAmount.trim().isNotEmpty()

    companion object {
        fun sample(): AddBudgetItemUiState = AddBudgetItemUiState(
            categoryOptions = listOf(
                CategoryOption(id = "cat-1", label = "스드메"),
                CategoryOption(id = "cat-2", label = "예식장"),
            ),
            selectedCategoryId = "cat-1",
            name = "웨딩 촬영",
            targetAmount = "2,500,000",
            memo = "",
            helperText = "예산 전체의 12%에 해당하는 금액입니다.",
        )
    }
}

sealed interface AddBudgetItemAction {
    data object OnClickClose : AddBudgetItemAction
    data object OnClickSave : AddBudgetItemAction

    data class OnSelectCategory(val id: String) : AddBudgetItemAction
    data class OnNameChange(val value: String) : AddBudgetItemAction
    data class OnTargetAmountChange(val value: String) : AddBudgetItemAction
    data class OnMemoChange(val value: String) : AddBudgetItemAction
}

package kr.jm.weddingledger.feature.money.presentation

data class AddBudgetItemUiState(
    val categoryLabels: List<String>,
    val selectedCategoryLabel: String,
    val name: String,
    val targetAmount: String,
    val memo: String,
    val helperText: String?,
) {
    val isSaveEnabled: Boolean
        get() = name.trim().isNotEmpty() && targetAmount.trim().isNotEmpty()

    companion object {
        fun sample(): AddBudgetItemUiState = AddBudgetItemUiState(
            categoryLabels = listOf("스드메", "예식장", "예물/예단", "가전/혼수", "허니문"),
            selectedCategoryLabel = "스드메",
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

    data class OnSelectCategory(val label: String) : AddBudgetItemAction
    data class OnNameChange(val value: String) : AddBudgetItemAction
    data class OnTargetAmountChange(val value: String) : AddBudgetItemAction
    data class OnMemoChange(val value: String) : AddBudgetItemAction
}


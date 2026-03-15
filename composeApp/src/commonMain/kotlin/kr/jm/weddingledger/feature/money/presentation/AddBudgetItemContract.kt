package kr.jm.weddingledger.feature.money.presentation

import kotlinx.datetime.LocalDate
import kr.jm.weddingledger.feature.money.domain.model.BudgetStageType

data class CategoryOption(
    val id: String,
    val label: String,
)

data class BudgetStageOption(
    val stage: BudgetStageType,
    val label: String,
)

data class AddBudgetItemUiState(
    val categoryOptions: List<CategoryOption>,
    val selectedCategoryId: String?,
    val stageOptions: List<BudgetStageOption>,
    val selectedStage: BudgetStageType,
    val quickNameSuggestions: List<String>,
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
        fun sample(): AddBudgetItemUiState = AddBudgetItemUiState(
            categoryOptions = listOf(
                CategoryOption(id = "cat-1", label = "스드메"),
                CategoryOption(id = "cat-2", label = "예식장"),
            ),
            selectedCategoryId = "cat-1",
            stageOptions = BudgetStageType.entries.map { BudgetStageOption(stage = it, label = it.label) },
            selectedStage = BudgetStageType.Deposit,
            quickNameSuggestions = listOf("계약금", "중도금", "잔금", "추가 비용"),
            name = "웨딩 촬영",
            targetAmount = "2,500,000",
            dueDate = LocalDate.parse("2026-04-12"),
            dueDateText = "2026.04.12 (일)",
            memo = "",
            helperText = "예산 전체의 12%에 해당하는 금액입니다.",
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
    data class OnSelectStage(val stage: BudgetStageType) : AddBudgetItemAction
    data class OnSelectQuickName(val value: String) : AddBudgetItemAction
    data class OnNameChange(val value: String) : AddBudgetItemAction
    data class OnTargetAmountChange(val value: String) : AddBudgetItemAction
    data class OnMemoChange(val value: String) : AddBudgetItemAction
}

package kr.jm.weddingledger.feature.money.presentation

data class EditableCategoryItem(
    val id: String,
    val name: String,
)

data class ManageCategoriesUiState(
    val categories: List<EditableCategoryItem>,
    val newCategoryName: String,
    val editingCategoryId: String?,
    val editingCategoryName: String,
) {
    companion object {
        fun empty(): ManageCategoriesUiState = ManageCategoriesUiState(
            categories = emptyList(),
            newCategoryName = "",
            editingCategoryId = null,
            editingCategoryName = "",
        )
    }
}

sealed interface ManageCategoriesAction {
    data object OnClickClose : ManageCategoriesAction
    data class OnNewCategoryNameChange(val value: String) : ManageCategoriesAction
    data object OnClickAddCategory : ManageCategoriesAction
    data class OnClickDeleteCategory(val id: String) : ManageCategoriesAction
    data class OnStartEditCategory(val id: String) : ManageCategoriesAction
    data class OnEditCategoryNameChange(val value: String) : ManageCategoriesAction
    data object OnClickSaveCategoryEdit : ManageCategoriesAction
    data object OnClickCancelCategoryEdit : ManageCategoriesAction
}

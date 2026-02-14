package kr.jm.weddingledger.feature.schedule.presentation

data class AddChecklistUiState(
    val title: String,
    val targetDateText: String,
    val dTagOptions: List<String>,
    val selectedDTag: String,
    val isCompleted: Boolean,
    val memo: String,
) {
    val isSaveEnabled: Boolean
        get() = title.trim().isNotEmpty()

    companion object {
        fun sample(): AddChecklistUiState = AddChecklistUiState(
            title = "",
            targetDateText = "2023. 10. 21 (토)",
            dTagOptions = listOf("D-180", "D-100", "D-60", "D-30", "D-7"),
            selectedDTag = "D-180",
            isCompleted = false,
            memo = "",
        )
    }
}

sealed interface AddChecklistAction {
    data object OnClickClose : AddChecklistAction
    data object OnClickSave : AddChecklistAction
    data object OnClickTargetDate : AddChecklistAction
    data class OnTitleChange(val value: String) : AddChecklistAction
    data class OnSelectDTag(val value: String) : AddChecklistAction
    data class OnToggleCompleted(val value: Boolean) : AddChecklistAction
    data class OnMemoChange(val value: String) : AddChecklistAction
}


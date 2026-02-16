package kr.jm.weddingledger.feature.money.domain.model

data class ExpenseFilterPreferenceState(
    val selectedCategoryId: String? = null,
    val selectedPeriodFilterName: String,
    val selectedStatusFilterName: String,
    val selectedPresetId: String? = null,
    val presetSeed: Int = 1,
    val overBudgetOnly: Boolean = false,
    val presets: List<ExpenseFilterPresetPreference> = emptyList(),
)

data class ExpenseFilterPresetPreference(
    val id: String,
    val label: String,
    val categoryId: String?,
    val periodFilterName: String,
    val statusFilterName: String,
)

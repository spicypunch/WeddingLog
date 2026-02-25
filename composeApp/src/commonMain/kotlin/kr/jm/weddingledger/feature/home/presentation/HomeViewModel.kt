package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.time.Clock
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase

class HomeViewModel(
    private val fetchMoneySnapshotUseCase: FetchMoneySnapshotUseCase,
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState.empty())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefresh -> refresh(isRefreshing = true)
            HomeAction.OnClickNotifications,
            HomeAction.OnClickAddExpense,
            HomeAction.OnClickViewAllCategories,
            is HomeAction.OnClickCategory,
            is HomeAction.OnClickRecentExpense,
            -> Unit
        }
    }

    private fun refresh(isRefreshing: Boolean = false) {
        if (isRefreshing) {
            _state.update { it.copy(isRefreshing = true) }
        }

        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val dDayTitle = resolveDdayTitle(today)

            val nextState = when (val money = fetchMoneySnapshotUseCase()) {
                is kr.jm.weddingledger.feature.money.domain.model.MoneyResult.Success ->
                    money.data.toHomeUiState(dDayTitle)

                is kr.jm.weddingledger.feature.money.domain.model.MoneyResult.Error ->
                    HomeUiState.empty(dDayTitle = dDayTitle)
            }

            _state.value = nextState.copy(isRefreshing = false)
        }
    }

    private suspend fun resolveDdayTitle(today: LocalDate): String {
        val profile = when (val result = fetchMyProfileUseCase()) {
            is OnboardingResult.Success -> result.data
            is OnboardingResult.Error -> return "우리 결혼 준비 중"
        }

        return profile.toDdayTitle(today)
    }
}

private fun MyProfile.toDdayTitle(today: LocalDate): String {
    val weddingDate = weddingDate
    if (isWeddingDateUndecided || weddingDate == null) return "우리 결혼 준비 중"

    val diff = today.daysUntil(weddingDate)
    return when {
        diff > 0 -> "우리 결혼 D-$diff"
        diff == 0 -> "우리 결혼 D-DAY"
        else -> "우리 결혼 D+${diff.absoluteValue}"
    }
}

private fun MoneySnapshot.toHomeUiState(dDayTitle: String): HomeUiState {
    val budgetItemsById = budgetItems.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }

    val totalBudget = budgetItems.sumOf { it.targetAmount }
    val totalSpent = expenses.sumOf { it.amount }
    val remaining = totalBudget - totalSpent

    val progress = if (totalBudget <= 0) 0f else (totalSpent.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f)
    val remainingPercent = if (totalBudget <= 0) 0 else floor((remaining.toDouble() / totalBudget.toDouble()) * 100.0).toInt().coerceIn(0, 100)

    val spentByCategoryId = categories.associate { category ->
        val spent = budgetItems
            .asSequence()
            .filter { it.categoryId == category.id }
            .sumOf { item ->
                expenses
                    .asSequence()
                    .filter { it.budgetItemId == item.id }
                    .sumOf { it.amount }
            }
        category.id to spent
    }

    val categoriesUi = categories
        .sortedWith(
            compareByDescending<kr.jm.weddingledger.feature.money.domain.model.MoneyCategory> { spentByCategoryId[it.id] ?: 0 }
                .thenBy { it.name }
        )
        .take(6)
        .map { category ->
            val visuals = category.name.toCategoryVisuals()
            HomeCategorySummary(
                name = category.name,
                amountKrw = formatKrw(spentByCategoryId[category.id] ?: 0),
                icon = visuals.icon,
                accent = visuals.accent,
                accentContainer = visuals.accentContainer,
            )
        }

    val recentExpenses = expenses
        .sortedWith(
            compareByDescending<kr.jm.weddingledger.feature.money.domain.model.Expense> { it.date }
                .thenByDescending { it.amount }
        )
        .take(6)
        .map { expense ->
            val budgetItem = budgetItemsById[expense.budgetItemId]
            val category = budgetItem?.let { categoriesById[it.categoryId] }
            val visuals = category?.name.toCategoryVisuals()

            HomeRecentExpense(
                title = budgetItem?.name ?: "지출",
                dateText = expense.date.toDotDate(),
                amountText = "- ${formatKrw(expense.amount)}",
                icon = visuals.icon,
            )
        }

    return HomeUiState(
        appName = "WeddingLog",
        dDayTitle = dDayTitle,
        remainingBudgetKrw = formatKrw(remaining),
        remainingPercentText = "${remainingPercent}% 남음",
        spentKrw = "지출 ${formatKrw(totalSpent)}",
        totalBudgetKrw = "총 예산 ${formatKrw(totalBudget)}",
        progressFraction = progress,
        categories = categoriesUi,
        recentExpenses = recentExpenses,
    )
}

private data class CategoryVisuals(
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
)

private fun String?.toCategoryVisuals(): CategoryVisuals {
    val name = this?.trim().orEmpty()
    return when {
        name.contains("웨딩홀") || name.contains("예식장") -> CategoryVisuals(
            icon = Icons.Rounded.Church,
            accent = Color(0xFFEF3948),
            accentContainer = Color(0xFFFDE9EB),
        )
        name.contains("스드메") || name.contains("드레스") -> CategoryVisuals(
            icon = Icons.Rounded.Checkroom,
            accent = Color(0xFFF97316),
            accentContainer = Color(0xFFFFEDD5),
        )
        name.contains("예물") || name.contains("예단") -> CategoryVisuals(
            icon = Icons.Rounded.Diamond,
            accent = Color(0xFF3B82F6),
            accentContainer = Color(0xFFDBEAFE),
        )
        name.contains("신혼여행") || name.contains("허니문") -> CategoryVisuals(
            icon = Icons.Rounded.Flight,
            accent = Color(0xFF10B981),
            accentContainer = Color(0xFFD1FAE5),
        )
        name.contains("식대") || name.contains("모임") -> CategoryVisuals(
            icon = Icons.Rounded.Restaurant,
            accent = Color(0xFFA855F7),
            accentContainer = Color(0xFFF3E8FF),
        )
        name.contains("촬영") -> CategoryVisuals(
            icon = Icons.Rounded.CameraAlt,
            accent = Color(0xFFEF3948),
            accentContainer = Color(0xFFFDE9EB),
        )
        else -> CategoryVisuals(
            icon = Icons.Rounded.AttachMoney,
            accent = Color(0xFF64748B),
            accentContainer = Color(0xFFF1F5F9),
        )
    }
}

private fun LocalDate.toDotDate(): String {
    val mm = month.number.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "${year}.$mm.$dd"
}

private fun formatKrw(amount: Int): String {
    val sign = if (amount < 0) "-" else ""
    return "${sign}₩${formatNumber(amount.toLong().absoluteValue)}"
}

private fun formatNumber(value: Long): String = value
    .toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

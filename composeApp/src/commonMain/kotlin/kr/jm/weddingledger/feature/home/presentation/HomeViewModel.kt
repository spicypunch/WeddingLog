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
import kotlinx.coroutines.async
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
import kr.jm.weddingledger.core.network.logSupabaseInfo
import kr.jm.weddingledger.core.network.logSupabaseWarning
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.usecase.FetchMoneySnapshotUseCase
import kr.jm.weddingledger.feature.onboarding.domain.model.MyProfile
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingResult
import kr.jm.weddingledger.feature.onboarding.domain.usecase.FetchMyProfileUseCase
import kr.jm.weddingledger.feature.schedule.domain.model.ChecklistTask
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleEvent
import kr.jm.weddingledger.feature.schedule.domain.model.ScheduleResult
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchChecklistUseCase
import kr.jm.weddingledger.feature.schedule.domain.usecase.FetchScheduleEventsUseCase

class HomeViewModel(
    private val fetchMoneySnapshotUseCase: FetchMoneySnapshotUseCase,
    private val fetchMyProfileUseCase: FetchMyProfileUseCase,
    private val fetchScheduleEventsUseCase: FetchScheduleEventsUseCase,
    private val fetchChecklistUseCase: FetchChecklistUseCase,
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
            HomeAction.OnClickOpenMoney,
            HomeAction.OnClickOpenSchedule,
            is HomeAction.OnClickCategory,
            is HomeAction.OnClickRecentExpense,
            is HomeAction.OnClickUpcoming,
            -> Unit
        }
    }

    private fun refresh(isRefreshing: Boolean = false) {
        if (isRefreshing) {
            _state.update { it.copy(isRefreshing = true) }
        }

        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            val profileDeferred = async { fetchMyProfileUseCase() }
            val moneyDeferred = async { fetchMoneySnapshotUseCase() }
            val scheduleDeferred = async { fetchScheduleEventsUseCase() }
            val checklistDeferred = async { fetchChecklistUseCase() }

            val dDayTitle = resolveDdayTitle(profileDeferred.await(), today)
            val moneySnapshot = when (val result = moneyDeferred.await()) {
                is kr.jm.weddingledger.feature.money.domain.model.MoneyResult.Success -> result.data
                is kr.jm.weddingledger.feature.money.domain.model.MoneyResult.Error -> null
            }
            val scheduleEvents = when (val result = scheduleDeferred.await()) {
                is ScheduleResult.Success -> result.data
                is ScheduleResult.Error -> emptyList()
            }
            val checklistTasks = when (val result = checklistDeferred.await()) {
                is ScheduleResult.Success -> result.data
                is ScheduleResult.Error -> emptyList()
            }

            _state.value = buildHomeUiState(
                dDayTitle = dDayTitle,
                moneySnapshot = moneySnapshot,
                scheduleEvents = scheduleEvents,
                checklistTasks = checklistTasks,
                today = today,
            ).copy(isRefreshing = false)
        }
    }
}

private fun resolveDdayTitle(
    profileResult: OnboardingResult<MyProfile>,
    today: LocalDate,
): String {
    val profile = when (profileResult) {
        is OnboardingResult.Success -> profileResult.data
        is OnboardingResult.Error -> {
            logSupabaseWarning(
                featureName = HOME_LOG_FEATURE_NAME,
                message = "dday_profile_fetch_error=${profileResult.message}",
            )
            return "우리 결혼 준비 중"
        }
    }

    logSupabaseInfo(
        featureName = HOME_LOG_FEATURE_NAME,
        message = "dday weddingId=${profile.weddingId} weddingDate=${profile.weddingDate} " +
            "undecided=${profile.isWeddingDateUndecided}",
    )

    return profile.toDdayTitle(today)
}

private fun buildHomeUiState(
    dDayTitle: String,
    moneySnapshot: MoneySnapshot?,
    scheduleEvents: List<ScheduleEvent>,
    checklistTasks: List<ChecklistTask>,
    today: LocalDate,
): HomeUiState {
    val overview = moneySnapshot?.toOverviewCard() ?: HomeOverviewCard.empty()
    val payers = moneySnapshot?.toPayerSummaries() ?: HomePayerSummary.defaults()
    val categories = moneySnapshot?.toCategorySettlements() ?: emptyList()
    val upcoming = buildUpcomingItems(
        scheduleEvents = scheduleEvents,
        checklistTasks = checklistTasks,
        today = today,
    )
    val recentExpenses = moneySnapshot?.toRecentExpenses() ?: emptyList()

    return HomeUiState(
        appName = "WeddingLog",
        dDayTitle = dDayTitle,
        summaryLine = buildSummaryLine(
            usedPercentText = overview.usedPercentText,
            upcomingCount = upcoming.size,
        ),
        overview = overview,
        payers = payers,
        categories = categories,
        upcoming = upcoming,
        recentExpenses = recentExpenses,
    )
}

private fun buildSummaryLine(
    usedPercentText: String,
    upcomingCount: Int,
): String {
    return when {
        usedPercentText == "0% 사용" && upcomingCount == 0 -> "예산과 일정을 한눈에 확인해보세요."
        upcomingCount == 0 -> "예산 $usedPercentText · 예정된 일정 없음"
        else -> "예산 $usedPercentText · 다가오는 일정 ${upcomingCount}건"
    }
}

private fun MyProfile.toDdayTitle(today: LocalDate): String {
    val resolvedWeddingDate = weddingDate
    if (isWeddingDateUndecided || resolvedWeddingDate == null) return "우리 결혼 준비 중"

    val diff = today.daysUntil(resolvedWeddingDate)
    return when {
        diff > 0 -> "우리 결혼 D-$diff"
        diff == 0 -> "우리 결혼 D-DAY"
        else -> "우리 결혼 D+${diff.absoluteValue}"
    }
}

private fun MoneySnapshot.toOverviewCard(): HomeOverviewCard {
    val totalBudget = budgetItems.sumOf { it.targetAmount }
    val totalSpent = expenses.sumOf { it.amount }
    val remaining = totalBudget - totalSpent
    val usedPercent = if (totalBudget <= 0) {
        0
    } else {
        floor((totalSpent.toDouble() / totalBudget.toDouble()) * 100.0).toInt().coerceAtLeast(0)
    }
    val progress = if (totalBudget <= 0) 0f else (totalSpent.toFloat() / totalBudget.toFloat()).coerceIn(0f, 1f)

    return HomeOverviewCard(
        totalBudgetText = "총 예산 ${formatKrw(totalBudget)}",
        totalSpentText = "총 지출 ${formatKrw(totalSpent)}",
        remainingBudgetText = formatKrw(remaining),
        usedPercentText = "${usedPercent}% 사용",
        progressFraction = progress,
    )
}

private fun MoneySnapshot.toPayerSummaries(): List<HomePayerSummary> {
    val totalSpent = expenses.sumOf { it.amount }
    val amountByPayer = expenses
        .groupBy { it.payer }
        .mapValues { (_, value) -> value.sumOf { it.amount } }

    return listOf(
        payerSummary(
            label = "신랑",
            amount = amountByPayer[Payer.Groom] ?: 0,
            totalSpent = totalSpent,
            accent = Color(0xFF3B82F6),
            accentContainer = Color(0xFFDBEAFE),
        ),
        payerSummary(
            label = "신부",
            amount = amountByPayer[Payer.Bride] ?: 0,
            totalSpent = totalSpent,
            accent = Color(0xFFEF3948),
            accentContainer = Color(0xFFFDE9EB),
        ),
        payerSummary(
            label = "공동",
            amount = amountByPayer[Payer.Joint] ?: 0,
            totalSpent = totalSpent,
            accent = Color(0xFFF97316),
            accentContainer = Color(0xFFFFEDD5),
        ),
    )
}

private fun payerSummary(
    label: String,
    amount: Int,
    totalSpent: Int,
    accent: Color,
    accentContainer: Color,
): HomePayerSummary {
    val ratio = if (totalSpent <= 0) 0 else floor((amount.toDouble() / totalSpent.toDouble()) * 100.0).toInt()
    val progress = if (totalSpent <= 0) 0f else (amount.toFloat() / totalSpent.toFloat()).coerceIn(0f, 1f)
    return HomePayerSummary(
        label = label,
        amountText = formatKrw(amount),
        shareText = "${ratio}%",
        progressFraction = progress,
        accent = accent,
        accentContainer = accentContainer,
    )
}

private fun MoneySnapshot.toCategorySettlements(): List<HomeCategorySettlement> {
    val budgetItemsByCategory = budgetItems.groupBy { it.categoryId }
    val spentByCategoryId = expenses.groupBy { it.categoryId }.mapValues { (_, value) -> value.sumOf { it.amount } }

    return categories
        .mapNotNull { category ->
            val categoryBudgetItems = budgetItemsByCategory[category.id].orEmpty()
            val targetAmount = categoryBudgetItems.sumOf { it.targetAmount }
            val spentAmount = spentByCategoryId[category.id] ?: 0
            if (targetAmount <= 0 && spentAmount <= 0) return@mapNotNull null

            val remainingAmount = targetAmount - spentAmount
            val visuals = category.name.toCategoryVisuals()
            HomeCategorySettlement(
                name = category.name,
                targetAmountText = "예산 ${formatKrw(targetAmount)}",
                spentAmountText = "지출 ${formatKrw(spentAmount)}",
                remainingAmountText = if (remainingAmount >= 0) {
                    "${formatKrw(remainingAmount)} 남음"
                } else {
                    "${formatKrw(remainingAmount.absoluteValue)} 초과"
                },
                progressFraction = if (targetAmount <= 0) 0f else (spentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f),
                icon = visuals.icon,
                accent = visuals.accent,
                accentContainer = visuals.accentContainer,
                isOverBudget = remainingAmount < 0,
            )
        }
        .sortedWith(compareByDescending<HomeCategorySettlement> { extractNumericAmount(it.spentAmountText) }.thenBy { it.name })
        .take(5)
}

private fun MoneySnapshot.toRecentExpenses(): List<HomeRecentExpense> {
    val budgetItemsById = budgetItems.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }

    return expenses
        .sortedWith(compareByDescending<kr.jm.weddingledger.feature.money.domain.model.Expense> { it.date }.thenByDescending { it.amount })
        .take(5)
        .map { expense ->
            val budgetItem = expense.budgetItemId?.let { budgetItemsById[it] }
            val category = categoriesById[expense.categoryId]
            val visuals = category?.name.toCategoryVisuals()
            val noteParts = buildList {
                category?.name?.takeIf { it.isNotBlank() }?.let(::add)
                add(expense.payer.label)
            }

            HomeRecentExpense(
                id = expense.id,
                title = budgetItem?.name ?: expense.vendorName ?: category?.name?.let { "$it 지출" } ?: "지출",
                dateText = expense.date.toDotDate(),
                amountText = "- ${formatKrw(expense.amount)}",
                noteText = noteParts.joinToString(" · "),
                icon = visuals.icon,
                accent = visuals.accent,
                accentContainer = visuals.accentContainer,
            )
        }
}

private fun buildUpcomingItems(
    scheduleEvents: List<ScheduleEvent>,
    checklistTasks: List<ChecklistTask>,
    today: LocalDate,
): List<HomeUpcomingItem> {
    val eventItems = scheduleEvents
        .filter { it.eventDate >= today }
        .map { event ->
            val subtitle = listOfNotNull(event.location, event.startTime).joinToString(" · ").ifBlank { "예정된 일정" }
            HomeUpcomingCandidate(
                date = event.eventDate,
                item = HomeUpcomingItem(
                    id = event.id,
                    title = event.title,
                    subtitle = subtitle,
                    dateText = event.eventDate.toDotDate(),
                    dDayText = event.eventDate.toCountdownText(today),
                    typeLabel = "일정",
                    dDayTone = HomeBadgeTone.Primary,
                )
            )
        }

    val checklistItems = checklistTasks
        .filter { !it.isChecked && it.targetDate != null }
        .map { task ->
            val targetDate = task.targetDate ?: return@map null
            val tone = if (targetDate < today) HomeBadgeTone.Error else HomeBadgeTone.Neutral
            HomeUpcomingCandidate(
                date = targetDate,
                item = HomeUpcomingItem(
                    id = task.id,
                    title = task.title,
                    subtitle = listOfNotNull("체크리스트", task.groupTitle).joinToString(" · "),
                    dateText = targetDate.toDotDate(),
                    dDayText = targetDate.toCountdownText(today),
                    typeLabel = "체크리스트",
                    dDayTone = tone,
                )
            )
        }
        .filterNotNull()

    return (eventItems + checklistItems)
        .sortedWith(
            compareBy<HomeUpcomingCandidate> { candidate ->
                if (candidate.date < today) 0 else 1
            }.thenBy { it.date }
        )
        .take(4)
        .map { it.item }
}

private data class HomeUpcomingCandidate(
    val date: LocalDate,
    val item: HomeUpcomingItem,
)

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
        name.contains("식대") || name.contains("모임") || name.contains("하객") -> CategoryVisuals(
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

private fun LocalDate.toCountdownText(today: LocalDate): String {
    val diff = today.daysUntil(this)
    return when {
        diff > 0 -> "D-$diff"
        diff == 0 -> "오늘"
        else -> "D+${diff.absoluteValue}"
    }
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

private fun extractNumericAmount(text: String): Int = text
    .filter { it.isDigit() }
    .toIntOrNull()
    ?: 0

private const val HOME_LOG_FEATURE_NAME: String = "Home"

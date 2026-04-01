package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeUiState(
    val appName: String,
    val dDayTitle: String,
    val summaryLine: String,
    val overview: HomeOverviewCard,
    val payers: List<HomePayerSummary>,
    val categories: List<HomeCategorySettlement>,
    val upcoming: List<HomeUpcomingItem>,
    val recentExpenses: List<HomeRecentExpense>,
    val isRefreshing: Boolean = false,
) {
    companion object {
        fun empty(dDayTitle: String = "우리 결혼 준비 중"): HomeUiState = HomeUiState(
            appName = "WeddingLog",
            dDayTitle = dDayTitle,
            summaryLine = "예산과 일정을 한눈에 확인해보세요.",
            overview = HomeOverviewCard.empty(),
            payers = HomePayerSummary.defaults(),
            categories = emptyList(),
            upcoming = emptyList(),
            recentExpenses = emptyList(),
        )
    }
}

data class HomeOverviewCard(
    val totalBudgetText: String,
    val totalSpentText: String,
    val remainingBudgetText: String,
    val usedPercentText: String,
    val progressFraction: Float,
) {
    companion object {
        fun empty(): HomeOverviewCard = HomeOverviewCard(
            totalBudgetText = "총 예산 ₩0",
            totalSpentText = "총 지출 ₩0",
            remainingBudgetText = "₩0",
            usedPercentText = "0% 사용",
            progressFraction = 0f,
        )
    }
}

data class HomePayerSummary(
    val label: String,
    val amountText: String,
    val shareText: String,
    val progressFraction: Float,
    val accent: Color,
    val accentContainer: Color,
) {
    companion object {
        fun defaults(): List<HomePayerSummary> = listOf(
            HomePayerSummary(
                label = "신랑",
                amountText = "₩0",
                shareText = "0%",
                progressFraction = 0f,
                accent = Color(0xFF3B82F6),
                accentContainer = Color(0xFFDBEAFE),
            ),
            HomePayerSummary(
                label = "신부",
                amountText = "₩0",
                shareText = "0%",
                progressFraction = 0f,
                accent = Color(0xFFEF3948),
                accentContainer = Color(0xFFFDE9EB),
            ),
            HomePayerSummary(
                label = "공동",
                amountText = "₩0",
                shareText = "0%",
                progressFraction = 0f,
                accent = Color(0xFFF97316),
                accentContainer = Color(0xFFFFEDD5),
            ),
        )
    }
}

data class HomeCategorySettlement(
    val name: String,
    val targetAmountText: String,
    val spentAmountText: String,
    val remainingAmountText: String,
    val progressFraction: Float,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
    val isOverBudget: Boolean = false,
)

enum class HomeBadgeTone {
    Primary,
    Neutral,
    Error,
}

data class HomeUpcomingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateText: String,
    val dDayText: String,
    val typeLabel: String,
    val dDayTone: HomeBadgeTone,
)

data class HomeRecentExpense(
    val id: String,
    val title: String,
    val dateText: String,
    val amountText: String,
    val noteText: String,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
)

sealed interface HomeAction {
    data object OnRefresh : HomeAction
    data object OnClickNotifications : HomeAction
    data object OnClickOpenMoney : HomeAction
    data object OnClickOpenSchedule : HomeAction
    data class OnClickCategory(val name: String) : HomeAction
    data class OnClickRecentExpense(val id: String) : HomeAction
    data class OnClickUpcoming(val id: String) : HomeAction
}

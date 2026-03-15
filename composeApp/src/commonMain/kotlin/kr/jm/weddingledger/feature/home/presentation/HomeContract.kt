package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Restaurant
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

        fun sample(): HomeUiState = HomeUiState(
            appName = "WeddingLog",
            dDayTitle = "우리 결혼 D-150",
            summaryLine = "예산 32% 사용 · 다가오는 일정 3건",
            overview = HomeOverviewCard(
                totalBudgetText = "총 예산 ₩50,000,000",
                totalSpentText = "총 지출 ₩15,800,000",
                remainingBudgetText = "₩34,200,000",
                usedPercentText = "32% 사용",
                progressFraction = 0.32f,
            ),
            payers = listOf(
                HomePayerSummary(
                    label = "신랑",
                    amountText = "₩6,200,000",
                    shareText = "39%",
                    progressFraction = 0.39f,
                    accent = Color(0xFF3B82F6),
                    accentContainer = Color(0xFFDBEAFE),
                ),
                HomePayerSummary(
                    label = "신부",
                    amountText = "₩7,100,000",
                    shareText = "45%",
                    progressFraction = 0.45f,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                ),
                HomePayerSummary(
                    label = "공동",
                    amountText = "₩2,500,000",
                    shareText = "16%",
                    progressFraction = 0.16f,
                    accent = Color(0xFFF97316),
                    accentContainer = Color(0xFFFFEDD5),
                ),
            ),
            categories = listOf(
                HomeCategorySettlement(
                    name = "웨딩홀",
                    targetAmountText = "예산 ₩5,000,000",
                    spentAmountText = "지출 ₩4,000,000",
                    remainingAmountText = "₩1,000,000 남음",
                    progressFraction = 0.80f,
                    icon = Icons.Rounded.Church,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                ),
                HomeCategorySettlement(
                    name = "예물",
                    targetAmountText = "예산 ₩3,500,000",
                    spentAmountText = "지출 ₩2,200,000",
                    remainingAmountText = "₩1,300,000 남음",
                    progressFraction = 0.63f,
                    icon = Icons.Rounded.Diamond,
                    accent = Color(0xFF3B82F6),
                    accentContainer = Color(0xFFDBEAFE),
                ),
                HomeCategorySettlement(
                    name = "신혼여행",
                    targetAmountText = "예산 ₩4,500,000",
                    spentAmountText = "지출 ₩1,200,000",
                    remainingAmountText = "₩3,300,000 남음",
                    progressFraction = 0.27f,
                    icon = Icons.Rounded.Flight,
                    accent = Color(0xFF10B981),
                    accentContainer = Color(0xFFD1FAE5),
                ),
            ),
            upcoming = listOf(
                HomeUpcomingItem(
                    id = "event-1",
                    title = "스튜디오 촬영",
                    subtitle = "강남구 루브르 스튜디오 · 14:00",
                    dateText = "2026.08.14",
                    dDayText = "D-10",
                    typeLabel = "일정",
                    dDayTone = HomeBadgeTone.Primary,
                ),
                HomeUpcomingItem(
                    id = "check-1",
                    title = "청첩장 주문",
                    subtitle = "체크리스트 · D-30 준비",
                    dateText = "2026.08.20",
                    dDayText = "D-4",
                    typeLabel = "체크리스트",
                    dDayTone = HomeBadgeTone.Neutral,
                ),
            ),
            recentExpenses = listOf(
                HomeRecentExpense(
                    id = "exp-1",
                    title = "웨딩홀 계약금",
                    dateText = "2026.08.01",
                    amountText = "- ₩1,000,000",
                    noteText = "웨딩홀 · 신랑",
                    icon = Icons.Rounded.Church,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                ),
                HomeRecentExpense(
                    id = "exp-2",
                    title = "청첩장 인쇄",
                    dateText = "2026.07.28",
                    amountText = "- ₩250,000",
                    noteText = "하객 · 신부",
                    icon = Icons.Rounded.Restaurant,
                    accent = Color(0xFFF97316),
                    accentContainer = Color(0xFFFFEDD5),
                ),
            ),
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

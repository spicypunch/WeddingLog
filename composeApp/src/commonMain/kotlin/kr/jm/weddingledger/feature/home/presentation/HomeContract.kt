package kr.jm.weddingledger.feature.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeUiState(
    val appName: String,
    val dDayTitle: String,
    val remainingBudgetKrw: String,
    val remainingPercentText: String,
    val spentKrw: String,
    val totalBudgetKrw: String,
    val progressFraction: Float,
    val categories: List<HomeCategorySummary>,
    val recentExpenses: List<HomeRecentExpense>,
) {
    companion object {
        fun sample(): HomeUiState = HomeUiState(
            appName = "WeddingLog",
            dDayTitle = "우리 결혼 D-150",
            remainingBudgetKrw = "₩34,200,000",
            remainingPercentText = "68% 남음",
            spentKrw = "지출 ₩15,800,000",
            totalBudgetKrw = "총 예산 ₩50,000,000",
            progressFraction = 0.32f,
            categories = listOf(
                HomeCategorySummary(
                    name = "웨딩홀",
                    amountKrw = "₩5,000,000",
                    icon = Icons.Rounded.Church,
                    accent = Color(0xFFEF3948),
                    accentContainer = Color(0xFFFDE9EB),
                ),
                HomeCategorySummary(
                    name = "스드메",
                    amountKrw = "₩3,500,000",
                    icon = Icons.Rounded.Checkroom,
                    accent = Color(0xFFF97316),
                    accentContainer = Color(0xFFFFEDD5),
                ),
                HomeCategorySummary(
                    name = "예물",
                    amountKrw = "₩2,200,000",
                    icon = Icons.Rounded.Diamond,
                    accent = Color(0xFF3B82F6),
                    accentContainer = Color(0xFFDBEAFE),
                ),
                HomeCategorySummary(
                    name = "신혼여행",
                    amountKrw = "₩4,500,000",
                    icon = Icons.Rounded.Flight,
                    accent = Color(0xFF10B981),
                    accentContainer = Color(0xFFD1FAE5),
                ),
            ),
            recentExpenses = listOf(
                HomeRecentExpense(
                    title = "부케 예약금",
                    dateText = "2023.10.15",
                    amountText = "- ₩300,000",
                    icon = Icons.Rounded.LocalFlorist
                ),
                HomeRecentExpense(
                    title = "청첩장 모임",
                    dateText = "2023.10.12",
                    amountText = "- ₩125,000",
                    icon = Icons.Rounded.Restaurant
                ),
                HomeRecentExpense(
                    title = "스튜디오 잔금",
                    dateText = "2023.10.10",
                    amountText = "- ₩1,500,000",
                    icon = Icons.Rounded.CameraAlt
                ),
            )
        )
    }
}

data class HomeCategorySummary(
    val name: String,
    val amountKrw: String,
    val icon: ImageVector,
    val accent: Color,
    val accentContainer: Color,
)

data class HomeRecentExpense(
    val title: String,
    val dateText: String,
    val amountText: String,
    val icon: ImageVector,
)

sealed interface HomeAction {
    data object OnClickNotifications : HomeAction
    data object OnClickAddExpense : HomeAction
    data object OnClickViewAllCategories : HomeAction
    data class OnClickCategory(val name: String) : HomeAction
    data class OnClickRecentExpense(val title: String) : HomeAction
}


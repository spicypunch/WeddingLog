package kr.jm.weddingledger.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home(route = "home", label = "홈", icon = Icons.Rounded.Home),
    Money(route = "money", label = "돈", icon = Icons.Rounded.AttachMoney),
    Schedule(route = "schedule", label = "일정", icon = Icons.Rounded.CalendarToday),
    Guests(route = "guests", label = "하객", icon = Icons.Rounded.People),
    Tools(route = "tools", label = "도구", icon = Icons.Rounded.GridView),
}


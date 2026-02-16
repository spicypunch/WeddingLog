package kr.jm.weddingledger.core.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit
) {
    val destinations = TopLevelDestination.entries

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinations.forEach { destination ->
            val selected = destination.isSelected(currentRoute)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { AppNavIcon(icon = destination.icon) },
                label = {
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

private fun TopLevelDestination.isSelected(currentRoute: String?): Boolean {
    // Nested screens should still highlight their parent tab where appropriate.
    return when (this) {
        TopLevelDestination.Tools -> currentRoute == route ||
            currentRoute == AppRoutes.VenueComparison ||
            currentRoute == AppRoutes.ValuesSurvey
        else -> currentRoute == route
    }
}

@Composable
private fun AppNavIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null
    )
}

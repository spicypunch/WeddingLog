package kr.jm.weddingledger.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.feature.guests.presentation.GuestsRoute
import kr.jm.weddingledger.feature.home.presentation.HomeRoute
import kr.jm.weddingledger.feature.money.presentation.MoneyRoute
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleRoute
import kr.jm.weddingledger.feature.settings.presentation.SettingsRoute
import kr.jm.weddingledger.feature.tools.presentation.ToolsRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route
    ) {
        composable(TopLevelDestination.Home.route) {
            HomeRoute()
        }
        composable(TopLevelDestination.Money.route) {
            MoneyRoute()
        }
        composable(TopLevelDestination.Schedule.route) {
            ScheduleRoute()
        }
        composable(TopLevelDestination.Guests.route) {
            GuestsRoute()
        }
        composable(TopLevelDestination.Tools.route) {
            ToolsRoute(
                onOpenSettings = { navController.navigate(AppRoutes.Settings) }
            )
        }
        composable(AppRoutes.Settings) {
            SettingsRoute(
                themeStyle = themeStyle,
                onThemeStyleChange = onThemeStyleChange,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

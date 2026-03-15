package kr.jm.weddingledger.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.feature.guests.presentation.GuestsRoute
import kr.jm.weddingledger.feature.home.presentation.HomeRoute
import kr.jm.weddingledger.feature.money.presentation.MoneyRoute
import kr.jm.weddingledger.feature.notifications.presentation.NotificationCenterRoute
import kr.jm.weddingledger.feature.schedule.presentation.ScheduleRoute
import kr.jm.weddingledger.feature.settings.presentation.AccountSettingsRoute
import kr.jm.weddingledger.feature.settings.presentation.AppInfoRoute
import kr.jm.weddingledger.feature.settings.presentation.ProfileEditRoute
import kr.jm.weddingledger.feature.settings.presentation.SettingsRoute
import kr.jm.weddingledger.feature.tools.presentation.ToolsRoute
import kr.jm.weddingledger.feature.tools.presentation.ValuesSurveyRoute
import kr.jm.weddingledger.feature.tools.presentation.VenueComparisonRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onLogout: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route
    ) {
        composable(TopLevelDestination.Home.route) {
            HomeRoute(
                onOpenMoney = {
                    navController.navigate(TopLevelDestination.Money.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                onOpenSchedule = {
                    navController.navigate(TopLevelDestination.Schedule.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                onOpenNotifications = { navController.navigate(AppRoutes.NotificationCenter) },
            )
        }
        composable(TopLevelDestination.Money.route) {
            MoneyRoute(
                onOpenNotifications = { navController.navigate(AppRoutes.NotificationCenter) },
                onOpenSettings = { navController.navigate(AppRoutes.Settings) },
            )
        }
        composable(TopLevelDestination.Schedule.route) {
            ScheduleRoute(
                onOpenNotifications = { navController.navigate(AppRoutes.NotificationCenter) },
            )
        }
        composable(TopLevelDestination.Guests.route) {
            GuestsRoute(
                onOpenNotifications = { navController.navigate(AppRoutes.NotificationCenter) },
            )
        }
        composable(TopLevelDestination.Tools.route) {
            ToolsRoute(
                onOpenVenueComparison = { navController.navigate(AppRoutes.VenueComparison) },
                onOpenValuesSurvey = { navController.navigate(AppRoutes.ValuesSurvey) },
                onOpenSettings = { navController.navigate(AppRoutes.Settings) }
            )
        }
        composable(AppRoutes.VenueComparison) {
            VenueComparisonRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.ValuesSurvey) {
            ValuesSurveyRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.NotificationCenter) {
            NotificationCenterRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.Settings) {
            SettingsRoute(
                themeStyle = themeStyle,
                onThemeStyleChange = onThemeStyleChange,
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
                onOpenNotificationCenter = { navController.navigate(AppRoutes.NotificationCenter) },
                onOpenProfileEdit = { navController.navigate(AppRoutes.ProfileEdit) },
                onOpenAccountSettings = { email ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(AccountEmailArgKey, email)
                    navController.navigate(AppRoutes.AccountSettings)
                },
                onOpenAppInfo = { navController.navigate(AppRoutes.AppInfo) },
            )
        }
        composable(AppRoutes.ProfileEdit) {
            ProfileEditRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.AccountSettings) {
            val email = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>(AccountEmailArgKey)
                .orEmpty()
            AccountSettingsRoute(
                email = email,
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
            )
        }
        composable(AppRoutes.AppInfo) {
            AppInfoRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val AccountEmailArgKey: String = "settings_account_email"

package kr.jm.weddingledger.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.navigation.AppRoutes
import kr.jm.weddingledger.core.navigation.TopLevelDestination

@Composable
expect fun AdMobBanner(modifier: Modifier = Modifier)

fun shouldShowAdMobBanner(currentRoute: String?): Boolean {
    if (!BuildConfig.ENABLE_ADMOB_BANNER) return false

    return when (currentRoute) {
        TopLevelDestination.Home.route,
        TopLevelDestination.Money.route,
        TopLevelDestination.Schedule.route,
        TopLevelDestination.Guests.route,
        TopLevelDestination.Tools.route,
        AppRoutes.VenueComparison,
        AppRoutes.ValuesSurvey,
        -> true

        else -> false
    }
}

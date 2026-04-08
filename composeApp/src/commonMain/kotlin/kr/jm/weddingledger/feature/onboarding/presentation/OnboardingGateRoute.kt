package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kr.jm.weddingledger.core.ads.TestBannerAd
import kr.jm.weddingledger.core.ads.shouldShowTestBanner
import kr.jm.weddingledger.core.ads.syncHostBannerVisibility
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.navigation.AppBottomBar
import kr.jm.weddingledger.core.navigation.AppNavHost
import kr.jm.weddingledger.core.ui.components.WeddingLogSplashScreen
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingGateRoute(
    themeStyle: WeddingLogThemeStyle,
    holdSplash: Boolean,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    when {
        holdSplash || uiState.step == OnboardingStep.Loading -> WeddingLogSplashScreen(themeStyle = themeStyle)
        else -> when (uiState.step) {
            OnboardingStep.Done -> MainAppScaffold(
                themeStyle = themeStyle,
                onThemeStyleChange = onThemeStyleChange,
                onLogout = onLogout,
                onAccountDeleted = onAccountDeleted,
            )
            OnboardingStep.EntryChoice,
            OnboardingStep.ProfileSetup,
            OnboardingStep.InviteShare,
            OnboardingStep.InviteJoin,
            -> OnboardingRoute(viewModel = viewModel)
            OnboardingStep.Loading -> WeddingLogSplashScreen(themeStyle = themeStyle)
        }
    }
}

@Composable
private fun MainAppScaffold(
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val shouldShowBanner = shouldShowTestBanner(backStackEntry?.destination?.route)

    LaunchedEffect(shouldShowBanner) {
        syncHostBannerVisibility(shouldShowBanner)
    }

    DisposableEffect(Unit) {
        onDispose {
            syncHostBannerVisibility(false)
        }
    }

    // Avoid recreating lambda in bottom bar recompositions.
    val onNavigate: (kr.jm.weddingledger.core.navigation.TopLevelDestination) -> Unit = remember(navController) {
        { destination ->
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                if (shouldShowBanner) {
                    TestBannerAd()
                }
                AppBottomBar(
                    currentRoute = backStackEntry?.destination?.route,
                    onNavigate = onNavigate,
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppNavHost(
                navController = navController,
                themeStyle = themeStyle,
                onThemeStyleChange = onThemeStyleChange,
                onLogout = onLogout,
                onAccountDeleted = onAccountDeleted,
            )
        }
    }
}

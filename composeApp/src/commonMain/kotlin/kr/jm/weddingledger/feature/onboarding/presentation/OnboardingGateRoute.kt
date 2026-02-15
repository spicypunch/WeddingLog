package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.navigation.AppBottomBar
import kr.jm.weddingledger.core.navigation.AppNavHost
import kr.jm.weddingledger.feature.onboarding.domain.model.OnboardingStep
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingGateRoute(
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onLogout: () -> Unit,
) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    when (uiState.step) {
        OnboardingStep.Loading -> GateLoadingScreen()
        OnboardingStep.Done -> MainAppScaffold(
            themeStyle = themeStyle,
            onThemeStyleChange = onThemeStyleChange,
            onLogout = onLogout,
        )
        OnboardingStep.ProfileSetup,
        OnboardingStep.PartnerConnect,
        -> OnboardingRoute(viewModel = viewModel)
    }
}

@Composable
private fun MainAppScaffold(
    themeStyle: WeddingLogThemeStyle,
    onThemeStyleChange: (WeddingLogThemeStyle) -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

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
            AppBottomBar(
                currentRoute = backStackEntry?.destination?.route,
                onNavigate = onNavigate,
            )
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
            )
        }
    }
}

@Composable
private fun GateLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "WeddingLog")
            CircularProgressIndicator()
        }
    }
}


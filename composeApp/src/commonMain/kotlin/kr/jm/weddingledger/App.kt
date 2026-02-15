package kr.jm.weddingledger

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kr.jm.weddingledger.core.designsystem.WeddingLogTheme
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.navigation.AppBottomBar
import kr.jm.weddingledger.core.navigation.AppNavHost
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.auth.presentation.AuthRoute
import kr.jm.weddingledger.feature.onboarding.presentation.OnboardingGateRoute
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    var themeStyle by remember { mutableStateOf(WeddingLogThemeStyle.Minimal) }

    WeddingLogTheme(style = themeStyle) {
        val observeAuthStateUseCase: ObserveAuthStateUseCase = koinInject()
        val authState by observeAuthStateUseCase().collectAsState(initial = AuthState.Loading)

        when (authState) {
            AuthState.Loading -> SplashScreen()
            is AuthState.Unauthenticated -> {
                AuthRoute(
                    initialMessage = (authState as AuthState.Unauthenticated).reason
                )
            }
            is AuthState.Authenticated -> {
                val scope = rememberCoroutineScope()
                val signOutUseCase: SignOutUseCase = koinInject()

                OnboardingGateRoute(
                    themeStyle = themeStyle,
                    onThemeStyleChange = { themeStyle = it },
                    onLogout = { scope.launch { signOutUseCase() } },
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
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

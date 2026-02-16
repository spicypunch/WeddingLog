package kr.jm.weddingledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kr.jm.weddingledger.core.designsystem.WeddingLogTheme
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.settings.data.ThemeStylePreferenceStorage
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.usecase.HandleAuthDeepLinkUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.auth.presentation.AuthDeepLinkBridge
import kr.jm.weddingledger.feature.auth.presentation.AuthRoute
import kr.jm.weddingledger.feature.onboarding.presentation.OnboardingGateRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val themeStyle by ThemeStylePreferenceStorage.themeStyle.collectAsState(initial = WeddingLogThemeStyle.Minimal)
    var authInitialMessage by remember { mutableStateOf<String?>(null) }

    WeddingLogTheme(style = themeStyle) {
        val observeAuthStateUseCase: ObserveAuthStateUseCase = koinInject()
        val handleAuthDeepLinkUseCase: HandleAuthDeepLinkUseCase = koinInject()
        val authState by observeAuthStateUseCase().collectAsState(initial = AuthState.Loading)

        LaunchedEffect(Unit) {
            AuthDeepLinkBridge.deepLinks.collect { url ->
                when (val result = handleAuthDeepLinkUseCase(url)) {
                    is AuthResult.Success -> authInitialMessage = null
                    is AuthResult.Error -> authInitialMessage = result.message
                }
            }
        }

        LaunchedEffect(Unit) {
            AuthDeepLinkBridge.messages.collect { message ->
                authInitialMessage = message
            }
        }

        LaunchedEffect(authState) {
            if (authState is AuthState.Authenticated) {
                authInitialMessage = null
            }
        }

        when (authState) {
            AuthState.Loading -> SplashScreen()
            is AuthState.Unauthenticated -> {
                AuthRoute(
                    initialMessage = authInitialMessage ?: (authState as AuthState.Unauthenticated).reason,
                )
            }
            is AuthState.Authenticated -> {
                val signOutUseCase: SignOutUseCase = koinInject()

                OnboardingGateRoute(
                    themeStyle = themeStyle,
                    onThemeStyleChange = {
                        scope.launch { ThemeStylePreferenceStorage.saveThemeStyle(it) }
                    },
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

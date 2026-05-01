package kr.jm.weddingledger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kr.jm.weddingledger.core.designsystem.WeddingLogTheme
import kr.jm.weddingledger.core.designsystem.WeddingLogThemeStyle
import kr.jm.weddingledger.core.settings.data.ThemeStylePreferenceStorage
import kr.jm.weddingledger.core.ui.components.WeddingLogSplashScreen
import kr.jm.weddingledger.feature.auth.domain.model.AuthResult
import kr.jm.weddingledger.feature.auth.domain.model.AuthState
import kr.jm.weddingledger.feature.auth.domain.usecase.HandleAuthDeepLinkUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kr.jm.weddingledger.feature.auth.domain.usecase.SignOutUseCase
import kr.jm.weddingledger.feature.auth.presentation.AuthDeepLinkBridge
import kr.jm.weddingledger.feature.auth.presentation.AuthMessage
import kr.jm.weddingledger.feature.auth.presentation.AuthRoute
import kr.jm.weddingledger.feature.notifications.domain.repository.PartnerActivityNotifier
import kr.jm.weddingledger.feature.onboarding.presentation.OnboardingGateRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val themeStyle by ThemeStylePreferenceStorage.themeStyle.collectAsState(initial = WeddingLogThemeStyle.Minimal)
    var authInitialMessage by remember { mutableStateOf<AuthMessage?>(null) }
    var isMinimumSplashElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3_000)
        isMinimumSplashElapsed = true
    }

    WeddingLogTheme(style = themeStyle) {
        val observeAuthStateUseCase: ObserveAuthStateUseCase = koinInject()
        val handleAuthDeepLinkUseCase: HandleAuthDeepLinkUseCase = koinInject()
        val authStateFlow = remember(observeAuthStateUseCase) { observeAuthStateUseCase() }
        val authState by authStateFlow.collectAsState(initial = AuthState.Loading)

        LaunchedEffect(Unit) {
            AuthDeepLinkBridge.deepLinks.collect { url ->
                when (val result = handleAuthDeepLinkUseCase(url)) {
                    is AuthResult.Success -> {
                        authInitialMessage = AuthMessage.Info(deepLinkSuccessMessage(url))
                    }
                    is AuthResult.Error -> {
                        authInitialMessage = AuthMessage.Error(result.message)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            AuthDeepLinkBridge.messages.collect { message ->
                authInitialMessage = AuthMessage.Info(message)
            }
        }

        LaunchedEffect(authState) {
            if (authState is AuthState.Authenticated) {
                authInitialMessage = null
            }
        }

        when (authState) {
            AuthState.Loading -> WeddingLogSplashScreen(themeStyle = themeStyle)
            is AuthState.Unauthenticated -> {
                if (isMinimumSplashElapsed) {
                    AuthRoute(
                        initialMessage = authInitialMessage
                            ?: (authState as AuthState.Unauthenticated).reason?.let(AuthMessage::Error),
                    )
                } else {
                    WeddingLogSplashScreen(themeStyle = themeStyle)
                }
            }
            is AuthState.Authenticated -> {
                val authenticatedUser = (authState as AuthState.Authenticated).user
                val signOutUseCase: SignOutUseCase = koinInject()
                val partnerActivityNotifier: PartnerActivityNotifier = koinInject()

                LaunchedEffect(authenticatedUser.id) {
                    partnerActivityNotifier.syncCurrentDevicePushToken()
                }

                key(authenticatedUser.id) {
                    OnboardingGateRoute(
                        userId = authenticatedUser.id,
                        themeStyle = themeStyle,
                        holdSplash = !isMinimumSplashElapsed,
                        onThemeStyleChange = {
                            scope.launch { ThemeStylePreferenceStorage.saveThemeStyle(it) }
                        },
                        onLogout = {
                            scope.launch {
                                partnerActivityNotifier.deactivateCurrentDevicePushToken()
                                signOutUseCase()
                            }
                        },
                        onAccountDeleted = {
                            authInitialMessage = AuthMessage.Info("회원탈퇴가 완료되었습니다.")
                        },
                    )
                }
            }
        }
    }
}

private fun deepLinkSuccessMessage(url: String): String {
    return when {
        url.contains("type=signup", ignoreCase = true) ||
            url.contains("type=email", ignoreCase = true) -> {
            "이메일 인증이 완료되었습니다. 로그인 후 계속 진행해주세요."
        }

        url.contains("type=recovery", ignoreCase = true) -> {
            "비밀번호 재설정 링크를 확인했습니다. 새로운 비밀번호로 로그인해주세요."
        }

        else -> "인증 링크를 확인했습니다."
    }
}

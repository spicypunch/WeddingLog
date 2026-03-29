package kr.jm.weddingledger.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import kr.jm.weddingledger.core.settings.data.LegalConsentPreferenceStorage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthRoute(
    initialMessage: AuthMessage? = null,
) {
    val viewModel: AuthViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val hasAcceptedLegalConsent by LegalConsentPreferenceStorage.hasAcceptedCurrentVersion.collectAsState(initial = false)

    val handleAction: (AuthAction) -> Unit = { action ->
        when (action) {
            is AuthAction.OnTermsAcceptedChange -> {
                if (action.accepted) {
                    scope.launch { LegalConsentPreferenceStorage.markAcceptedCurrentVersion() }
                }
                viewModel.onAction(action)
            }
            else -> viewModel.onAction(action)
        }
    }

    LaunchedEffect(initialMessage) {
        if (initialMessage != null) {
            viewModel.onAction(AuthAction.OnShowMessage(initialMessage))
        }
    }

    LaunchedEffect(hasAcceptedLegalConsent) {
        if (hasAcceptedLegalConsent) {
            viewModel.onAction(AuthAction.OnTermsAcceptedChange(true))
        }
    }

    LaunchedEffect(viewModel, uriHandler) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.OpenExternalUrl -> {
                    runCatching { uriHandler.openUri(effect.url) }
                        .onFailure {
                            viewModel.onAction(
                                AuthAction.OnShowMessage(
                                    AuthMessage.Error("문서를 열지 못했습니다. 잠시 후 다시 시도해주세요.")
                                )
                            )
                        }
                }
            }
        }
    }

    AuthScreen(
        state = state,
        onAction = handleAction,
    )
}

package kr.jm.weddingledger.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.getPlatform
import kr.jm.weddingledger.core.ui.keyboard.keyboardAwareBottomPadding
import kr.jm.weddingledger.core.ui.keyboard.rememberImeVisible

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    SignInScreen(state = state, onAction = onAction)
}

@Composable
private fun SignInScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    val primary = Color(0xFF7D7463)
    val background = Color(0xFFF9F8F6)
    val border = Color(0xFFE5E5E5)
    val textMain = Color(0xFF333333)
    val textMuted = Color(0xFF999999)
    val isImeVisible = rememberImeVisible()
    val scrollState = rememberScrollState()
    val isAndroid = getPlatform().name.startsWith("Android", ignoreCase = true)
    val socialOrder = if (isAndroid) {
        listOf(
            SocialProvider.Kakao,
            // SocialProvider.Google, // 첫 출시 안정화를 위해 일시 미노출
        )
    } else {
        listOf(
            SocialProvider.Kakao,
            SocialProvider.Apple,
            // SocialProvider.Google, // 첫 출시 안정화를 위해 일시 미노출
        )
    }
    val orderedSocialProviders = socialOrder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        BackgroundBlobs(primary = primary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(
                    top = if (isImeVisible) 20.dp else 28.dp,
                    bottom = keyboardAwareBottomPadding(
                        normalBottomPadding = 28.dp,
                        imeVisibleBottomPadding = 16.dp,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isImeVisible) Arrangement.Top else Arrangement.Center,
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(primary.copy(alpha = 0.10f), CircleShape)
                    .border(
                        BorderStroke(1.dp, primary.copy(alpha = 0.20f)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WeddingLog",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "당신의 소중한 순간을 기록하세요",
                style = MaterialTheme.typography.bodySmall,
                color = textMuted,
            )

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                orderedSocialProviders.forEach { provider ->
                    when (provider) {
                        SocialProvider.Kakao -> {
                            Button(
                                onClick = { onAction(AuthAction.OnClickSignInWithKakao) },
                                enabled = state.isSocialSignInEnabled && state.isKakaoLoginAvailable,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFEE500),
                                    contentColor = Color(0xFF181600),
                                    disabledContainerColor = Color(0xFFFEE500).copy(alpha = 0.35f),
                                    disabledContentColor = Color(0xFF181600).copy(alpha = 0.75f),
                                ),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Text(
                                    text = "Kakao로 계속하기",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        SocialProvider.Google -> {
                            Button(
                                onClick = { onAction(AuthAction.OnClickSignInWithGoogle) },
                                enabled = state.isSocialSignInEnabled && state.isGoogleLoginAvailable,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2937),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF1F2937).copy(alpha = 0.35f),
                                    disabledContentColor = Color.White.copy(alpha = 0.85f),
                                ),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Text(
                                    text = "Google로 계속하기",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        SocialProvider.Apple -> {
                            OutlinedButton(
                                onClick = { onAction(AuthAction.OnClickSignInWithApple) },
                                enabled = state.isSocialSignInEnabled && state.isAppleLoginAvailable,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, border),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Text(
                                    text = "Apple로 계속하기",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = textMain,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                if (!state.isTermsAccepted) {
                    TermsAgreementRow(
                        state = state,
                        primary = primary,
                        textColor = textMuted,
                        checkboxColor = border,
                        onAction = onAction,
                    )
                }

                if (!state.hasAnySocialLoginAvailable) {
                    Text(
                        text = "현재 사용 가능한 소셜 로그인 제공자가 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.message != null) {
                    AuthMessageBanner(
                        message = state.message,
                        primary = primary,
                        onDismiss = { onAction(AuthAction.OnDismissMessage) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "© WeddingLog Corp.",
                style = MaterialTheme.typography.labelSmall,
                color = textMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class SocialProvider {
    Kakao,
    Google,
    Apple,
}

private const val TERMS_LINK_TAG = "terms_link"
private const val PRIVACY_LINK_TAG = "privacy_link"

@Composable
private fun TermsAgreementRow(
    state: AuthUiState,
    primary: Color,
    textColor: Color,
    checkboxColor: Color,
    onAction: (AuthAction) -> Unit,
) {
    val annotatedText = remember(primary, textColor) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = primary,
                    fontWeight = FontWeight.SemiBold,
                )
            ) {
                append("(필수) ")
            }

            pushStringAnnotation(tag = TERMS_LINK_TAG, annotation = "terms")
            withStyle(
                SpanStyle(
                    color = textColor,
                    textDecoration = TextDecoration.Underline,
                )
            ) {
                append("이용약관")
            }
            pop()

            withStyle(SpanStyle(color = textColor)) {
                append(" 및 ")
            }

            pushStringAnnotation(tag = PRIVACY_LINK_TAG, annotation = "privacy")
            withStyle(
                SpanStyle(
                    color = textColor,
                    textDecoration = TextDecoration.Underline,
                )
            ) {
                append("개인정보 처리방침")
            }
            pop()

            withStyle(SpanStyle(color = textColor)) {
                append("에 동의합니다.")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = state.isTermsAccepted,
            onCheckedChange = { onAction(AuthAction.OnTermsAcceptedChange(it)) },
            enabled = !state.isLoading,
            colors = CheckboxDefaults.colors(
                checkedColor = primary,
                checkmarkColor = Color.White,
                uncheckedColor = checkboxColor,
            ),
        )
        Spacer(modifier = Modifier.size(6.dp))
        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall.copy(color = textColor),
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp),
            onClick = { offset ->
                when (annotatedText.getStringAnnotations(start = offset, end = offset).firstOrNull()?.tag) {
                    TERMS_LINK_TAG -> onAction(AuthAction.OnClickTermsOfService)
                    PRIVACY_LINK_TAG -> onAction(AuthAction.OnClickPrivacyPolicy)
                }
            },
        )
    }
}

@Composable
private fun BackgroundBlobs(primary: Color) {
    val alpha = 0.05f
    val blurRadius = 80.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Compose padding does not allow negative values; use offset for "off-screen" placement.
                .offset(x = 72.dp, y = (-72).dp)
                .size(420.dp)
                .background(primary.copy(alpha = alpha), CircleShape)
                .blur(blurRadius)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-72).dp, y = 72.dp)
                .size(360.dp)
                .background(primary.copy(alpha = alpha), CircleShape)
                .blur(blurRadius)
        )
    }
}

@Composable
private fun AuthMessageBanner(
    message: AuthMessage,
    primary: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (message) {
        is AuthMessage.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        is AuthMessage.Info -> primary.copy(alpha = 0.10f) to primary
    }

    Surface(
        onClick = onDismiss,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Text(
            text = when (message) {
                is AuthMessage.Error -> message.text
                is AuthMessage.Info -> message.text
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

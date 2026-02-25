package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.keyboard.keyboardAwareBottomPadding
import kr.jm.weddingledger.core.ui.keyboard.rememberImeVisible

@Composable
fun InviteJoinScreen(
    state: InviteJoinUiState,
    isLoading: Boolean,
    message: String?,
    onAction: (OnboardingAction) -> Unit,
) {
    val primary = Color(0xFFEE2B8C)
    val background = Color(0xFFF8F6F7)
    val card = Color.White
    val field = Color(0xFFF3F4F6)
    val border = Color(0xFFE5E7EB)
    val textMain = Color(0xFF111827)
    val textSub = Color(0xFF6B7280)
    val isImeVisible = rememberImeVisible()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .background(primary.copy(alpha = 0.06f), CircleShape)
                .alpha(0.75f),
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .background(primary.copy(alpha = 0.05f), CircleShape)
                .alpha(0.65f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(
                    top = 28.dp,
                    bottom = keyboardAwareBottomPadding(
                        normalBottomPadding = 24.dp,
                        imeVisibleBottomPadding = 32.dp,
                    ),
                ),
        ) {
            Spacer(modifier = Modifier.height(if (isImeVisible) 4.dp else 8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = primary,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "공유 코드로 연결하기",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textMain,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "닉네임과 초대 코드만 입력하면 바로 함께 시작할 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSub,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (!message.isNullOrBlank()) {
                InviteJoinMessage(
                    text = message,
                    onDismiss = { onAction(OnboardingAction.OnDismissMessage) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Surface(
                color = card,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, border),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = { onAction(OnboardingAction.OnJoinNicknameChange(it)) },
                        placeholder = { Text(text = "닉네임을 입력해주세요", color = textSub) },
                        enabled = !isLoading,
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = textSub,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = field,
                            unfocusedContainerColor = field,
                            disabledContainerColor = field,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            disabledTextColor = textMain.copy(alpha = 0.6f),
                            cursorColor = primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.partnerCodeInput,
                        onValueChange = { onAction(OnboardingAction.OnPartnerCodeInputChange(it.uppercase())) },
                        placeholder = { Text(text = "초대 코드를 입력해주세요", color = textSub) },
                        enabled = !isLoading,
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = textSub,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = field,
                            unfocusedContainerColor = field,
                            disabledContainerColor = field,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            disabledTextColor = textMain.copy(alpha = 0.6f),
                            cursorColor = primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onAction(OnboardingAction.OnClickConnectPartner) },
                        enabled = state.isConnectEnabled && !isLoading,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = Color.White,
                            disabledContainerColor = primary.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.85f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(text = "파트너 연결하기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "시작 방식 선택으로 돌아가기",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textSub,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { onAction(OnboardingAction.OnClickBackToEntry) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun InviteJoinMessage(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.error,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "닫기",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

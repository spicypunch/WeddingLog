package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun PartnerConnectScreen(
    state: PartnerConnectUiState,
    isLoading: Boolean,
    message: String?,
    onAction: (OnboardingAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primary = Color(0xFFEE2B8C)
    val background = if (isDark) Color(0xFF221019) else Color(0xFFF8F6F7)
    val surface = if (isDark) Color(0xFF1C0F14) else Color.White
    val card = if (isDark) Color(0xFF2A1720) else Color.White
    val field = if (isDark) Color(0xFF120A0E) else Color(0xFFF3F4F6)
    val border = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
    val textMain = if (isDark) Color(0xFFF6F6F7) else Color(0xFF111827)
    val textSub = if (isDark) Color(0xFFC7B7C0) else Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Background blobs
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp)
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
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
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
                    text = "파트너와 연결하기",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textMain,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "결혼 준비의 시작, 파트너와 함께하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSub,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (!message.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    contentColor = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
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
                                .clickable { onAction(OnboardingAction.OnDismissMessage) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // My invite code card
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "MY INVITE CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textSub,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.inviteCode.isBlank()) "W-0000" else state.inviteCode,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onAction(OnboardingAction.OnClickInviteShare) },
                            enabled = !isLoading && state.inviteCode.isNotBlank(),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primary.copy(alpha = 0.12f),
                                contentColor = primary,
                                disabledContainerColor = primary.copy(alpha = 0.06f),
                                disabledContentColor = primary.copy(alpha = 0.55f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.IosShare,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "초대 코드 공유하기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // OR divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textSub,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = border)
                }

                // Partner code input
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
                        Text(
                            text = "초대 코드를 받으셨나요?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textMain,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = state.partnerCodeInput,
                            onValueChange = { onAction(OnboardingAction.OnPartnerCodeInputChange(it.uppercase())) },
                            placeholder = { Text(text = "코드를 입력해주세요", color = textSub) },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "나중에 연결하고 혼자 먼저 시작할게요",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textSub,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { onAction(OnboardingAction.OnClickSkipPartner) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

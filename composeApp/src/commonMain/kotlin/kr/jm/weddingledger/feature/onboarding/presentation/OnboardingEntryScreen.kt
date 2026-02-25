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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun OnboardingEntryScreen(
    isLoading: Boolean,
    message: String?,
    onAction: (OnboardingAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primary = Color(0xFFE64C19)
    val accent = Color(0xFFEE2B8C)
    val background = if (isDark) Color(0xFF1F1512) else Color(0xFFF8F6F6)
    val card = if (isDark) Color(0xFF2A1E1A) else Color.White
    val border = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
    val textMain = if (isDark) Color(0xFFF3F3F2) else Color(0xFF111827)
    val textSub = if (isDark) Color(0xFFBBB3AD) else Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .background(primary.copy(alpha = 0.07f), CircleShape)
                .alpha(0.75f),
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .background(accent.copy(alpha = 0.06f), CircleShape)
                .alpha(0.70f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "어떻게 시작할까요?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textMain,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "원하는 방법을 선택하면 바로 다음 단계로 안내해드려요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!message.isNullOrBlank()) {
                OnboardingInlineMessage(
                    text = message,
                    onDismiss = { onAction(OnboardingAction.OnDismissMessage) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EntryOptionCard(
                    title = "혼자 시작하기",
                    description = "닉네임, 역할, 예식일을 입력하고 바로 시작할게요.",
                    iconTint = primary,
                    icon = Icons.Rounded.Favorite,
                    buttonText = "혼자 시작",
                    onClick = { onAction(OnboardingAction.OnClickStartOnboarding) },
                    enabled = !isLoading,
                    cardColor = card,
                    borderColor = border,
                    textMain = textMain,
                    textSub = textSub,
                    filled = true,
                )

                EntryOptionCard(
                    title = "공유 코드로 시작하기",
                    description = "파트너가 보낸 공유 코드와 닉네임으로 함께 시작할게요.",
                    iconTint = accent,
                    icon = Icons.Rounded.PersonAddAlt1,
                    buttonText = "공유 코드 입력",
                    onClick = { onAction(OnboardingAction.OnClickJoinWithSharedCode) },
                    enabled = !isLoading,
                    cardColor = card,
                    borderColor = border,
                    textMain = textMain,
                    textSub = textSub,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun EntryOptionCard(
    title: String,
    description: String,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    onClick: () -> Unit,
    enabled: Boolean,
    cardColor: Color,
    borderColor: Color,
    textMain: Color,
    textSub: Color,
    filled: Boolean,
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = textMain,
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = textSub,
            )

            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(999.dp),
                colors = if (filled) {
                    ButtonDefaults.buttonColors(
                        containerColor = iconTint,
                        contentColor = Color.White,
                        disabledContainerColor = iconTint.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.85f),
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = iconTint.copy(alpha = 0.12f),
                        contentColor = iconTint,
                        disabledContainerColor = iconTint.copy(alpha = 0.06f),
                        disabledContentColor = iconTint.copy(alpha = 0.50f),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OnboardingInlineMessage(
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

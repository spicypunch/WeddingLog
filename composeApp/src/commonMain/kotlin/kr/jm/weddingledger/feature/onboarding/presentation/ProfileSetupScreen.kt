package kr.jm.weddingledger.feature.onboarding.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.feature.onboarding.domain.model.UserRole

@Composable
fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    isLoading: Boolean,
    message: String?,
    onAction: (OnboardingAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primary = Color(0xFFE64C19)
    val background = if (isDark) Color(0xFF211511) else Color(0xFFF8F6F6)
    val surface = if (isDark) Color(0xFF2A1E1A) else Color.White
    val fieldBg = if (isDark) Color.White.copy(alpha = 0.06f) else background
    val border = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
    val textMain = if (isDark) Color(0xFFF2F2F2) else Color(0xFF111827)
    val textSub = if (isDark) Color(0xFFBDB6B0) else Color(0xFF6B7280)
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 24.dp,
                        bottom = if (isImeVisible) 24.dp else 110.dp,
                    ),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(primary.copy(alpha = if (isDark) 0.16f else 0.10f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Diamond,
                            contentDescription = null,
                            tint = primary.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(40.dp)
                                .padding(start = 2.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .border(BorderStroke(2.dp, primary.copy(alpha = 0.35f)), CircleShape)
                                .align(Alignment.TopStart)
                                .padding(0.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "우리만의 웨딩로그를\n시작해볼까요?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textMain,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "간단한 정보 입력으로 나만의 플래너를 만들어보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(26.dp))

                if (!message.isNullOrBlank()) {
                    MessageBanner(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        onDismiss = { onAction(OnboardingAction.OnDismissMessage) },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = "닉네임",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = { onAction(OnboardingAction.OnNicknameChange(it)) },
                    placeholder = { Text(text = "닉네임을 입력해주세요", color = textSub) },
                    enabled = !isLoading,
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = textSub,
                        )
                    },
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        disabledContainerColor = fieldBg,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        disabledTextColor = textMain.copy(alpha = 0.6f),
                        cursorColor = primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(2.dp, if (state.nickname.trim().isNotEmpty()) primary.copy(alpha = 0.35f) else border),
                            RoundedCornerShape(16.dp),
                        ),
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "나의 역할",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RoleCard(
                        label = UserRole.Groom.label,
                        icon = Icons.Rounded.Person,
                        selected = state.selectedRole == UserRole.Groom,
                        primary = primary,
                        surface = fieldBg,
                        border = border,
                        textMain = textMain,
                        textSub = textSub,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(OnboardingAction.OnSelectRole(UserRole.Groom)) },
                    )
                    RoleCard(
                        label = UserRole.Bride.label,
                        icon = Icons.Rounded.Favorite,
                        selected = state.selectedRole == UserRole.Bride,
                        primary = primary,
                        surface = fieldBg,
                        border = border,
                        textMain = textMain,
                        textSub = textSub,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(OnboardingAction.OnSelectRole(UserRole.Bride)) },
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "예식일",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = { onAction(OnboardingAction.OnClickWeddingDate) },
                    enabled = !state.isDateUndecided && !isLoading,
                    color = fieldBg,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, if (state.weddingDate != null) primary.copy(alpha = 0.25f) else border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(primary.copy(alpha = 0.10f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = primary,
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (state.isDateUndecided) "날짜를 선택해주세요" else state.weddingDateText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.weddingDate == null || state.isDateUndecided) textSub else textMain,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = textSub,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) {
                            onAction(OnboardingAction.OnToggleDateUndecided(!state.isDateUndecided))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.isDateUndecided,
                        onCheckedChange = { onAction(OnboardingAction.OnToggleDateUndecided(it)) },
                        enabled = !isLoading,
                        colors = CheckboxDefaults.colors(
                            checkedColor = primary,
                            uncheckedColor = border,
                            checkmarkColor = Color.White,
                        ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "아직 정해지지 않았어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSub,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (isImeVisible) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { onAction(OnboardingAction.OnClickProfileNext) },
                        enabled = state.isNextEnabled && !isLoading,
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
                        Text(text = "다음으로", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }

            // Sticky bottom button
            if (!isImeVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface.copy(alpha = 0.92f))
                        .border(BorderStroke(1.dp, border.copy(alpha = 0.75f)))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                ) {
                    Button(
                        onClick = { onAction(OnboardingAction.OnClickProfileNext) },
                        enabled = state.isNextEnabled && !isLoading,
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
                        Text(text = "다음으로", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    primary: Color,
    surface: Color,
    border: Color,
    textMain: Color,
    textSub: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val container = if (selected) primary.copy(alpha = 0.06f) else surface
    val stroke = if (selected) primary else Color.Transparent

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            color = container,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, if (selected) stroke else Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.55f), CircleShape)
                        .border(BorderStroke(1.dp, border), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) primary else textSub,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) primary else textMain,
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(22.dp)
                    .background(primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
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
                    .clickable { onDismiss() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

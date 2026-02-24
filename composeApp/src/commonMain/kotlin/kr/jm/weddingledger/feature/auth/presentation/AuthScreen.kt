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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    when (state.mode) {
        AuthMode.SignIn -> SignInScreen(state = state, onAction = onAction)
        AuthMode.SignUp -> SignUpScreen(state = state, onAction = onAction)
    }
}

@Composable
private fun SignInScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primary = Color(0xFF7D7463)
    val primaryDark = Color(0xFF665D4F)
    val background = if (isDark) Color(0xFF1A1918) else Color(0xFFF9F8F6)
    val surface = if (isDark) Color(0xFF232220) else Color(0xFFFFFFFF)
    val border = if (isDark) Color(0xFF3A3A36) else Color(0xFFE5E5E5)
    val textMain = if (isDark) Color(0xFFF1F1F0) else Color(0xFF333333)
    val textMuted = if (isDark) Color(0xFFB4B0AA) else Color(0xFF999999)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        BackgroundBlobs(primary = primary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(primary.copy(alpha = if (isDark) 0.20f else 0.10f), CircleShape)
                    .border(
                        BorderStroke(1.dp, primary.copy(alpha = if (isDark) 0.35f else 0.20f)),
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
                LabeledOutlinedField(
                    label = "이메일",
                    value = state.email,
                    onValueChange = { onAction(AuthAction.OnEmailChange(it)) },
                    placeholder = "이메일 주소를 입력해주세요",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = surface,
                    borderColor = border,
                    focusedBorderColor = primary,
                    textColor = textMain,
                    labelColor = if (isDark) Color(0xFFD6D6D3) else Color(0xFF4B5563),
                )

                LabeledOutlinedField(
                    label = "비밀번호",
                    value = state.password,
                    onValueChange = { onAction(AuthAction.OnPasswordChange(it)) },
                    placeholder = "비밀번호를 입력해주세요",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = surface,
                    borderColor = border,
                    focusedBorderColor = primary,
                    textColor = textMain,
                    labelColor = if (isDark) Color(0xFFD6D6D3) else Color(0xFF4B5563),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onAction(AuthAction.OnClickTogglePasswordVisibility) },
                            enabled = !state.isLoading,
                        ) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) {
                                    Icons.Rounded.Visibility
                                } else {
                                    Icons.Rounded.VisibilityOff
                                },
                                contentDescription = null,
                                tint = textMuted,
                            )
                        }
                    },
                )

                if (state.shouldShowVerificationGuide) {
                    EmailVerificationGuideCard(
                        email = state.email.trim(),
                        remainingAttempts = state.remainingResendAttempts,
                        resendButtonLabel = state.resendButtonLabel,
                        canResend = state.isResendVerificationEnabled,
                        isResending = state.isResendingVerificationEmail,
                        onClickResend = { onAction(AuthAction.OnClickResendVerificationEmail) },
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

                Button(
                    onClick = { onAction(AuthAction.OnClickSubmit) },
                    enabled = state.isSubmitEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White,
                        disabledContainerColor = primary.copy(alpha = 0.40f),
                        disabledContentColor = Color.White.copy(alpha = 0.85f),
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "로그인",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onAction(AuthAction.OnClickToggleMode) },
                        enabled = !state.isLoading && !state.isResendingVerificationEmail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = "회원가입",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFBDB7B0) else Color(0xFF6B7280),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .width(1.dp)
                            .background(border)
                    )
                    TextButton(
                        onClick = { onAction(AuthAction.OnClickResetPassword) },
                        enabled = !state.isLoading && !state.isResendingVerificationEmail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = "비밀번호 재설정",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFBDB7B0) else Color(0xFF6B7280),
                        )
                    }
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

@Composable
private fun SignUpScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val primary = Color(0xFFEF3948)
    val surface = if (isDark) Color(0xFF2A1A1C) else Color(0xFFFFFFFF)
    val background = if (isDark) Color(0xFF221012) else Color(0xFFF8F6F6)

    // Field colors (filled look with subtle border)
    val fieldContainer = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF9FAFB)
    val fieldBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
    val textMain = if (isDark) Color(0xFFF2F2F2) else Color(0xFF111827)
    val textSub = if (isDark) Color(0xFFB5B5B5) else Color(0xFF6B7280)
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(surface),
        ) {
            // Top navigation (back)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 56.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onAction(AuthAction.OnClickToggleMode) },
                    enabled = !state.isLoading,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = null,
                        tint = textMain,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp)
                    .padding(
                        top = 12.dp,
                        bottom = if (isImeVisible) 24.dp else 120.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "회원가입",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = textMain,
                    )
                    Text(
                        text = "웨딩로그와 함께\n결혼 준비를 시작해보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSub,
                    )
                }

                LabeledOutlinedField(
                    label = "이메일",
                    value = state.email,
                    onValueChange = { onAction(AuthAction.OnEmailChange(it)) },
                    placeholder = "example@email.com",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = fieldContainer,
                    borderColor = fieldBorder,
                    focusedBorderColor = primary,
                    textColor = textMain,
                    labelColor = if (isDark) Color(0xFFD7D7D7) else Color(0xFF374151),
                )

                LabeledOutlinedField(
                    label = "비밀번호",
                    value = state.password,
                    onValueChange = { onAction(AuthAction.OnPasswordChange(it)) },
                    placeholder = "8자리 이상 입력",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = fieldContainer,
                    borderColor = fieldBorder,
                    focusedBorderColor = primary,
                    textColor = textMain,
                    labelColor = if (isDark) Color(0xFFD7D7D7) else Color(0xFF374151),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onAction(AuthAction.OnClickTogglePasswordVisibility) },
                            enabled = !state.isLoading,
                        ) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) {
                                    Icons.Rounded.Visibility
                                } else {
                                    Icons.Rounded.VisibilityOff
                                },
                                contentDescription = null,
                                tint = textSub,
                            )
                        }
                    },
                )

                LabeledOutlinedField(
                    label = "비밀번호 확인",
                    value = state.passwordConfirm,
                    onValueChange = { onAction(AuthAction.OnPasswordConfirmChange(it)) },
                    placeholder = "비밀번호를 다시 입력해주세요",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = fieldContainer,
                    borderColor = fieldBorder,
                    focusedBorderColor = primary,
                    textColor = textMain,
                    labelColor = if (isDark) Color(0xFFD7D7D7) else Color(0xFF374151),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                )

                // Terms (required)
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
                            uncheckedColor = fieldBorder,
                        ),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            ) {
                                append("(필수) ")
                            }
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("이용약관")
                            }
                            append(" 및 ")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("개인정보 처리방침")
                            }
                            append("에 동의합니다.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = textSub,
                        modifier = Modifier.padding(top = 12.dp),
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

                if (isImeVisible) {
                    Button(
                        onClick = { onAction(AuthAction.OnClickSubmit) },
                        enabled = state.isSubmitEnabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = Color.White,
                            disabledContainerColor = primary.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.85f),
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "가입하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Bottom action bar
            if (!isImeVisible) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = fieldBorder)
                    Box(modifier = Modifier.padding(24.dp)) {
                        Button(
                            onClick = { onAction(AuthAction.OnClickSubmit) },
                            enabled = state.isSubmitEnabled,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primary,
                                contentColor = Color.White,
                                disabledContainerColor = primary.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f),
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "가입하기",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundBlobs(primary: Color) {
    val isDark = isSystemInDarkTheme()
    val alpha = if (isDark) 0.08f else 0.05f
    val blurRadius = if (isDark) 70.dp else 80.dp

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
private fun LabeledOutlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    shape: Shape,
    containerColor: Color,
    borderColor: Color,
    focusedBorderColor: Color,
    textColor: Color,
    labelColor: Color = textColor,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = focusedBorderColor,
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor.copy(alpha = 0.5f),
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                cursorColor = focusedBorderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = textColor.copy(alpha = 0.6f),
                focusedPlaceholderColor = borderColor,
                unfocusedPlaceholderColor = borderColor,
                disabledPlaceholderColor = borderColor.copy(alpha = 0.6f),
            ),
            shape = shape,
            modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun EmailVerificationGuideCard(
    email: String,
    remainingAttempts: Int,
    resendButtonLabel: String,
    canResend: Boolean,
    isResending: Boolean,
    onClickResend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "이메일 인증이 필요합니다",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (email.isNotBlank()) {
                    "${email} 메일함에서 인증 링크를 눌러주세요."
                } else {
                    "메일함에서 인증 링크를 눌러주세요."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "남은 재전송 ${remainingAttempts}회",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onClickResend,
                    enabled = canResend,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    if (isResending) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                    } else {
                        Text(
                            text = resendButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

package kr.jm.weddingledger.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "WeddingLog",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "예산, 하객, 일정까지 한 번에 관리해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { onAction(AuthAction.OnEmailChange(it)) },
            label = { Text("이메일") },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Mail, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(AuthAction.OnPasswordChange(it)) },
            label = { Text("비밀번호") },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.message != null) {
            AuthMessageBanner(
                message = state.message,
                onDismiss = { onAction(AuthAction.OnDismissMessage) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = { onAction(AuthAction.OnClickSubmit) },
            enabled = state.isSubmitEnabled,
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(text = state.submitLabel, style = MaterialTheme.typography.titleMedium)
            }
        }

        Column {
            TextButton(
                onClick = { onAction(AuthAction.OnClickToggleMode) },
                enabled = !state.isLoading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(text = state.toggleModeLabel)
            }
            TextButton(
                onClick = { onAction(AuthAction.OnClickResetPassword) },
                enabled = !state.isLoading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(text = "비밀번호를 잊으셨나요?")
            }
        }
    }
}

@Composable
private fun AuthMessageBanner(
    message: AuthMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (message) {
        is AuthMessage.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        is AuthMessage.Info -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) to MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onDismiss,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
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

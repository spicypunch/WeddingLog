package kr.jm.weddingledger.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.feature.notifications.domain.model.NotificationItemType

@Composable
fun NotificationCenterScreen(
    state: NotificationCenterUiState,
    onBack: () -> Unit,
    onAction: (NotificationCenterAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NotificationTopBar(
            title = state.title,
            onBack = onBack,
            onRefresh = { onAction(NotificationCenterAction.OnRefresh) },
        )

        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        state.message?.let { message ->
            InlineMessage(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                actionLabel = if (state.showPermissionAction) "권한 허용" else null,
                onClickAction = {
                    onAction(NotificationCenterAction.OnClickRequestPermission)
                },
                onDismiss = { onAction(NotificationCenterAction.OnDismissMessage) },
            )
        }

        if (!state.isLoading && state.activityItems.isEmpty() && state.reminderItems.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 40.dp),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.activityItems.isNotEmpty()) {
                item {
                    NotificationSectionTitle(title = "새 활동")
                }
                items(state.activityItems, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onClickScheduleReminder = {
                            onAction(NotificationCenterAction.OnClickScheduleReminder(item.id))
                        },
                        onClickCancelReminder = {
                            onAction(NotificationCenterAction.OnClickCancelReminder(item.id))
                        },
                    )
                }
            }

            if (state.reminderItems.isNotEmpty()) {
                item {
                    NotificationSectionTitle(title = "리마인더")
                }
                items(state.reminderItems, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onClickScheduleReminder = {
                            onAction(NotificationCenterAction.OnClickScheduleReminder(item.id))
                        },
                        onClickCancelReminder = {
                            onAction(NotificationCenterAction.OnClickCancelReminder(item.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun NotificationTopBar(
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onBack,
            color = Color.Transparent,
            shape = CircleShape,
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "뒤로가기",
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        TextButton(onClick = onRefresh) {
            Text(text = "새로고침")
        }
    }
}

@Composable
private fun InlineMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String?,
    onClickAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (!actionLabel.isNullOrBlank()) {
                TextButton(onClick = onClickAction) {
                    Text(text = actionLabel)
                }
            }
            TextButton(onClick = onDismiss) {
                Text(text = "닫기")
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "확인할 알림이 없어요.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "파트너 활동과 리마인더가 생기면 여기에서 확인할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItemUi,
    onClickScheduleReminder: () -> Unit,
    onClickCancelReminder: () -> Unit,
) {
    val visuals = item.type.toVisuals()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(visuals.containerColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        tint = visuals.contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (item.isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = visuals.containerColor,
                    contentColor = visuals.contentColor,
                    shape = CircleShape,
                ) {
                    Text(
                        text = visuals.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.canScheduleReminder) {
                if (item.isReminderScheduled) {
                    OutlinedButton(
                        onClick = onClickCancelReminder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(text = "리마인더 취소")
                    }
                } else {
                    Button(
                        onClick = onClickScheduleReminder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(text = "하루 전 리마인더 예약")
                    }
                }
            }
        }
    }
}

private data class ItemVisuals(
    val icon: ImageVector,
    val badgeLabel: String,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun NotificationItemType.toVisuals(): ItemVisuals {
    return when (this) {
        NotificationItemType.Schedule -> ItemVisuals(
            icon = Icons.Rounded.CalendarToday,
            badgeLabel = "일정",
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.primary,
        )

        NotificationItemType.Checklist -> ItemVisuals(
            icon = Icons.Rounded.Check,
            badgeLabel = "체크",
            containerColor = Color(0xFFD1FAE5),
            contentColor = Color(0xFF047857),
        )

        NotificationItemType.BudgetWarning -> ItemVisuals(
            icon = Icons.Rounded.WarningAmber,
            badgeLabel = "예산",
            containerColor = Color(0xFFFFEDD5),
            contentColor = Color(0xFFC2410C),
        )

        NotificationItemType.PartnerExpenseAdded -> ItemVisuals(
            icon = Icons.Rounded.Payments,
            badgeLabel = "활동",
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.primary,
        )

        NotificationItemType.PartnerScheduleAdded -> ItemVisuals(
            icon = Icons.Rounded.CalendarToday,
            badgeLabel = "활동",
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }
}

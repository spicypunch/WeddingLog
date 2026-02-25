package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GuestsScreen(
    state: GuestsUiState,
    onAction: (GuestsAction) -> Unit,
) {
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { onAction(GuestsAction.OnRefresh) },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pullRefresh(pullToRefreshState)
    ) {
        GuestsList(
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxSize()
        )

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        )

        FloatingActionButton(
            onClick = { onAction(GuestsAction.OnClickAdd) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "추가")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GuestsList(
    state: GuestsUiState,
    onAction: (GuestsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GuestsHeader(
                onClickMore = { onAction(GuestsAction.OnRefresh) },
            )
        }

        item {
            SummaryCards(
                totalGuestsText = state.totalGuestsText,
                expectedGiftTotalText = state.expectedGiftTotalText,
                expectedAttendanceText = state.expectedAttendanceText,
                expectedAttendingGiftText = state.expectedAttendingGiftText,
            )
        }

        item {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onAction(GuestsAction.OnSearchQueryChange(it)) }
            )
        }

        item {
            FilterRow(
                filters = state.filters,
                onSelect = { onAction(GuestsAction.OnSelectFilter(it)) }
            )
        }

        if (state.groupSummaries.isNotEmpty()) {
            item(key = "group-summary") {
                GroupSummaryRow(summaries = state.groupSummaries)
            }
        }

        if (state.isLoading) {
            item(key = "guests-loading") {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        state.message?.let { message ->
            item(key = "guests-message") {
                GuestsInlineMessage(
                    text = message,
                    onDismiss = { onAction(GuestsAction.OnDismissMessage) },
                )
            }
        }

        if (!state.isLoading && state.sections.isEmpty()) {
            item(key = "guests-empty") {
                GuestsEmptyState(
                    onRefresh = { onAction(GuestsAction.OnRefresh) },
                    onAdd = { onAction(GuestsAction.OnClickAdd) },
                )
            }
        }

        state.sections.forEach { section ->
            stickyHeader(key = "section-${section.title}") {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            items(section.items, key = { it.id }) { item ->
                GuestRow(
                    item = item,
                    onClick = { onAction(GuestsAction.OnClickGuest(item.id)) },
                    onClickStatus = {
                        onAction(
                            GuestsAction.OnQuickSetAttendance(
                                id = item.id,
                                value = item.status.next(),
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun GuestsHeader(
    onClickMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "하객 관리",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Surface(
            onClick = onClickMore,
            color = Color.Transparent,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "더보기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SummaryCards(
    totalGuestsText: String,
    expectedGiftTotalText: String,
    expectedAttendanceText: String,
    expectedAttendingGiftText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsCard(
                title = "총 하객",
                value = totalGuestsText,
                isPrimary = false,
                modifier = Modifier.weight(1f),
            )
            StatsCard(
                title = "예상 축의금",
                value = expectedGiftTotalText,
                isPrimary = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsCard(
                title = "참석 예상",
                value = expectedAttendanceText,
                isPrimary = false,
                modifier = Modifier.weight(1f),
            )
            StatsCard(
                title = "참석 기준 축의금",
                value = expectedAttendingGiftText,
                isPrimary = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    }
    val titleColor = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val valueColor = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GroupSummaryRow(
    summaries: List<GuestGroupSummary>,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(summaries, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.width(160.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.countText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = item.attendanceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.giftText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "이름 또는 태그 검색 (예: 김철수, 친구)",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterRow(
    filters: List<GuestsFilterChip>,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters, key = { it.id }) { chip ->
            val container = if (chip.selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface
            val content = if (chip.selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
            Surface(
                onClick = { onSelect(chip.id) },
                color = container,
                contentColor = content,
                shape = CircleShape,
                border = if (chip.selected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    chip.countText?.let { count ->
                        Surface(
                            color = if (chip.selected) MaterialTheme.colorScheme.background.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (chip.selected) content else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = count,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestRow(
    item: GuestListItem,
    onClick: () -> Unit,
    onClickStatus: () -> Unit,
) {
    val statusColor = when (item.status) {
        GuestAttendanceStatus.Attending -> Color(0xFF10B981)
        GuestAttendanceStatus.Undecided -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        GuestAttendanceStatus.NotAttending -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isDimmed) 0.6f else 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = item.avatarContainer,
                    contentColor = item.avatarContent,
                    shape = CircleShape,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.avatarText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TagChip(text = item.tagText)
                    }
                    item.note?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.giftAmountText ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.giftAmountText == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onClickStatus,
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.status.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestsEmptyState(
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "아직 등록된 하객이 없어요.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "첫 하객을 추가하거나 목록을 새로고침해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onRefresh) { Text(text = "새로고침") }
                TextButton(onClick = onAdd) { Text(text = "하객 추가") }
            }
        }
    }
}

@Composable
private fun GuestsInlineMessage(
    text: String,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.error,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.20f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(text = "닫기")
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun GuestAttendanceStatus.next(): GuestAttendanceStatus {
    return when (this) {
        GuestAttendanceStatus.Undecided -> GuestAttendanceStatus.Attending
        GuestAttendanceStatus.Attending -> GuestAttendanceStatus.NotAttending
        GuestAttendanceStatus.NotAttending -> GuestAttendanceStatus.Undecided
    }
}

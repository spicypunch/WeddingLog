package kr.jm.weddingledger.feature.guests.presentation

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.components.WeddingLogTopBar

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
        GuestsContent(
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
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
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "추가")
        }
    }
}

@Composable
private fun GuestsContent(
    state: GuestsUiState,
    onAction: (GuestsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            WeddingLogTopBar(
                onClickNotifications = { onAction(GuestsAction.OnClickNotifications) },
                horizontalPadding = 0.dp,
                topPadding = 0.dp,
                bottomPadding = 4.dp,
                titleContent = {
                    Text(
                        text = "하객",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        }

        item {
            GuestsTabSelector(
                selectedTab = state.selectedTab,
                onSelect = { onAction(GuestsAction.OnSelectTab(it)) },
            )
        }

        when (state.selectedTab) {
            GuestsTab.Expected -> expectedGuestsContent(state, onAction)
            GuestsTab.GiftLedger -> giftLedgerContent(state, onAction)
        }
    }
}

private fun LazyListScope.expectedGuestsContent(
    state: GuestsUiState,
    onAction: (GuestsAction) -> Unit,
) {
    val expected = state.expected

    item {
        ExpectedHeroCard(
            state = expected,
            onClickEditGuaranteedCount = { onAction(GuestsAction.OnClickEditGuaranteedCount) },
        )
    }

    if (expected.sideSummaries.isNotEmpty()) {
        item {
            SummaryRow(title = "측별 예상", items = expected.sideSummaries)
        }
    }

    if (expected.statusSummaries.isNotEmpty()) {
        item {
            ExpectedStatusRow(items = expected.statusSummaries)
        }
    }

    item {
        GuestsSearchBar(
            query = expected.searchQuery,
            placeholder = "이름, 메모로 검색",
            onQueryChange = { onAction(GuestsAction.OnExpectedSearchQueryChange(it)) },
        )
    }

    item {
        FilterSection(
            title = "구분",
            filters = expected.sideFilters,
            onSelect = { onAction(GuestsAction.OnSelectExpectedSideFilter(it)) },
        )
    }

    item {
        FilterSection(
            title = "상태",
            filters = expected.statusFilters,
            onSelect = { onAction(GuestsAction.OnSelectExpectedStatusFilter(it)) },
        )
    }

    guestsFeedback(
        isLoading = state.isLoading,
        message = state.message,
        isEmpty = expected.sections.isEmpty(),
        emptyText = expected.emptyText,
        onDismissMessage = { onAction(GuestsAction.OnDismissMessage) },
        onRefresh = { onAction(GuestsAction.OnRefresh) },
        onAdd = { onAction(GuestsAction.OnClickAdd) },
    )

    expected.sections.forEach { section ->
        item(key = "expected-title-${section.title}") {
            SectionTitle(section.title)
        }
        items(section.items, key = { it.id }) { item ->
            ExpectedGuestCard(
                item = item,
                onClick = { onAction(GuestsAction.OnClickExpectedGuest(item.id)) },
                onQuickSetStatus = {
                    onAction(
                        GuestsAction.OnQuickSetExpectedStatus(
                            id = item.id,
                            value = item.status.next(),
                        )
                    )
                },
            )
        }
    }
}

private fun LazyListScope.giftLedgerContent(
    state: GuestsUiState,
    onAction: (GuestsAction) -> Unit,
) {
    val ledger = state.giftLedger

    item {
        GiftLedgerHeroCard(state = ledger)
    }

    if (ledger.sideSummaries.isNotEmpty()) {
        item {
            SummaryRow(title = "측별 기록", items = ledger.sideSummaries)
        }
    }

    item {
        GuestsSearchBar(
            query = ledger.searchQuery,
            placeholder = "이름, 메모로 검색",
            onQueryChange = { onAction(GuestsAction.OnGiftSearchQueryChange(it)) },
        )
    }

    item {
        FilterSection(
            title = "구분",
            filters = ledger.sideFilters,
            onSelect = { onAction(GuestsAction.OnSelectGiftSideFilter(it)) },
        )
    }

    item {
        FilterSection(
            title = "상태",
            filters = ledger.statusFilters,
            onSelect = { onAction(GuestsAction.OnSelectGiftStatusFilter(it)) },
        )
    }

    guestsFeedback(
        isLoading = state.isLoading,
        message = state.message,
        isEmpty = ledger.sections.isEmpty(),
        emptyText = ledger.emptyText,
        onDismissMessage = { onAction(GuestsAction.OnDismissMessage) },
        onRefresh = { onAction(GuestsAction.OnRefresh) },
        onAdd = { onAction(GuestsAction.OnClickAdd) },
    )

    ledger.sections.forEach { section ->
        item(key = "gift-title-${section.title}") {
            SectionTitle(section.title)
        }
        items(section.items, key = { it.id }) { item ->
            GiftGuestCard(
                item = item,
                onClick = { onAction(GuestsAction.OnClickGiftGuest(item.id)) },
            )
        }
    }
}

private fun LazyListScope.guestsFeedback(
    isLoading: Boolean,
    message: String?,
    isEmpty: Boolean,
    emptyText: String,
    onDismissMessage: () -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
) {
    if (isLoading) {
        item("guests-loading") {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }

    message?.let {
        item("guests-message") {
            MessageCard(
                text = it,
                onDismiss = onDismissMessage,
            )
        }
    }

    if (!isLoading && isEmpty) {
        item("guests-empty") {
            EmptyStateCard(
                text = emptyText,
                onRefresh = onRefresh,
                onAdd = onAdd,
            )
        }
    }
}

@Composable
private fun GuestsTabSelector(
    selectedTab: GuestsTab,
    onSelect: (GuestsTab) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuestsTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Surface(
                    onClick = { onSelect(tab) },
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpectedHeroCard(
    state: ExpectedGuestsUiState,
    onClickEditGuaranteedCount: () -> Unit,
) {
    val toneColor = toneColor(state.guaranteedDiffTone)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "예상 총 하객",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.totalExpectedCountText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "보증인원 ${state.guaranteedGuestCountText}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.guaranteedDiffText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = toneColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                TextButton(onClick = onClickEditGuaranteedCount) {
                    Text(text = "보증인원 수정")
                }
            }
        }
    }
}

@Composable
private fun GiftLedgerHeroCard(
    state: GiftLedgerUiState,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "총 축의금",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.totalGiftText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniMetricCard(
                    title = "실제 참석",
                    value = state.actualAttendingCountText,
                    modifier = Modifier.weight(1f),
                )
                MiniMetricCard(
                    title = "입력 완료",
                    value = state.completedEntryCountText,
                    modifier = Modifier.weight(1f),
                )
                MiniMetricCard(
                    title = "미입력",
                    value = state.pendingEntryCountText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    title: String,
    items: List<GuestsSideSummaryItem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.primaryText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = item.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpectedStatusRow(
    items: List<ExpectedStatusSummaryItem>,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items) { item ->
            val color = toneColor(item.tone)
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.valueText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestsSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        placeholder = { Text(text = placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FilterSection(
    title: String,
    filters: List<GuestsFilterChip>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters, key = { it.id }) { filter ->
                FilterChip(
                    selected = filter.selected,
                    onClick = { onSelect(filter.id) },
                    label = {
                        Text(
                            text = buildString {
                                append(filter.label)
                                filter.countText?.takeIf { it.isNotBlank() }?.let {
                                    append(" ")
                                    append(it)
                                }
                            }
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpectedGuestCard(
    item: ExpectedGuestItem,
    onClick: () -> Unit,
    onQuickSetStatus: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.alpha(if (item.isDimmed) 0.72f else 1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AvatarBubble(
                text = item.avatarText,
                container = item.avatarContainer,
                content = item.avatarContent,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${item.sideLabel} · ${item.relationshipLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.companionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusBadge(
                    text = item.statusText,
                    tone = when (item.status) {
                        GuestExpectedStatus.Certain -> SummaryTone.Primary
                        GuestExpectedStatus.Maybe -> SummaryTone.Warning
                        GuestExpectedStatus.NotAttending -> SummaryTone.Neutral
                    },
                    onClick = onQuickSetStatus,
                )
                Text(
                    text = item.expectedHeadcountText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun GiftGuestCard(
    item: GiftGuestItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.alpha(if (item.isDimmed) 0.72f else 1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AvatarBubble(
                text = item.avatarText,
                container = item.avatarContainer,
                content = item.avatarContent,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${item.sideLabel} · ${item.relationshipLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.attendanceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AmountBadge(
                text = item.amountText,
                pending = item.isPending,
            )
        }
    }
}

@Composable
private fun AvatarBubble(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    tone: SummaryTone,
    onClick: () -> Unit,
) {
    val color = toneColor(tone)
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AmountBadge(
    text: String,
    pending: Boolean,
) {
    val container = if (pending) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    val content = if (pending) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MessageCard(
    text: String,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(text = "닫기")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    text: String,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "하객을 추가하거나 새로고침해서 최신 상태를 확인해보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRefresh) {
                    Text(text = "새로고침")
                }
                TextButton(onClick = onAdd) {
                    Text(text = "하객 추가")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private fun GuestExpectedStatus.next(): GuestExpectedStatus {
    return when (this) {
        GuestExpectedStatus.Certain -> GuestExpectedStatus.Maybe
        GuestExpectedStatus.Maybe -> GuestExpectedStatus.NotAttending
        GuestExpectedStatus.NotAttending -> GuestExpectedStatus.Certain
    }
}

@Composable
private fun toneColor(tone: SummaryTone): Color {
    return when (tone) {
        SummaryTone.Primary -> MaterialTheme.colorScheme.primary
        SummaryTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        SummaryTone.Positive -> Color(0xFF059669)
        SummaryTone.Warning -> Color(0xFFF59E0B)
    }
}

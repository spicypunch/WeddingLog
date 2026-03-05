package kr.jm.weddingledger.feature.schedule.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
) {
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { onAction(ScheduleAction.OnRefresh) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScheduleHeader(
            title = "WeddingLog",
            onClickNotifications = { onAction(ScheduleAction.OnClickNotifications) },
        )

        PrimaryTabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            ScheduleTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = state.selectedTab.ordinal == index,
                    onClick = { onAction(ScheduleAction.OnSelectTab(tab)) },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .pullRefresh(pullToRefreshState)
        ) {
            when (state.selectedTab) {
                ScheduleTab.Schedule -> CalendarTab(
                    state = state.calendar,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize()
                )
                ScheduleTab.Checklist -> ChecklistTab(
                    state = state.checklist,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize()
                )
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            )

            FloatingActionButton(
                onClick = { onAction(ScheduleAction.OnClickAdd) },
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
}

@Composable
private fun ScheduleHeader(
    title: String,
    onClickNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
            onClick = onClickNotifications,
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = "알림",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarTab(
    state: CalendarUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            CalendarMonthHeader(
                monthLabel = state.monthLabel,
                onPrevMonth = { onAction(ScheduleAction.OnClickPrevMonth) },
                onNextMonth = { onAction(ScheduleAction.OnClickNextMonth) },
            )
        }

        item {
            DayOfWeekRow(labels = state.dayOfWeekLabels)
        }

        item {
            CalendarGrid(
                cells = state.cells,
                onSelectDay = { onAction(ScheduleAction.OnSelectCalendarDay(it)) }
            )
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        }

        state.message?.let { message ->
            item(key = "calendar-message") {
                InlineMessage(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "calendar-search") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(ScheduleAction.OnCalendarQueryChange(it)) },
                placeholder = { Text("일정/장소/메모 검색") },
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item(key = "calendar-sort") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UpcomingSortType.entries) { option ->
                    FilterChip(
                        selected = state.sortType == option,
                        onClick = { onAction(ScheduleAction.OnSelectUpcomingSort(option)) },
                        label = { Text(text = option.label) },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.upcomingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (state.selectedDayId != null) {
                    TextButton(
                        onClick = { onAction(ScheduleAction.OnClickViewAllUpcoming) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = "전체보기",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.upcoming.isEmpty()) {
            item(key = "upcoming-empty") {
                EmptyUpcomingMessage(
                    text = state.upcomingEmptyText,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(state.upcoming, key = { it.id }) { item ->
            UpcomingScheduleCard(
                item = item,
                onClick = { onAction(ScheduleAction.OnClickUpcomingSchedule(item.id)) },
                onClickMore = if (item.source == UpcomingSource.Event) {
                    { onAction(ScheduleAction.OnClickUpcomingMore(item.id)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthLabel: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onPrevMonth,
            color = Color.Transparent,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "이전 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp)
            )
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            onClick = onNextMonth,
            color = Color.Transparent,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "다음 달",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

@Composable
private fun DayOfWeekRow(labels: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    cells: List<CalendarDayCell>,
    onSelectDay: (String) -> Unit,
) {
    val weeks = cells.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val cell = week.getOrNull(i)
                    if (cell == null) {
                        Spacer(modifier = Modifier.weight(1f).height(58.dp))
                    } else {
                        CalendarDayCellView(
                            cell = cell,
                            onClick = { if (cell.inMonth && !cell.isDDay) onSelectDay(cell.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCellView(
    cell: CalendarDayCell,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundTone = when {
        cell.isDDay -> CalendarDayBackgroundTone.SolidPrimary
        cell.isSelected -> CalendarDayBackgroundTone.SoftPrimary
        else -> cell.backgroundTone
    }

    val textTone = when {
        cell.isDDay -> CalendarDayTextTone.OnPrimary
        !cell.inMonth -> CalendarDayTextTone.Muted
        cell.isSelected -> CalendarDayTextTone.Primary
        else -> cell.textTone
    }

    val circleColor = when (backgroundTone) {
        CalendarDayBackgroundTone.None -> Color.Transparent
        CalendarDayBackgroundTone.SoftPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        CalendarDayBackgroundTone.SolidPrimary -> MaterialTheme.colorScheme.primary
    }

    val numberColor = when (textTone) {
        CalendarDayTextTone.Normal -> MaterialTheme.colorScheme.onBackground
        CalendarDayTextTone.Primary -> MaterialTheme.colorScheme.primary
        CalendarDayTextTone.Muted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        CalendarDayTextTone.OnPrimary -> MaterialTheme.colorScheme.onPrimary
    }

    val fontWeight = when {
        cell.isDDay -> FontWeight.Bold
        cell.isSelected -> FontWeight.SemiBold
        cell.dotStyle != null -> FontWeight.SemiBold
        cell.textTone == CalendarDayTextTone.Primary -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }

    val dotColor = when (cell.dotStyle) {
        CalendarDotStyle.Primary -> MaterialTheme.colorScheme.primary
        CalendarDotStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
        null -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.height(58.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Surface(
                color = circleColor,
                contentColor = numberColor,
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cell.day.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = fontWeight,
                        color = numberColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (cell.dotStyle != null && !cell.isDDay) {
                when {
                    cell.markerCount <= 1 -> {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(dotColor, CircleShape)
                        )
                    }

                    cell.markerCount == 2 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(dotColor, CircleShape)
                                )
                            }
                        }
                    }

                    else -> {
                        Surface(
                            color = dotColor.copy(alpha = 0.12f),
                            contentColor = dotColor,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = "${cell.markerCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            cell.dDayLabel?.let { label ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UpcomingScheduleCard(
    item: UpcomingScheduleItem,
    onClick: () -> Unit,
    onClickMore: (() -> Unit)?,
) {
    val alpha = if (item.isCompleted) 0.60f else 1f
    val titleDecoration = if (item.isCompleted) TextDecoration.LineThrough else null

    val tagContainer = when (item.tagTone) {
        UpcomingTagTone.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        UpcomingTagTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tagContent = when (item.tagTone) {
        UpcomingTagTone.Primary -> MaterialTheme.colorScheme.primary
        UpcomingTagTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier.alpha(alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .width(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.monthAbbr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.dayOfMonth,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = titleDecoration,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onClickMore != null) {
                    Surface(
                        onClick = onClickMore,
                        color = Color.Transparent,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "일정 더보기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Surface(
                    color = tagContainer,
                    contentColor = tagContent,
                    shape = CircleShape,
                ) {
                    Text(
                        text = item.dTagText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tagContent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistTab(
    state: ChecklistUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ChecklistProgressCard(
                percentText = state.percentText,
                countText = state.countText,
                progressFraction = state.progressFraction
            )
        }

        if (state.isLoading) {
            item(key = "checklist-loading") {
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
            item(key = "checklist-message") {
                InlineMessage(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.groups.forEach { group ->
            stickyHeader(key = "header-${group.id}") {
                ChecklistGroupHeader(
                    group = group,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 10.dp)
                )
            }

            items(group.items, key = { it.id }) { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { onAction(ScheduleAction.OnToggleChecklistItem(item.id)) },
                    onClickMore = { onAction(ScheduleAction.OnClickChecklistItemMore(item.id)) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistProgressCard(
    percentText: String,
    countText: String,
    progressFraction: Float,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "나의 웨딩 준비",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "전체 ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = percentText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = " 완료",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun ChecklistGroupHeader(
    group: ChecklistGroup,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DTagChip(
                text = group.dTag,
                style = group.tagStyle
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = group.progressText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DTagChip(
    text: String,
    style: DTagStyle,
) {
    val container = when (style) {
        DTagStyle.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        DTagStyle.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (style) {
        DTagStyle.Primary -> MaterialTheme.colorScheme.primary
        DTagStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onClickMore: () -> Unit,
) {
    val isDone = item.isDone
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
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
                ChecklistCheckbox(
                    checked = isDone,
                    onToggle = onToggle,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (isDone) 0.55f else 1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    )
                    item.subtitle?.let { subtitle ->
                        val subtitleColor = when (item.subtitleTone) {
                            ChecklistItemSubtitleTone.Muted -> MaterialTheme.colorScheme.onSurfaceVariant
                            ChecklistItemSubtitleTone.Primary -> MaterialTheme.colorScheme.primary
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor
                        )
                    }
                }
            }

            Surface(
                onClick = onClickMore,
                color = Color.Transparent,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "더보기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ChecklistCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    val borderColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val container = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    val checkTint = if (checked) MaterialTheme.colorScheme.onPrimary else Color.Transparent

    Surface(
        onClick = onToggle,
        color = Color.Transparent,
        shape = shape,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(width = 2.dp, color = borderColor, shape = shape)
                .background(container, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun InlineMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.error,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.20f),
        ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun EmptyUpcomingMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
        )
    }
}

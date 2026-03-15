package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.components.WeddingLogTopBar

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MoneyScreen(
    state: MoneyUiState,
    onAction: (MoneyAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WeddingLogTopBar(
            onClickNotifications = { onAction(MoneyAction.OnClickNotifications) },
            titleContent = {
                Text(
                    text = "예산과 지출",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
            )
        }

        val pullToRefreshState = rememberPullRefreshState(
            refreshing = state.isRefreshing,
            onRefresh = { onAction(MoneyAction.OnRefresh) },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .pullRefresh(pullToRefreshState)
        ) {
            MoneyContent(
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
                onClick = { onAction(MoneyAction.OnClickAdd) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "추가",
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoneyContent(
    state: MoneyUiState,
    onAction: (MoneyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredExpenseCount = state.expenses.sections.sumOf { it.items.size }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BudgetSummaryCard(state = state.budget)
        }

        if (state.budget.insightItems.isNotEmpty()) {
            item {
                BudgetInsightsRow(items = state.budget.insightItems)
            }
        }

        if (state.budget.payerSummaries.isNotEmpty()) {
            item {
                PayerSummaryCard(items = state.budget.payerSummaries)
            }
        }

        if (state.budget.paymentStageSummaries.isNotEmpty()) {
            item {
                PaymentStageSection(items = state.budget.paymentStageSummaries)
            }
        }

        if (state.budget.pendingPayments.isNotEmpty()) {
            item {
                PendingPaymentsSection(
                    items = state.budget.pendingPayments,
                    onClickBudgetItem = { onAction(MoneyAction.OnClickBudgetItem(it)) },
                )
            }
        }

        item {
            ExpenseOverviewCard(
                state = state.expenses,
                filteredExpenseCount = filteredExpenseCount,
            )
        }

        item {
            ExpenseFilterPanel(
                state = state.expenses,
                onAction = onAction,
            )
        }

        item {
            SectionHeader(
                title = "카테고리별 예산과 지출",
                subtitle = "예산을 먼저 잡고, 같은 화면에서 실제 지출 흐름을 함께 보세요.",
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onAction(MoneyAction.OnClickManageCategories) }) {
                            Text(text = "카테고리 관리")
                        }
                        TextButton(onClick = { onAction(MoneyAction.OnToggleBudgetFilter) }) {
                            Text(text = if (state.budget.isOverBudgetOnly) "초과만 보기" else "전체 보기")
                        }
                    }
                }
            )
        }

        if (state.budget.categories.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "예산 카테고리가 없어요",
                    body = "카테고리를 만든 뒤 예산 항목을 추가하면 지출 흐름을 한눈에 볼 수 있어요.",
                )
            }
        } else {
            items(state.budget.categories, key = { it.id }) { category ->
                BudgetCategoryCard(
                    item = category,
                    onToggle = { onAction(MoneyAction.OnToggleBudgetCategory(category.id)) },
                    onClickAddItem = { onAction(MoneyAction.OnClickBudgetAddItem(category.id)) },
                    onClickBudgetItem = { onAction(MoneyAction.OnClickBudgetItem(it)) },
                    onClickExpense = { onAction(MoneyAction.OnClickExpense(it)) },
                )
            }
        }

        item {
            SectionHeader(
                title = "전체 지출 기록",
                subtitle = if (filteredExpenseCount == 0) {
                    "현재 필터 조건에 맞는 지출이 없어요."
                } else {
                    "필터 결과 ${filteredExpenseCount}건"
                }
            )
        }

        if (state.expenses.sections.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "표시할 지출이 없어요",
                    body = "필터를 바꾸거나 새 지출을 추가해보세요.",
                )
            }
        } else {
            state.expenses.sections.forEach { section ->
                stickyHeader(key = "expense-header-${section.title}") {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 4.dp, vertical = 10.dp)
                    )
                }
                items(section.items, key = { it.id }) { item ->
                    ExpenseRow(
                        item = item,
                        onClick = { onAction(MoneyAction.OnClickExpense(item.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseOverviewCard(
    state: ExpensesUiState,
    filteredExpenseCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = state.monthTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "₩",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, end = 2.dp),
                    )
                    Text(
                        text = state.totalSpentKrw,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OverviewMeta(label = "필터 결과", value = "${filteredExpenseCount}건")
                OverviewMeta(label = "저장된 필터", value = "${state.presets.size}개")
                OverviewMeta(label = "연결된 예산 항목", value = "${state.budgetItemOptions.size}개")
            }
        }
    }
}

@Composable
private fun OverviewMeta(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ExpenseFilterPanel(
    state: ExpensesUiState,
    onAction: (MoneyAction) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FilterSection(
                title = "카테고리",
                chips = {
                    items(state.categoryFilters, key = { it.id ?: "all" }) { chip ->
                        ExpenseFilterChip(
                            label = chip.label,
                            selected = chip.selected,
                            onClick = { onAction(MoneyAction.OnSelectExpenseFilter(chip.id)) },
                        )
                    }
                }
            )
            FilterSection(
                title = "기간",
                chips = {
                    items(state.periodFilters, key = { it.option.name }) { chip ->
                        ExpenseFilterChip(
                            label = chip.option.label,
                            selected = chip.selected,
                            onClick = { onAction(MoneyAction.OnSelectExpensePeriodFilter(chip.option)) },
                        )
                    }
                }
            )
            FilterSection(
                title = "상태",
                chips = {
                    items(state.statusFilters, key = { it.option.name }) { chip ->
                        ExpenseFilterChip(
                            label = chip.option.label,
                            selected = chip.selected,
                            onClick = { onAction(MoneyAction.OnSelectExpenseStatusFilter(chip.option)) },
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterSectionLabel(text = "저장된 필터")
                    TextButton(onClick = { onAction(MoneyAction.OnSaveExpenseFilterPreset) }) {
                        Text(text = "현재 조합 저장")
                    }
                }

                if (state.presets.isEmpty()) {
                    Text(
                        text = "자주 쓰는 필터 조합을 저장해두면 결산 확인이 빨라져요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.presets, key = { it.id }) { chip ->
                            ExpenseFilterChip(
                                label = chip.label,
                                selected = chip.selected,
                                onClick = { onAction(MoneyAction.OnSelectExpenseFilterPreset(chip.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    chips: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterSectionLabel(
            text = title,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            chips()
        }
    }
}

@Composable
private fun FilterSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun ExpenseFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            trailing?.invoke()
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpenseRow(
    item: ExpenseListItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(item.accentContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accent,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val subtitle = item.vendorName
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { "${item.dateText} · $it" }
                        ?: item.dateText
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = item.amountText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(state: BudgetUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "총 예산",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = state.totalBudgetText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = state.usedPercentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "사용 금액",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.usedAmountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.usedProgress.coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "잔액",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.remainingAmountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.remainingAmountText.contains("초과")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (state.remainingAmountText.contains("초과")) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun BudgetInsightsRow(
    items: List<BudgetInsightItem>,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.label }) { item ->
            val containerColor = when (item.emphasis) {
                BudgetInsightEmphasis.Neutral -> MaterialTheme.colorScheme.surface
                BudgetInsightEmphasis.Highlight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                BudgetInsightEmphasis.Caution -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
            }
            val contentColor = when (item.emphasis) {
                BudgetInsightEmphasis.Neutral -> MaterialTheme.colorScheme.onSurface
                BudgetInsightEmphasis.Highlight -> MaterialTheme.colorScheme.primary
                BudgetInsightEmphasis.Caution -> MaterialTheme.colorScheme.error
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.width(150.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PayerSummaryCard(
    items: List<PayerSummaryItem>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "결제 분담",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${item.amountText} · ${item.ratioText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentStageSection(
    items: List<PaymentStageSummaryItem>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "지출 단계 현황",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (item.isPriority) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        text = "확인 필요",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.countText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.paidText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.remainingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (item.isPriority) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingPaymentsSection(
    items: List<PendingPaymentItem>,
    onClickBudgetItem: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "남은 결제 확인",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            items.take(5).forEach { item ->
                Surface(
                    onClick = { onClickBudgetItem(item.id) },
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${item.categoryName} · ${item.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            text = item.stageLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    item.priorityText?.let { priority ->
                                        Text(
                                            text = priority,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                item.dueDateText?.let { dueDateText ->
                                    Text(
                                        text = dueDateText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = item.amountText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = item.paidText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = item.targetText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryCard(
    item: BudgetCategoryItem,
    onToggle: () -> Unit,
    onClickAddItem: () -> Unit,
    onClickBudgetItem: (String) -> Unit,
    onClickExpense: (String) -> Unit,
) {
    val borderColor = if (item.isExpanded) {
        item.accent.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = Color.Transparent,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(item.accentContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.accent,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${item.itemCountText} · ${item.expenseCountText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = item.usedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isOverBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (item.isOverBudget) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = item.targetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        color = item.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.remainingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isOverBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (item.isOverBudget) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }

            if (item.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (item.subItems.isEmpty()) {
                        EmptyMiniState(
                            text = "아직 예산 항목이 없어요. 계약금, 잔금 같은 항목부터 추가해보세요.",
                        )
                    } else {
                        item.subItems.forEach { subItem ->
                            BudgetSubItemCard(
                                item = subItem,
                                onClick = { onClickBudgetItem(subItem.id) },
                            )
                        }
                    }

                    Text(
                        text = "최근 지출",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (item.recentExpenses.isEmpty()) {
                        EmptyMiniState(text = "이 카테고리에는 아직 기록된 지출이 없어요.")
                    } else {
                        item.recentExpenses.forEach { expense ->
                            ExpenseRow(
                                item = expense,
                                onClick = { onClickExpense(expense.id) },
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onClickAddItem,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "예산 항목 추가")
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSubItemCard(
    item: BudgetSubItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = item.stageLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = item.expenseCountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        item.dueDateText?.let { dueDateText ->
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Text(
                    text = item.valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isEmpty) FontWeight.Medium else FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.spentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.targetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress = { item.progress.coerceIn(0f, 1f) },
                color = if (item.isOverBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
            )

            Text(
                text = item.remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isOverBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (item.isOverBudget) FontWeight.SemiBold else FontWeight.Medium,
            )

            item.memo
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { memo ->
                    Text(
                        text = memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    body: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyMiniState(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
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
        border = BorderStroke(
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

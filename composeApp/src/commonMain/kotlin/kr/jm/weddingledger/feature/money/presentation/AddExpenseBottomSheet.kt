package kr.jm.weddingledger.feature.money.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHandle
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHeader
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    title: String,
    state: AddExpenseUiState,
    showDelete: Boolean,
    onDismiss: () -> Unit,
    onAction: (AddExpenseAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { WeddingLogSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            WeddingLogSheetHeader(
                title = title,
                onClose = { onAction(AddExpenseAction.OnClickClose) },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                AmountSection(
                    amount = state.amount,
                    onAmountChange = { onAction(AddExpenseAction.OnAmountChange(it)) }
                )

                DateRow(
                    dateText = state.dateText,
                    onClick = { onAction(AddExpenseAction.OnClickDate) }
                )

                ExpenseCategorySelector(
                    options = state.categoryOptions,
                    selectedCategoryId = state.selectedCategoryId,
                    onSelect = { onAction(AddExpenseAction.OnSelectCategory(it)) }
                )

                BudgetItemSelector(
                    selected = state.filteredBudgetItemOptions.firstOrNull { it.id == state.selectedBudgetItemId }?.label,
                    options = state.filteredBudgetItemOptions,
                    onSelect = { onAction(AddExpenseAction.OnSelectBudgetItem(it)) }
                )

                VendorNameSection(
                    vendorName = state.vendorName,
                    onVendorNameChange = { onAction(AddExpenseAction.OnVendorNameChange(it)) }
                )

                PaymentMethodSection(
                    selected = state.paymentMethod,
                    onSelect = { onAction(AddExpenseAction.OnSelectPaymentMethod(it)) },
                )

                if (state.paymentMethod != PaymentMethod.JointAccount) {
                    SegmentedThreeOption(
                        label = "결제자",
                        first = Payer.Groom.label,
                        second = Payer.Bride.label,
                        third = Payer.Joint.label,
                        firstSelected = state.payer == Payer.Groom,
                        secondSelected = state.payer == Payer.Bride,
                        thirdSelected = state.payer == Payer.Joint,
                        onSelectFirst = { onAction(AddExpenseAction.OnSelectPayer(Payer.Groom)) },
                        onSelectSecond = { onAction(AddExpenseAction.OnSelectPayer(Payer.Bride)) },
                        onSelectThird = { onAction(AddExpenseAction.OnSelectPayer(Payer.Joint)) },
                    )
                }

                MemoSection(
                    memo = state.memo,
                    onMemoChange = { onAction(AddExpenseAction.OnMemoChange(it)) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
            ) {
                if (showDelete) {
                    OutlinedButton(
                        onClick = { onAction(AddExpenseAction.OnClickDelete) },
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "삭제하기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { onAction(AddExpenseAction.OnClickSave) },
                    enabled = state.isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "저장하기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun AmountSection(
    amount: String,
    onAmountChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "금액",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₩",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = amount,
                onValueChange = onAmountChange,
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Bold,
                    )
                },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DateRow(
    dateText: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "날짜",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseCategorySelector(
    options: List<CategoryOption>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "카테고리",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = options, key = { it.id }) { option ->
                val selected = option.id == selectedCategoryId
                Surface(
                    onClick = { onSelect(option.id) },
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape,
                    shadowElevation = if (selected) 2.dp else 0.dp,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetItemSelector(
    selected: String?,
    options: List<BudgetItemOption>,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "예산 항목 (선택)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { if (options.isNotEmpty()) expanded = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when {
                            selected != null -> selected
                            options.isEmpty() -> "선택 가능한 예산 항목이 없어요"
                            else -> "선택 안 함"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text(text = "선택 안 함") },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label) },
                        onClick = {
                            expanded = false
                            onSelect(option.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorNameSection(
    vendorName: String,
    onVendorNameChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "업체명 (선택)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        TextField(
            value = vendorName,
            onValueChange = onVendorNameChange,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SegmentedThreeOption(
    label: String,
    first: String,
    second: String,
    third: String,
    firstSelected: Boolean,
    secondSelected: Boolean,
    thirdSelected: Boolean,
    onSelectFirst: () -> Unit,
    onSelectSecond: () -> Unit,
    onSelectThird: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                SegmentButton(
                    text = first,
                    selected = firstSelected,
                    onClick = onSelectFirst,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SegmentButton(
                    text = second,
                    selected = secondSelected,
                    onClick = onSelectSecond,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SegmentButton(
                    text = third,
                    selected = thirdSelected,
                    onClick = onSelectThird,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodSection(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "결제 수단",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                SegmentButton(
                    text = PaymentMethod.Card.label,
                    selected = selected == PaymentMethod.Card,
                    onClick = { onSelect(PaymentMethod.Card) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                SegmentButton(
                    text = PaymentMethod.Cash.label,
                    selected = selected == PaymentMethod.Cash,
                    onClick = { onSelect(PaymentMethod.Cash) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                SegmentButton(
                    text = PaymentMethod.JointAccount.label,
                    selected = selected == PaymentMethod.JointAccount,
                    onClick = { onSelect(PaymentMethod.JointAccount) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.small,
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MemoSection(
    memo: String,
    onMemoChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "메모",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        TextField(
            value = memo,
            onValueChange = onMemoChange,
            placeholder = { Text(text = "내용을 입력하세요 (선택)") },
            minLines = 3,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package kr.jm.weddingledger.feature.guests.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHandle
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGuestBottomSheet(
    state: AddGuestUiState,
    onDismiss: () -> Unit,
    onAction: (AddGuestAction) -> Unit,
    sheetTitle: String = state.mode.title,
    saveButtonText: String = state.mode.saveLabel,
    onClickDelete: (() -> Unit)? = null,
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
                title = sheetTitle,
                onClose = { onAction(AddGuestAction.OnClickClose) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                EditorTextField(
                    label = "이름",
                    value = state.name,
                    onValueChange = { onAction(AddGuestAction.OnNameChange(it)) },
                    placeholder = "이름을 입력하세요",
                )

                SideSection(
                    selected = state.side,
                    onSelect = { onAction(AddGuestAction.OnSelectSide(it)) }
                )

                RelationshipSection(
                    selected = state.relationship,
                    onSelect = { onAction(AddGuestAction.OnSelectRelationship(it)) }
                )

                if (state.mode == GuestEditorMode.Expected) {
                    StatusSection(
                        title = "참석 가능성",
                        helperText = "안 옴은 0명, 애매는 0.5명, 꼭 옴은 1명으로 계산돼요.",
                        options = GuestExpectedStatus.entries.map { it.label to (state.expectedStatus == it) },
                        onSelect = { index -> onAction(AddGuestAction.OnSelectExpectedStatus(GuestExpectedStatus.entries[index])) },
                    )

                    EditorTextField(
                        label = "동반 인원",
                        value = state.companionCount,
                        onValueChange = { onAction(AddGuestAction.OnCompanionCountChange(it.filter(Char::isDigit))) },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                        helperText = "본인을 제외하고 함께 오는 인원을 입력하세요.",
                    )
                } else {
                    StatusSection(
                        title = "실제 참석",
                        helperText = "결혼식이 끝난 뒤 참석 여부와 축의금을 기록해두면 나중에 바로 참고할 수 있어요.",
                        options = GiftAttendanceState.entries.map { it.label to (state.actualAttendance == it) },
                        onSelect = { index -> onAction(AddGuestAction.OnSelectActualAttendance(GiftAttendanceState.entries[index])) },
                    )

                    EditorTextField(
                        label = "실제 축의금",
                        value = state.actualGiftAmount,
                        onValueChange = { onAction(AddGuestAction.OnActualGiftAmountChange(it.filter { c -> c.isDigit() || c == ',' })) },
                        placeholder = "50,000",
                        keyboardType = KeyboardType.Number,
                        helperText = "금액을 입력하지 않으면 축의대에서 미입력 상태로 남아요.",
                    )
                }

                EditorTextField(
                    label = "메모",
                    value = state.note,
                    onValueChange = { onAction(AddGuestAction.OnNoteChange(it)) },
                    placeholder = if (state.mode == GuestEditorMode.Expected) "예: 신랑 직장 동료, 식권 2장" else "예: 현장 수기봉투, 부모님 지인",
                    singleLine = false,
                    minLines = 3,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
            ) {
                Button(
                    onClick = { onAction(AddGuestAction.OnClickSave) },
                    enabled = state.isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    androidx.compose.material3.Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = saveButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (onClickDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onClickDelete,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "하객 삭제",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    helperText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder) },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        helperText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SideSection(
    selected: GuestSide,
    onSelect: (GuestSide) -> Unit,
) {
    SelectionRow(
        label = "구분",
        options = GuestSide.entries.map { it.label to (it == selected) },
        onSelect = { index -> onSelect(GuestSide.entries[index]) },
    )
}

@Composable
private fun RelationshipSection(
    selected: GuestRelationshipLevel,
    onSelect: (GuestRelationshipLevel) -> Unit,
) {
    SelectionRow(
        label = "관계",
        options = GuestRelationshipLevel.entries.map { it.label to (it == selected) },
        onSelect = { index -> onSelect(GuestRelationshipLevel.entries[index]) },
    )
}

@Composable
private fun StatusSection(
    title: String,
    helperText: String,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        SelectionRow(
            label = null,
            options = options,
            onSelect = onSelect,
        )
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectionRow(
    label: String?,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, (text, selected) ->
                Surface(
                    onClick = { onSelect(index) },
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

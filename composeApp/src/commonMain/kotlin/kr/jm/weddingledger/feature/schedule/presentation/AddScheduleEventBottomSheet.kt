package kr.jm.weddingledger.feature.schedule.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHandle
import kr.jm.weddingledger.core.ui.components.WeddingLogSheetHeader
import kr.jm.weddingledger.feature.schedule.domain.model.EventRepeatRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleEventBottomSheet(
    state: AddScheduleEventUiState,
    onDismiss: () -> Unit,
    onAction: (AddScheduleEventAction) -> Unit,
    sheetTitle: String = "새 일정",
    saveButtonText: String = "일정 저장",
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
                .fillMaxHeight(0.85f)
        ) {
            WeddingLogSheetHeader(
                title = sheetTitle,
                onClose = { onAction(AddScheduleEventAction.OnClickClose) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onAction(AddScheduleEventAction.OnTitleChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("일정 제목") },
                    placeholder = { Text("예: 웨딩홀 미팅") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.EventAvailable, contentDescription = null)
                    },
                )

                DateField(
                    value = state.eventDateText,
                    onClick = { onAction(AddScheduleEventAction.OnClickEventDate) },
                )

                OutlinedTextField(
                    value = state.startTime,
                    onValueChange = { onAction(AddScheduleEventAction.OnStartTimeChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("시간 (선택)") },
                    placeholder = { Text("예: 14:30") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.startTimeErrorMessage != null,
                    supportingText = {
                        val error = state.startTimeErrorMessage
                        if (error != null) {
                            Text(text = error)
                        } else {
                            Text(text = "24시간 형식 (HH:mm)")
                        }
                    },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Schedule, contentDescription = null) },
                )

                OutlinedTextField(
                    value = state.location,
                    onValueChange = { onAction(AddScheduleEventAction.OnLocationChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("장소 (선택)") },
                    placeholder = { Text("예: 강남구 ○○웨딩홀") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    leadingIcon = { Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null) },
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "반복",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EventRepeatRule.entries) { rule ->
                            FilterChip(
                                selected = state.repeatRule == rule,
                                onClick = { onAction(AddScheduleEventAction.OnSelectRepeatRule(rule)) },
                                label = {
                                    Text(
                                        text = rule.toLabel(),
                                    )
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.memo,
                    onValueChange = { onAction(AddScheduleEventAction.OnMemoChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("메모 (선택)") },
                    placeholder = { Text("준비물, 담당자 연락처 등") },
                    minLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
            ) {
                Button(
                    onClick = { onAction(AddScheduleEventAction.OnClickSave) },
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
                    Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = saveButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onClickDelete != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onClickDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                        ),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "일정 삭제",
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

private fun EventRepeatRule.toLabel(): String {
    return when (this) {
        EventRepeatRule.None -> "반복 없음"
        EventRepeatRule.Weekly -> "매주"
        EventRepeatRule.Monthly -> "매월"
    }
}

@Composable
private fun DateField(
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "날짜",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

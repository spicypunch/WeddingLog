package kr.jm.weddingledger.feature.tools.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ToolsRoute(
    onOpenVenueComparison: () -> Unit,
    onOpenValuesSurvey: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun showPreparingMessage() {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("준비 중인 기능이에요.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        ToolsScreen(
            modifier = Modifier.padding(innerPadding),
            onOpenVenueComparison = {
                showPreparingMessage()
            },
            onOpenValuesSurvey = {
                showPreparingMessage()
            },
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun ToolsScreen(
    modifier: Modifier = Modifier,
    onOpenVenueComparison: () -> Unit,
    onOpenValuesSurvey: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "도구",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        item {
            ToolsCard(
                title = "웨딩홀 비교",
                subtitle = "홀 비용과 평가를 한 번에 비교",
                icon = Icons.Rounded.Compare,
                onClick = onOpenVenueComparison
            )
        }
        item {
            ToolsCard(
                title = "가치관 설문",
                subtitle = "Q&A로 서로의 생각을 확인",
                icon = Icons.Rounded.AutoAwesome,
                onClick = onOpenValuesSurvey
            )
        }
        item {
            ToolsCard(
                title = "설정",
                subtitle = "테마, 계정, 초대코드",
                icon = Icons.Rounded.Settings,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun ToolsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

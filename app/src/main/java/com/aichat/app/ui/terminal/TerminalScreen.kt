package com.aichat.app.ui.terminal

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.data.terminal.TerminalLogBuffer
import com.aichat.app.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val filterLevel by viewModel.filterLevel.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val expandedEntryId by viewModel.expandedEntryId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var autoScroll by remember { mutableStateOf(true) }

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(filteredLogs.size, isAtBottom) {
        if (autoScroll && isAtBottom && filteredLogs.isNotEmpty()) {
            try { listState.scrollToItem(filteredLogs.size - 1) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatsHeader(stats, logs.size)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearch(it) },
            placeholder = { Text("\u641C\u7D22\u65E5\u5FD7...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        FilterChipRow(filterLevel, viewModel::setFilter)

        Box(modifier = Modifier.fillMaxSize()) {
            if (filteredLogs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Terminal,
                    title = if (searchQuery.isNotBlank() || filterLevel != null) "\u6CA1\u6709\u5339\u914D\u7684\u65E5\u5FD7" else "\u6682\u65E0\u7EC8\u7AEF\u65E5\u5FD7",
                    subtitle = if (searchQuery.isNotBlank() || filterLevel != null) "\u5C1D\u8BD5\u8C03\u6574\u7B5B\u9009\u6761\u4EF6" else "\u5BF9\u8BDD\u4E0E\u8FDE\u63A5\u64CD\u4F5C\u5C06\u5728\u6B64\u5B9E\u65F6\u663E\u793A"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        LogLine(
                            log = log,
                            isExpanded = expandedEntryId == log.id,
                            onToggle = { viewModel.toggleExpand(log.id) }
                        )
                    }
                    item { Spacer(Modifier.height(64.dp)) }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { shareLogs(context, viewModel.exportLogs()) }) {
                    Icon(Icons.Default.Share, contentDescription = "\u5BFC\u51FA")
                }
                IconButton(onClick = { viewModel.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "\u6E05\u7A7A")
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(
    stats: Map<TerminalLogBuffer.LogLevel, Int>,
    total: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u603B\u8BA1: $total",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        stats.forEach { (level, count) ->
            if (count > 0) {
                val color = logLevelColor(level)
                Text(
                    text = "${level.name}: $count",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(
    currentFilter: TerminalLogBuffer.LogLevel?,
    onFilter: (TerminalLogBuffer.LogLevel?) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = currentFilter == null,
            onClick = { onFilter(null) },
            label = { Text("\u5168\u90E8", style = MaterialTheme.typography.labelSmall) }
        )
        listOf(
            TerminalLogBuffer.LogLevel.INFO,
            TerminalLogBuffer.LogLevel.WARN,
            TerminalLogBuffer.LogLevel.ERROR,
            TerminalLogBuffer.LogLevel.TOOL_CALL,
            TerminalLogBuffer.LogLevel.TOOL_RESULT,
            TerminalLogBuffer.LogLevel.MEMORY,
            TerminalLogBuffer.LogLevel.NETWORK
        ).forEach { level ->
            FilterChip(
                selected = currentFilter == level,
                onClick = { onFilter(if (currentFilter == level) null else level) },
                label = {
                    Text(level.name, style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        logLevelIcon(level),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = logLevelColor(level)
                    )
                }
            )
        }
    }
}

@Composable
private fun LogLine(
    log: TerminalLogBuffer.LogEntry,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val color = logLevelColor(log.level)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    logLevelIcon(log.level),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 1.dp),
                    tint = color
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = log.formattedTime(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "[${log.tag}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(4.dp))
                LogDetails(log)
            }
        }
    }
}

@Composable
private fun LogDetails(log: TerminalLogBuffer.LogEntry) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DetailRow("\u6765\u6E90", log.source)
            DetailRow("\u7EA7\u522B", log.level.name)
            log.toolName?.let { DetailRow("\u5DE5\u5177", it) }
            log.details?.let { DetailRow("\u8BE6\u60C5", it) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun logLevelColor(level: TerminalLogBuffer.LogLevel): Color = when (level) {
    TerminalLogBuffer.LogLevel.DEBUG -> Color(0xFF9E9E9E)
    TerminalLogBuffer.LogLevel.INFO -> Color(0xFF00BCD4)
    TerminalLogBuffer.LogLevel.WARN -> Color(0xFFFFA000)
    TerminalLogBuffer.LogLevel.ERROR -> Color(0xFFE53935)
    TerminalLogBuffer.LogLevel.TOOL_CALL -> Color(0xFF7B1FA2)
    TerminalLogBuffer.LogLevel.TOOL_RESULT -> Color(0xFF43A047)
    TerminalLogBuffer.LogLevel.MEMORY -> Color(0xFF1E88E5)
    TerminalLogBuffer.LogLevel.NETWORK -> Color(0xFF00897B)
}

private fun logLevelIcon(level: TerminalLogBuffer.LogLevel): ImageVector = when (level) {
    TerminalLogBuffer.LogLevel.DEBUG -> Icons.Default.Code
    TerminalLogBuffer.LogLevel.INFO -> Icons.Default.Info
    TerminalLogBuffer.LogLevel.WARN -> Icons.Default.Warning
    TerminalLogBuffer.LogLevel.ERROR -> Icons.Default.Error
    TerminalLogBuffer.LogLevel.TOOL_CALL -> Icons.Default.Build
    TerminalLogBuffer.LogLevel.TOOL_RESULT -> Icons.Default.CheckCircle
    TerminalLogBuffer.LogLevel.MEMORY -> Icons.Default.Memory
    TerminalLogBuffer.LogLevel.NETWORK -> Icons.Default.Cloud
}

private fun shareLogs(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "\u5BFC\u51FA\u7EC8\u7AEF\u65E5\u5FD7"))
}

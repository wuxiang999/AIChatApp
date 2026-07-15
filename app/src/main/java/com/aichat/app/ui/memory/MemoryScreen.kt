package com.aichat.app.ui.memory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.data.model.Memory
import com.aichat.app.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val filteredMemories by viewModel.filteredMemories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCounts.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
    val extractedCount by viewModel.extractedCount.collectAsStateWithLifecycle()
    val isConsolidating by viewModel.isConsolidating.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Memory?>(null) }
    var showImportanceDialog by remember { mutableStateOf<Memory?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Memory?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("搜索记忆...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategoryFilter(cat) },
                        label = {
                            Text(
                                "$cat (${categoryCounts[cat] ?: 0})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sources.forEach { src ->
                    FilterChip(
                        selected = selectedSource == src,
                        onClick = { viewModel.setSourceFilter(src) },
                        label = {
                            Text(src, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "共 $totalCount 条记忆",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text(
                            when (sortBy) {
                                MemorySortBy.TIME_DESC -> "按时间"
                                MemorySortBy.IMPORTANCE_DESC -> "按重要性"
                                MemorySortBy.ACCESS_COUNT_DESC -> "按热度"
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("按时间") },
                            onClick = { viewModel.setSortBy(MemorySortBy.TIME_DESC); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("按重要性") },
                            onClick = { viewModel.setSortBy(MemorySortBy.IMPORTANCE_DESC); sortMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("按热度") },
                            onClick = { viewModel.setSortBy(MemorySortBy.ACCESS_COUNT_DESC); sortMenuExpanded = false }
                        )
                    }
                }
            }

            if (filteredMemories.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Psychology,
                    title = if (searchQuery.isNotEmpty() || selectedCategory != null || selectedSource != null)
                        "没有匹配的记忆" else "还没有记忆",
                    subtitle = if (searchQuery.isNotEmpty() || selectedCategory != null || selectedSource != null)
                        "尝试修改筛选条件" else "添加的记忆会注入到每轮对话中，让 AI 记住你"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.extractFromLatestConversation() },
                                enabled = !isExtracting,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isExtracting) "提取中..." else "从对话提取")
                            }
                            OutlinedButton(
                                onClick = { viewModel.consolidateMemories() },
                                enabled = !isConsolidating,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isConsolidating) "整合中..." else "整合记忆")
                            }
                        }
                    }
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onDelete = { showDeleteConfirm = memory },
                            onEdit = { showEditDialog = memory },
                            onChangeImportance = { showImportanceDialog = memory }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增记忆")
        }
    }

    if (showAdd) {
        AddMemoryDialog(
            onDismiss = { showAdd = false },
            onConfirm = { content, category ->
                viewModel.addMemory(content, category)
                showAdd = false
            }
        )
    }

    showEditDialog?.let { mem ->
        EditMemoryDialog(
            memory = mem,
            onDismiss = { showEditDialog = null },
            onConfirm = { content, tags, importance ->
                viewModel.updateMemory(mem.id, content, tags, importance)
                showEditDialog = null
            }
        )
    }

    showImportanceDialog?.let { mem ->
        ImportanceDialog(
            memory = mem,
            onDismiss = { showImportanceDialog = null },
            onConfirm = { importance ->
                viewModel.updateImportance(mem.id, importance)
                showImportanceDialog = null
            }
        )
    }

    showDeleteConfirm?.let { mem ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除记忆") },
            text = { Text("确定删除这条记忆吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMemory(mem.id)
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoryCard(
    memory: Memory,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onChangeImportance: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                alpha = 0.3f + (memory.importance / 10f) * 0.4f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Psychology, contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(memory.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${memory.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("来源:${memory.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("访问${memory.accessCount}次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (memory.tags.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("标签: ${memory.tags}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(memory.importance / 2) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    if (memory.importance % 2 == 1) {
                        Icon(Icons.Default.StarBorder, contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = { showMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("更改重要性") },
                    onClick = { showMenu = false; onChangeImportance() }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemoryDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var content by remember { mutableStateOf("") }
    val categories = listOf("general", "fact", "preference", "identity", "knowledge", "task")
    var category by remember { mutableStateOf("general") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("记忆内容") },
                    minLines = 3, maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category, onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { category = c; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = content.isNotBlank(),
                onClick = { onConfirm(content.trim(), category) }
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditMemoryDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var content by remember { mutableStateOf(memory.content) }
    var tags by remember { mutableStateOf(memory.tags) }
    var importance by remember { mutableStateOf(memory.importance.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("内容") },
                    minLines = 3, maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tags, onValueChange = { tags = it },
                    label = { Text("标签（逗号分隔）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text("重要性: ${importance.toInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = importance,
                    onValueChange = { importance = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = content.isNotBlank(),
                onClick = { onConfirm(content.trim(), tags.trim(), importance.toInt()) }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ImportanceDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var importance by remember { mutableStateOf(memory.importance.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更改重要性") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("当前重要性: ${memory.importance}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = importance,
                    onValueChange = { importance = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text("新值: ${importance.toInt()}", style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(importance.toInt()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

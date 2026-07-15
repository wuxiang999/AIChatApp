package com.aichat.app.ui.workflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aichat.app.core.workflow.WorkflowManager
import com.aichat.app.data.model.workflow.ExecutionStatus
import com.aichat.app.data.model.workflow.Workflow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(
    workflowManager: WorkflowManager,
    onRunWorkflow: (Workflow) -> Unit,
    onEditWorkflow: (Workflow) -> Unit
) {
    var workflows by remember { mutableStateOf(workflowManager.getAllWorkflowsList()) }
    var showNewMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Workflow?>(null) }

    LaunchedEffect(Unit) {
        workflowManager.addListener { workflows = workflowManager.getAllWorkflowsList() }
    }

    DisposableEffect(Unit) {
        onDispose { /* listener will be cleaned up */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("工作流", fontWeight = FontWeight.Bold)
                        Text("自动化任务流水线", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showNewMenu = true }) {
                            Icon(Icons.Filled.Add, "新建工作流")
                        }
                        DropdownMenu(
                            expanded = showNewMenu,
                            onDismissRequest = { showNewMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("空白工作流") },
                                onClick = {
                                    showNewMenu = false
                                    workflowManager.createWorkflow()
                                },
                                leadingIcon = { Icon(Icons.Filled.NoteAdd, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("文件备份模板") },
                                onClick = {
                                    showNewMenu = false
                                    workflowManager.createFromTemplate("file_backup")
                                },
                                leadingIcon = { Icon(Icons.Filled.Backup, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("网页监控模板") },
                                onClick = {
                                    showNewMenu = false
                                    workflowManager.createFromTemplate("web_monitor")
                                },
                                leadingIcon = { Icon(Icons.Filled.Language, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("代码搜索模板") },
                                onClick = {
                                    showNewMenu = false
                                    workflowManager.createFromTemplate("code_search_pipeline")
                                },
                                leadingIcon = { Icon(Icons.Filled.Search, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (workflows.isEmpty()) {
            EmptyWorkflowState(modifier = Modifier
                .fillMaxSize()
                .padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workflows, key = { it.id }) { workflow ->
                    WorkflowCard(
                        workflow = workflow,
                        onRun = { onRunWorkflow(workflow) },
                        onEdit = { onEditWorkflow(workflow) },
                        onDelete = { showDeleteDialog = workflow },
                        onDuplicate = { workflowManager.duplicateWorkflow(workflow.id) }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { wf ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除工作流") },
            text = { Text("确定要删除「${wf.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    workflowManager.deleteWorkflow(wf.id)
                    showDeleteDialog = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkflowCard(
    workflow: Workflow,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showMenu = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = workflow.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    StatusBadge(workflow.lastExecutionStatus)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "更多", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("运行") },
                                onClick = { showMenu = false; onRun() },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("复制") },
                                onClick = { showMenu = false; onDuplicate() },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            if (workflow.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = workflow.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(icon = Icons.Filled.Schema, text = "${workflow.nodes.size} 节点")
                InfoChip(icon = Icons.Filled.Timeline, text = "${workflow.connections.size} 连接")

                val total = workflow.totalExecutions
                if (total > 0) {
                    val successRate = if (total > 0)
                        (workflow.successfulExecutions * 100 / total) else 0
                    InfoChip(
                        icon = if (successRate >= 80) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        text = "${successRate}% 成功率"
                    )
                }

                if (workflow.lastExecutionTime != null) {
                    InfoChip(
                        icon = Icons.Filled.Schedule,
                        text = dateFormat.format(Date(workflow.lastExecutionTime))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onRun,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("运行")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ExecutionStatus?) {
    if (status == null) return
    val (color, label) = when (status) {
        ExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.primary to "成功"
        ExecutionStatus.FAILED -> MaterialTheme.colorScheme.error to "失败"
        ExecutionStatus.RUNNING -> MaterialTheme.colorScheme.tertiary to "运行中"
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyWorkflowState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "还没有创建工作流",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "创建一个自动化的任务流水线来加速你的工作",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = { /* show new menu */ }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("创建第一个工作流")
            }
        }
    }
}

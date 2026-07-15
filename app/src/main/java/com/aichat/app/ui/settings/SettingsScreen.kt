package com.aichat.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.BuildConfig
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.ui.components.EmptyStateView
import com.aichat.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val endpoints by viewModel.endpoints.collectAsStateWithLifecycle()
    val currentEndpoint by viewModel.currentEndpoint.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val endpointTestResults by viewModel.endpointTestResults.collectAsStateWithLifecycle()
    val endpointModels by viewModel.endpointModels.collectAsStateWithLifecycle()
    val loadingEndpoints by viewModel.loadingEndpoints.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ApiEndpoint?>(null) }
    var newEndpointName by remember { mutableStateOf("") }
    var newEndpointUrl by remember { mutableStateOf("") }
    var newEndpointKey by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { CurrentEndpointCard(currentEndpoint, onLoadModels = { viewModel.loadModels() }) }

            // === Workspace Settings ===
            item { SectionHeader("工作区设置", icon = Icons.Default.FolderOpen) }
            item {
                WorkspaceSettingsCard(
                    workspacePath = settings.workspacePath,
                    autoSaveEnabled = settings.autoSaveEnabled,
                    autoSaveInterval = settings.autoSaveInterval,
                    onWorkspacePathChange = { viewModel.updateWorkspacePath(it) },
                    onAutoSaveEnabledChange = { viewModel.updateAutoSaveEnabled(it) },
                    onAutoSaveIntervalChange = { viewModel.updateAutoSaveInterval(it) }
                )
            }

            // === Editor Settings ===
            item { SectionHeader("编辑器设置", icon = Icons.Default.Code) }
            item {
                EditorSettingsCard(
                    fontSize = settings.fontSize,
                    editorTheme = settings.editorTheme,
                    showLineNumbers = settings.showLineNumbers,
                    tabSize = settings.tabSize,
                    onFontSizeChange = { viewModel.updateFontSize(it) },
                    onEditorThemeChange = { viewModel.updateEditorTheme(it) },
                    onShowLineNumbersChange = { viewModel.updateShowLineNumbers(it) },
                    onTabSizeChange = { viewModel.updateTabSize(it) }
                )
            }

            // === API Endpoints ===
            item { SectionHeader("API 端点管理", icon = Icons.Default.Cloud) }

            if (endpoints.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.Cloud,
                            title = "暂无 API 端点",
                            subtitle = "添加端点后即可连接到 AI 模型服务"
                        )
                    }
                }
            } else {
                items(endpoints, key = { it.id }) { endpoint ->
                    val isLoading = loadingEndpoints.contains(endpoint.id)
                    val testRes = endpointTestResults[endpoint.id]
                    val epModels = endpointModels[endpoint.id]

                    EndpointItem(
                        endpoint = endpoint,
                        isSelected = endpoint.id == currentEndpoint?.id,
                        testResult = testRes,
                        models = epModels,
                        isLoading = isLoading,
                        onSelect = { viewModel.selectEndpoint(endpoint.id) },
                        onEdit = {
                            showEditDialog = endpoint
                            newEndpointName = endpoint.name
                            newEndpointUrl = endpoint.url
                            newEndpointKey = endpoint.apiKey
                        },
                        onDelete = { viewModel.deleteEndpoint(endpoint.id) },
                        onLoadModels = { viewModel.loadModelsForEndpoint(endpoint) }
                    )
                }
            }

            item { SectionHeader("可用模型", icon = Icons.Default.Bolt) }
            item { AvailableModelsCard(models = models, isLoading = isLoadingModels) }

            // === AI Agent Settings ===
            item { SectionHeader("AI 代理设置", icon = Icons.Default.Psychology) }
            item {
                AgentSettingsCard(
                    defaultCodingModel = settings.defaultCodingModel,
                    maxIterations = settings.maxIterations,
                    permissionPreset = settings.permissionPreset,
                    autoExtractMemory = settings.autoExtractMemory,
                    onDefaultCodingModelChange = { viewModel.updateDefaultCodingModel(it) },
                    onMaxIterationsChange = { viewModel.updateMaxIterations(it) },
                    onPermissionPresetChange = { viewModel.updatePermissionPreset(it) },
                    onAutoExtractMemoryChange = { viewModel.updateAutoExtractMemory(it) }
                )
            }

            // === Git Settings ===
            item { SectionHeader("Git 集成", icon = Icons.Default.AccountTree) }
            item {
                GitSettingsCard(
                    gitEnabled = settings.gitEnabled,
                    gitAutoCommit = settings.gitAutoCommit,
                    gitUserName = settings.gitUserName,
                    gitUserEmail = settings.gitUserEmail,
                    onGitEnabledChange = { viewModel.updateGitEnabled(it) },
                    onGitAutoCommitChange = { viewModel.updateGitAutoCommit(it) },
                    onGitUserNameChange = { viewModel.updateGitUserName(it) },
                    onGitUserEmailChange = { viewModel.updateGitUserEmail(it) }
                )
            }

            // === About ===
            item { SectionHeader("关于", icon = Icons.Default.Info) }
            item { AboutCard() }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = {
                newEndpointName = ""
                newEndpointUrl = ""
                newEndpointKey = ""
                showAddDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
            text = { Text("添加端点") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (showAddDialog) {
        EndpointDialog(
            title = "添加 API 端点",
            name = newEndpointName,
            url = newEndpointUrl,
            apiKey = newEndpointKey,
            onNameChange = { newEndpointName = it },
            onUrlChange = { newEndpointUrl = it },
            onApiKeyChange = { newEndpointKey = it },
            onConfirm = {
                if (newEndpointName.isNotBlank() && newEndpointUrl.isNotBlank() && newEndpointKey.isNotBlank()) {
                    viewModel.addEndpoint(newEndpointName, newEndpointUrl, newEndpointKey)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    showEditDialog?.let { endpoint ->
        EndpointDialog(
            title = "编辑端点",
            name = newEndpointName,
            url = newEndpointUrl,
            apiKey = newEndpointKey,
            onNameChange = { newEndpointName = it },
            onUrlChange = { newEndpointUrl = it },
            onApiKeyChange = { newEndpointKey = it },
            onConfirm = {
                if (newEndpointName.isNotBlank() && newEndpointUrl.isNotBlank()) {
                    viewModel.updateEndpoint(endpoint.id, newEndpointName, newEndpointUrl, newEndpointKey)
                    showEditDialog = null
                }
            },
            onDismiss = { showEditDialog = null }
        )
    }
}

// ===== New Section Cards =====

@Composable
private fun WorkspaceSettingsCard(
    workspacePath: String,
    autoSaveEnabled: Boolean,
    autoSaveInterval: Int,
    onWorkspacePathChange: (String) -> Unit,
    onAutoSaveEnabledChange: (Boolean) -> Unit,
    onAutoSaveIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = workspacePath,
                onValueChange = onWorkspacePathChange,
                label = { Text("默认项目路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动保存", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "自动保存工作区更改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoSaveEnabled, onCheckedChange = onAutoSaveEnabledChange)
            }

            if (autoSaveEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("保存间隔", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = autoSaveInterval.toFloat(),
                        onValueChange = { onAutoSaveIntervalChange(it.toInt().coerceIn(10, 120)) },
                        valueRange = 10f..120f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        "${autoSaveInterval}秒",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSettingsCard(
    fontSize: Int,
    editorTheme: String,
    showLineNumbers: Boolean,
    tabSize: Int,
    onFontSizeChange: (Int) -> Unit,
    onEditorThemeChange: (String) -> Unit,
    onShowLineNumbersChange: (Boolean) -> Unit,
    onTabSizeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("字体大小", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${fontSize}px", style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt().coerceIn(12, 24)) },
                valueRange = 12f..24f,
                steps = 11
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("主题", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = editorTheme == "dark",
                    onClick = { onEditorThemeChange("dark") },
                    label = { Text("深色", style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = editorTheme == "light",
                    onClick = { onEditorThemeChange("light") },
                    label = { Text("浅色", style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("显示行号", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = showLineNumbers, onCheckedChange = onShowLineNumbersChange)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tab 大小", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Slider(
                    value = tabSize.toFloat(),
                    onValueChange = { onTabSizeChange(it.toInt().coerceIn(2, 8)) },
                    valueRange = 2f..8f,
                    steps = 5,
                    modifier = Modifier.width(120.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${tabSize}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AgentSettingsCard(
    defaultCodingModel: String,
    maxIterations: Int,
    permissionPreset: String,
    autoExtractMemory: Boolean,
    onDefaultCodingModelChange: (String) -> Unit,
    onMaxIterationsChange: (Int) -> Unit,
    onPermissionPresetChange: (String) -> Unit,
    onAutoExtractMemoryChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = defaultCodingModel,
                onValueChange = onDefaultCodingModelChange,
                label = { Text("默认编码模型") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最大迭代次数", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${maxIterations}", style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = maxIterations.toFloat(),
                onValueChange = { onMaxIterationsChange(it.toInt().coerceIn(1, 50)) },
                valueRange = 1f..50f,
                steps = 48
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("权限预设", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "strict" to "严格",
                    "balanced" to "平衡",
                    "relaxed" to "宽松"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = permissionPreset == value,
                        onClick = { onPermissionPresetChange(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (permissionPreset) {
                    "strict" -> "所有操作执行前需用户确认"
                    "balanced" -> "常规操作自动执行，敏感操作需确认"
                    "relaxed" -> "自动执行所有操作，适合受信任环境"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动记忆提取", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "自动从对话中提取关键信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoExtractMemory, onCheckedChange = onAutoExtractMemoryChange)
            }
        }
    }
}

@Composable
private fun GitSettingsCard(
    gitEnabled: Boolean,
    gitAutoCommit: Boolean,
    gitUserName: String,
    gitUserEmail: String,
    onGitEnabledChange: (Boolean) -> Unit,
    onGitAutoCommitChange: (Boolean) -> Unit,
    onGitUserNameChange: (String) -> Unit,
    onGitUserEmailChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用 Git", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "在工作区中集成 Git 版本控制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = gitEnabled, onCheckedChange = onGitEnabledChange)
            }

            if (gitEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动提交", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "修改后自动创建 Git 提交",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = gitAutoCommit, onCheckedChange = onGitAutoCommitChange)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gitUserName,
                    onValueChange = onGitUserNameChange,
                    label = { Text("Git 用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gitUserEmail,
                    onValueChange = onGitUserEmailChange,
                    label = { Text("Git 邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AboutRow(label = "版本", value = BuildConfig.VERSION_NAME)
            Spacer(modifier = Modifier.height(10.dp))
            AboutRow(label = "构建", value = BuildConfig.VERSION_CODE.toString())
            Spacer(modifier = Modifier.height(10.dp))
            AboutRow(label = "开源许可", value = "MIT")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GitHub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "github.com/aichat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/aichat")
                    }
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

// ===== Existing Composable Functions (preserved) =====

@Composable
private fun CurrentEndpointCard(
    currentEndpoint: ApiEndpoint?,
    onLoadModels: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前端点",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                if (currentEndpoint != null) {
                    Text(
                        text = currentEndpoint.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentEndpoint.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "未配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLoadModels,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("加载模型", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndpointItem(
    endpoint: ApiEndpoint,
    isSelected: Boolean,
    testResult: Result<Int>?,
    models: List<String>?,
    isLoading: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLoadModels: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            else
                                SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = endpoint.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = endpoint.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLoadModels,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("加载中...", style = MaterialTheme.typography.labelSmall)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("加载模型", style = MaterialTheme.typography.labelSmall)
                }
            }

            testResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                result.onSuccess { count ->
                    ResultCard(
                        text = "连接成功，$count 个模型",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                result.onFailure { error ->
                    ResultCard(
                        text = "${error.message}",
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (!models.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "模型 (${models.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 2
                ) {
                    models.take(6).forEach { model ->
                        Text(
                            text = model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (models.size > 6) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "还有 ${models.size - 6} 个模型...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = content
        )
    }
}

@Composable
private fun AvailableModelsCard(
    models: List<String>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("加载中...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (models.isEmpty()) {
                Text(
                    text = "点击上方「加载模型」按钮获取模型列表",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                var searchQuery by remember { mutableStateOf("") }
                var selectedModel by remember { mutableStateOf<String?>(null) }
                val filteredModels = remember(models, searchQuery) {
                    if (searchQuery.isBlank()) models
                    else models.filter { it.contains(searchQuery, ignoreCase = true) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 ${models.size} 个模型",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "选中: ${selectedModel ?: "无"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索模型...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (filteredModels.isEmpty()) {
                            item {
                                Text(
                                    text = "未找到匹配的模型",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            items(filteredModels) { model ->
                                val isSelected = model == selectedModel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedModel = if (isSelected) null else model
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = model,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EndpointDialog(
    title: String,
    name: String,
    url: String,
    apiKey: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("API 地址 (如 https://api.openai.com/v1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

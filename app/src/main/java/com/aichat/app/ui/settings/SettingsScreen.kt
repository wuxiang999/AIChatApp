package com.aichat.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.data.model.ApiEndpoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val endpoints by viewModel.endpoints.collectAsStateWithLifecycle()
    val currentEndpoint by viewModel.currentEndpoint.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ApiEndpoint?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var newEndpointName by remember { mutableStateOf("") }
    var newEndpointUrl by remember { mutableStateOf("") }
    var newEndpointKey by remember { mutableStateOf("") }
    var testUrl by remember { mutableStateOf("") }
    var testKey by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("设置") },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newEndpointName = ""
                    newEndpointUrl = ""
                    newEndpointKey = ""
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
                text = { Text("添加端点") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "当前 API 端点",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentEndpoint != null) {
                            Text(
                                text = currentEndpoint!!.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentEndpoint!!.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "未配置端点",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.loadModels() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("加载模型")
                            }
                            OutlinedButtonTest(
                                onClick = { showTestDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("测试连接")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "API 端点管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (endpoints.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无端点，请点击右下角添加",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(endpoints, key = { it.id }) { endpoint ->
                    EndpointItem(
                        endpoint = endpoint,
                        isSelected = endpoint.id == currentEndpoint?.id,
                        onSelect = { viewModel.selectEndpoint(endpoint.id) },
                        onEdit = {
                            showEditDialog = endpoint
                            newEndpointName = endpoint.name
                            newEndpointUrl = endpoint.url
                            newEndpointKey = endpoint.apiKey
                        },
                        onDelete = { viewModel.deleteEndpoint(endpoint.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可用模型",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isLoadingModels) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("加载中...")
                            }
                        } else if (models.isEmpty()) {
                            Text(
                                text = "点击上方「加载模型」按钮获取模型列表",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "共 ${models.size} 个模型",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            models.take(20).forEach { model ->
                                Text(
                                    text = "• $model",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (models.size > 20) {
                                Text(
                                    text = "... 还有 ${models.size - 20} 个模型",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
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

    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = {
                showTestDialog = false
                viewModel.clearTestResult()
            },
            title = { Text("测试连接") },
            text = {
                Column {
                    OutlinedTextField(
                        value = testUrl,
                        onValueChange = { testUrl = it },
                        label = { Text("API 地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testKey,
                        onValueChange = { testKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    testResult?.let { result ->
                        result.onSuccess { count ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Text(
                                    text = "✓ 连接成功！获取到 $count 个模型",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        result.onFailure { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = "✗ 连接失败: ${error.message}",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.testEndpoint(testUrl, testKey) }) {
                    Text("测试")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTestDialog = false
                    viewModel.clearTestResult()
                }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun OutlinedButtonTest(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun EndpointItem(
    endpoint: ApiEndpoint,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = endpoint.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = endpoint.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            TextButton(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

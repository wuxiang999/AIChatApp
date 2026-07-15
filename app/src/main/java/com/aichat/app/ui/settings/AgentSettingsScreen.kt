package com.aichat.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 代理设置",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
    }
}

@Composable
internal fun AgentSettingsCard(
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

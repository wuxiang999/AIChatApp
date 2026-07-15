package com.aichat.app.ui.settings

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
fun WorkspaceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "工作区设置",
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
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
        ) {
            WorkspaceSettingsCard(
                workspacePath = settings.workspacePath,
                autoSaveEnabled = settings.autoSaveEnabled,
                autoSaveInterval = settings.autoSaveInterval,
                onWorkspacePathChange = { viewModel.updateWorkspacePath(it) },
                onAutoSaveEnabledChange = { viewModel.updateAutoSaveEnabled(it) },
                onAutoSaveIntervalChange = { viewModel.updateAutoSaveInterval(it) }
            )

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
    }
}

@Composable
internal fun WorkspaceSettingsCard(
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
internal fun EditorSettingsCard(
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

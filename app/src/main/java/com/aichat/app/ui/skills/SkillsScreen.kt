package com.aichat.app.ui.skills

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.data.model.Skill
import com.aichat.app.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val filteredSkills by viewModel.filteredSkills.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editingSkill by remember { mutableStateOf<Skill?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 分类筛选栏
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "全部",
                        onClick = { viewModel.setCategory("全部") },
                        label = { Text("全部") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (filteredSkills.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.AutoAwesome,
                    title = if (selectedCategory == "全部") "还没有技能" else "该分类暂无技能",
                    subtitle = "点击右下角添加技能，启用后注入对话上下文"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSkills, key = { it.id }) { skill ->
                        SkillCard(
                            skill = skill,
                            onToggle = { viewModel.toggleEnabled(skill.id, it) },
                            onDelete = { viewModel.deleteSkill(skill.id) },
                            onEdit = { editingSkill = skill }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增技能")
        }
    }

    if (showAdd) {
        AddSkillDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, desc, tpl, cat, tags ->
                viewModel.addSkill(name, desc, tpl, cat, tags)
                showAdd = false
            }
        )
    }

    editingSkill?.let { skill ->
        EditSkillDialog(
            skill = skill,
            onDismiss = { editingSkill = null },
            onConfirm = { id, name, desc, tpl, cat, tags ->
                viewModel.updateSkill(id, name, desc, tpl, cat, tags)
                editingSkill = null
            }
        )
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (skill.enabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // 分类标签
                if (skill.category != "general" && skill.category.isNotBlank()) {
                    Text(
                        skill.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = skill.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (skill.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                skill.promptTemplate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun AddSkillDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var tpl by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("general") }
    var tags by remember { mutableStateOf("") }

    val categories = listOf("general", "dev", "writing", "research")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增技能") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("技能名称") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("描述") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                // 分类下拉
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("分类：", style = MaterialTheme.typography.bodySmall)
                    categories.forEach { c ->
                        FilterChip(
                            selected = cat == c,
                            onClick = { cat = c },
                            label = { Text(c) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                OutlinedTextField(value = tags, onValueChange = { tags = it },
                    label = { Text("标签（逗号分隔）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = tpl, onValueChange = { tpl = it },
                    label = { Text("提示词模板（可用 {{input}} 占位）") },
                    minLines = 3, maxLines = 5, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && tpl.isNotBlank(),
                onClick = { onConfirm(name.trim(), desc.trim(), tpl.trim(), cat, tags.trim()) }
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditSkillDialog(
    skill: Skill,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(skill.name) }
    var desc by remember { mutableStateOf(skill.description) }
    var tpl by remember { mutableStateOf(skill.promptTemplate) }
    var cat by remember { mutableStateOf(skill.category) }
    var tags by remember { mutableStateOf(skill.tags) }

    val categories = listOf("general", "dev", "writing", "research")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑技能") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("技能名称") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("描述") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("分类：", style = MaterialTheme.typography.bodySmall)
                    categories.forEach { c ->
                        FilterChip(
                            selected = cat == c,
                            onClick = { cat = c },
                            label = { Text(c) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                OutlinedTextField(value = tags, onValueChange = { tags = it },
                    label = { Text("标签（逗号分隔）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = tpl, onValueChange = { tpl = it },
                    label = { Text("提示词模板（可用 {{input}} 占位）") },
                    minLines = 3, maxLines = 5, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && tpl.isNotBlank(),
                onClick = { onConfirm(skill.id, name.trim(), desc.trim(), tpl.trim(), cat, tags.trim()) }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

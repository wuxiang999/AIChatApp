package com.aichat.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionDialog(
    action: String,
    resource: String,
    args: String,
    onAllow: () -> Unit,
    onAllowAlways: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text("权限请求") },
        text = {
            Text(
                buildString {
                    appendLine("工具: $action")
                    appendLine("资源: $resource")
                    append("参数: $args")
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("允许一次")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDeny) {
                    Text("拒绝", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onAllowAlways) {
                    Text("始终允许", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

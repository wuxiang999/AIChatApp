package com.aichat.app.ui.chat

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.animateScrollTo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.aichat.app.data.model.Message
import com.aichat.app.ui.theme.ReasoningAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun MessageBubble(
    message: Message,
    onRevokeMessage: (Int) -> Unit = {}
) {
    val isUser = message.role == "user"
    val imageUrls = message.imageUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    var showReasoning by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val replyScrollState = rememberScrollState()

    // 流式回复时：回复内容变化即自动滚动到底部，显示最新内容
    LaunchedEffect(message.content) {
        if (message.isStreaming && !message.isRevoked) {
            replyScrollState.animateScrollTo(replyScrollState.maxValue)
        }
    }

    fun downloadImage(url: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val isBase64 = url.startsWith("data:image")
                val (data, fileName, mime) = if (isBase64) {
                    Triple(
                        Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT),
                        "AI_生成图_${System.currentTimeMillis()}.png",
                        "image/png"
                    )
                } else {
                    val connection = URL(url).openConnection()
                    val bytes = connection.getInputStream().use { it.readBytes() }
                    Triple(
                        bytes,
                        "AI_生成图_${System.currentTimeMillis()}.jpg",
                        "image/jpeg"
                    )
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/月下AI")
                }
                val target: Uri? = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                target?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(data)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            AvatarBox(label = "AI", isUser = false)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .then(
                    if (isUser) Modifier.padding(start = 48.dp)
                    else Modifier.padding(end = 48.dp)
                )
        ) {
            if (!isUser && message.reasoningContent != null) {
                ReasoningCard(
                    content = message.reasoningContent,
                    expanded = showReasoning,
                    onToggle = { showReasoning = !showReasoning },
                    isStreaming = message.isStreaming
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box {
                Card(
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = if (isUser && !message.isRevoked) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showMenu = true }
                            )
                        }
                    } else Modifier
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (message.isRevoked) "你撤回了一条消息" else message.content.ifEmpty { "\u200B" },
                            style = if (message.isRevoked) MaterialTheme.typography.bodyMedium
                            else MaterialTheme.typography.bodyLarge,
                            color = if (isUser) {
                                if (message.isRevoked) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 320.dp)
                                .verticalScroll(replyScrollState)
                        )
                        if (!message.isRevoked && message.isStreaming) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (isUser && showMenu && !message.isRevoked) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("撤回") },
                            onClick = {
                                onRevokeMessage(message.index)
                                showMenu = false
                            }
                        )
                    }
                }
            }

            if (!message.isRevoked && imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                imageUrls.forEach { url ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // SubcomposeAsyncImage 显式处理加载/错误/成功三种状态，
                            // 避免静默失败导致图片不显示且无任何提示
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "图片",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "图片加载失败",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = url.take(60) + if (url.length > 60) "..." else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { downloadImage(url) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "下载图片",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            AvatarBox(label = "我", isUser = true)
        }
    }
}

@Composable
private fun AvatarBox(label: String, isUser: Boolean) {
    val brush = if (isUser) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReasoningCard(
    content: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    isStreaming: Boolean = false
) {
    val reasoningScrollState = rememberScrollState()
    // 流式思考时：思考内容变化即自动滚动到底部，显示最新内容
    LaunchedEffect(content, expanded) {
        if (expanded && isStreaming) {
            reasoningScrollState.animateScrollTo(reasoningScrollState.maxValue)
        }
    }
    Card(
        shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "思考",
                    modifier = Modifier.size(18.dp),
                    tint = ReasoningAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "思考过程",
                    style = MaterialTheme.typography.titleSmall,
                    color = ReasoningAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        modifier = Modifier.size(20.dp),
                        tint = ReasoningAccent
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(reasoningScrollState)
                )
            }
        }
    }
}

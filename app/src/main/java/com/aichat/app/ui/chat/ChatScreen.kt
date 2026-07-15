package com.aichat.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.Message
import kotlinx.coroutines.launch

private const val MAX_FILE_SIZE = 50_000

data class ChatCallbacks(
    val onSendMessage: (String, List<String>) -> Unit,
    val onStopGeneration: () -> Unit,
    val onClearConversation: () -> Unit,
    val onModelChange: (String) -> Unit,
    val onRevokeMessage: (Int) -> Unit,
    val onGenerateImage: (String) -> Unit,
    val onEditImage: (String, String) -> Unit,
    val onRefreshModels: () -> Unit,
    val onRefreshAgent: () -> Unit,
    val onNewConversation: () -> Unit,
    val onEndpointChange: (Long) -> Unit,
    val onImageCountChange: (Int) -> Unit,
    val onImageSizeChange: (String) -> Unit,
    val onImageModelChange: (String) -> Unit,
    val onImageEditModeChange: (Boolean) -> Unit
)

data class ChatState(
    val messages: List<Message>,
    val isLoading: Boolean,
    val error: String?,
    val currentModel: String,
    val availableModels: List<String>,
    val endpoints: List<ApiEndpoint>,
    val currentEndpointId: Long?,
    val imageCount: Int,
    val imageSize: String,
    val imageModel: String,
    val isImageEditMode: Boolean,
    val currentAgentName: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    callbacks: ChatCallbacks,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf("") }
    var isImageMode by remember { mutableStateOf(false) }
    val selectedImageUris = remember { mutableStateListOf<String>() }
    val selectedFileContents = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var modelExpanded by remember { mutableStateOf(false) }
    var endpointExpanded by remember { mutableStateOf(false) }
    var imageCountExpanded by remember { mutableStateOf(false) }
    var imageSizeExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val imageSizeOptions = listOf("512x512", "768x768", "1024x1024", "1024x1792", "1792x1024")
    val imageCountOptions = listOf(1, 2, 3, 4, 5)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUris.add(it.toString())
            if (!state.isImageEditMode) {
                callbacks.onImageEditModeChange(true)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val content = context.contentResolver.openInputStream(fileUri)
                    ?.bufferedReader()?.use { it.readText() } ?: return@let
                if (content.length > MAX_FILE_SIZE) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("文件内容过长，请选择小于50KB的文件")
                    }
                } else {
                    val fileName = fileUri.lastPathSegment ?: "unknown.txt"
                    selectedFileContents.add("📄 $fileName:\n$content")
                    text = "${text}${if (text.isNotEmpty()) "\n" else ""}📄 $fileName\n"
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("读取文件失败: ${e.message}")
                }
            }
        }
    }

    val lastMessageContent = state.messages.lastOrNull()?.content
    val lastMessageReasoning = state.messages.lastOrNull()?.reasoningContent
    LaunchedEffect(state.messages.size, state.isLoading, lastMessageContent, lastMessageReasoning) {
        if (state.messages.isNotEmpty()) {
            try {
                listState.scrollToItem(state.messages.size - 1)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ParticleBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            if (!state.currentAgentName.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "人设：${state.currentAgentName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (state.messages.isEmpty()) {
                EmptyChatState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.index }) { message ->
                        MessageBubble(
                            message = message,
                            onRevokeMessage = callbacks.onRevokeMessage
                        )
                    }
                    if (state.isLoading && state.messages.lastOrNull()?.role != "assistant") {
                        item { ThinkingIndicator() }
                    }
                }
            }

            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                isImageMode = isImageMode,
                onToggleImageMode = {
                    isImageMode = !isImageMode
                    if (!isImageMode) {
                        callbacks.onImageEditModeChange(false)
                        selectedImageUris.clear()
                    }
                },
                isLoading = state.isLoading,
                endpoints = state.endpoints,
                currentEndpointId = state.currentEndpointId,
                endpointExpanded = endpointExpanded,
                onEndpointExpandedChange = { endpointExpanded = it },
                onEndpointChange = callbacks.onEndpointChange,
                availableModels = state.availableModels,
                currentModel = state.currentModel,
                modelExpanded = modelExpanded,
                onModelExpandedChange = { modelExpanded = it },
                onModelChange = callbacks.onModelChange,
                onRefreshModels = callbacks.onRefreshModels,
                selectedImageUris = selectedImageUris,
                onRemoveImage = { idx -> selectedImageUris.removeAt(idx) },
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onPickFile = { filePickerLauncher.launch("*/*") },
                isImageEditMode = state.isImageEditMode,
                onImageEditModeChange = callbacks.onImageEditModeChange,
                imageCount = state.imageCount,
                imageSize = state.imageSize,
                imageCountOptions = imageCountOptions,
                imageSizeOptions = imageSizeOptions,
                imageCountExpanded = imageCountExpanded,
                imageSizeExpanded = imageSizeExpanded,
                onImageCountExpandedChange = { imageCountExpanded = it },
                onImageSizeExpandedChange = { imageSizeExpanded = it },
                onImageCountChange = callbacks.onImageCountChange,
                onImageSizeChange = callbacks.onImageSizeChange,
                hasFileContents = selectedFileContents.isNotEmpty(),
                onSendClick = {
                    focusManager.clearFocus()
                    if (isImageMode) {
                        if (state.isImageEditMode && selectedImageUris.isNotEmpty() && text.isNotBlank()) {
                            callbacks.onEditImage(selectedImageUris.first(), text)
                            selectedImageUris.clear()
                            callbacks.onImageEditModeChange(false)
                            text = ""
                        } else if (!state.isImageEditMode && text.isNotBlank()) {
                            callbacks.onGenerateImage(text)
                            text = ""
                        }
                    } else {
                        if (text.isNotBlank() || selectedFileContents.isNotEmpty()) {
                            val fullText = buildString {
                                append(text)
                                selectedFileContents.forEach { fc -> append("\n\n").append(fc) }
                            }
                            callbacks.onSendMessage(fullText, selectedImageUris.toList())
                            selectedImageUris.clear()
                            selectedFileContents.clear()
                            text = ""
                        }
                    }
                },
                onStopClick = {
                    focusManager.clearFocus()
                    callbacks.onStopGeneration()
                },
                onNewConversation = callbacks.onNewConversation,
                onClearConversation = callbacks.onClearConversation
            )
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "C",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "CodeVibe",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Vibe Coding Agent",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "支持多轮对话 · 流式响应 · 图片上传\n图片生成 · 模型切换",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(end = 48.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "正在思考...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isImageMode: Boolean,
    onToggleImageMode: () -> Unit,
    isLoading: Boolean,
    endpoints: List<ApiEndpoint>,
    currentEndpointId: Long?,
    endpointExpanded: Boolean,
    onEndpointExpandedChange: (Boolean) -> Unit,
    onEndpointChange: (Long) -> Unit,
    availableModels: List<String>,
    currentModel: String,
    modelExpanded: Boolean,
    onModelExpandedChange: (Boolean) -> Unit,
    onModelChange: (String) -> Unit,
    onRefreshModels: () -> Unit,
    selectedImageUris: List<String>,
    onRemoveImage: (Int) -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    isImageEditMode: Boolean,
    onImageEditModeChange: (Boolean) -> Unit,
    imageCount: Int,
    imageSize: String,
    imageCountOptions: List<Int>,
    imageSizeOptions: List<String>,
    imageCountExpanded: Boolean,
    imageSizeExpanded: Boolean,
    onImageCountExpandedChange: (Boolean) -> Unit,
    onImageSizeExpandedChange: (Boolean) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onImageSizeChange: (String) -> Unit,
    hasFileContents: Boolean,
    onSendClick: () -> Unit,
    onStopClick: () -> Unit,
    onNewConversation: () -> Unit,
    onClearConversation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
        ) {
            if (endpoints.isNotEmpty()) {
                EndpointPicker(
                    endpoints = endpoints,
                    currentEndpointId = currentEndpointId,
                    expanded = endpointExpanded,
                    onExpandedChange = onEndpointExpandedChange,
                    onEndpointChange = onEndpointChange
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            ModelPicker(
                availableModels = availableModels,
                currentModel = currentModel,
                expanded = modelExpanded,
                onExpandedChange = onModelExpandedChange,
                onModelChange = onModelChange,
                onRefresh = onRefreshModels
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (selectedImageUris.isNotEmpty()) {
                SelectedImagesRow(
                    uris = selectedImageUris,
                    onRemove = onRemoveImage
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton(
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = "上传图片",
                    onClick = onPickImage
                )
                ToolButton(
                    icon = Icons.Default.AttachFile,
                    contentDescription = "上传文件",
                    onClick = onPickFile
                )
                ToolButton(
                    icon = Icons.Default.Image,
                    contentDescription = if (isImageMode) "图片生成模式" else "切换图片生成",
                    tint = if (isImageMode) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    onClick = onToggleImageMode
                )

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    placeholder = {
                        Text(
                            text = if (isImageMode) "输入图片描述生成图片..." else "输入消息...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (isLoading) {
                    IconButton(
                        onClick = onStopClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "停止",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    val canSend = if (isImageMode) {
                        if (isImageEditMode) text.isNotBlank() && selectedImageUris.isNotEmpty()
                        else text.isNotBlank()
                    } else {
                        text.isNotBlank() || hasFileContents
                    }
                    IconButton(
                        onClick = onSendClick,
                        enabled = canSend,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isImageMode) "生成图片" else "发送",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isImageMode) {
                Spacer(modifier = Modifier.height(6.dp))
                ImageOptionsRow(
                    imageCount = imageCount,
                    imageSize = imageSize,
                    imageCountOptions = imageCountOptions,
                    imageSizeOptions = imageSizeOptions,
                    imageCountExpanded = imageCountExpanded,
                    imageSizeExpanded = imageSizeExpanded,
                    onImageCountExpandedChange = onImageCountExpandedChange,
                    onImageSizeExpandedChange = onImageSizeExpandedChange,
                    onImageCountChange = onImageCountChange,
                    onImageSizeChange = onImageSizeChange,
                    isImageEditMode = isImageEditMode,
                    onImageEditModeChange = onImageEditModeChange
                )
                if (isImageEditMode && selectedImageUris.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请先上传一张图片用于图生图",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndpointPicker(
    endpoints: List<ApiEndpoint>,
    currentEndpointId: Long?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEndpointChange: (Long) -> Unit
) {
    val currentName = endpoints.find { it.id == currentEndpointId }?.name ?: "选择端点"
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "API端点",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开端点列表",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            endpoints.forEach { endpoint ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = endpoint.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onEndpointChange(endpoint.id)
                        onExpandedChange(false)
                    },
                    trailingIcon = {
                        if (endpoint.id == currentEndpointId) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(
    availableModels: List<String>,
    currentModel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModelChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var modelSearchQuery by remember { mutableStateOf("") }
    val filteredModels = remember(availableModels, modelSearchQuery) {
        if (modelSearchQuery.isBlank()) availableModels
        else availableModels.filter { it.contains(modelSearchQuery, ignoreCase = true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                modelSearchQuery = ""
                onExpandedChange(true)
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前模型",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expanded) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentModel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展开模型列表",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = {
                onExpandedChange(false)
                modelSearchQuery = ""
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择模型",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (availableModels.isNotEmpty()) {
                        Text(
                            text = "共 ${availableModels.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新模型列表",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = modelSearchQuery,
                    onValueChange = { modelSearchQuery = it },
                    placeholder = { Text("搜索模型...", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    availableModels.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .clickable { onRefresh() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "当前端点暂无模型",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "点击刷新重新加载",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    filteredModels.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到匹配「$modelSearchQuery」的模型",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(filteredModels) { model ->
                                val isSelected = model == currentModel
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = model,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        onModelChange(model)
                                        onExpandedChange(false)
                                        modelSearchQuery = ""
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImagesRow(
    uris: List<String>,
    onRemove: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uris.forEachIndexed { idx, uri ->
            Box {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = { onRemove(idx) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "移除",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageOptionsRow(
    imageCount: Int,
    imageSize: String,
    imageCountOptions: List<Int>,
    imageSizeOptions: List<String>,
    imageCountExpanded: Boolean,
    imageSizeExpanded: Boolean,
    onImageCountExpandedChange: (Boolean) -> Unit,
    onImageSizeExpandedChange: (Boolean) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onImageSizeChange: (String) -> Unit,
    isImageEditMode: Boolean,
    onImageEditModeChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = imageCountExpanded,
                onExpandedChange = onImageCountExpandedChange,
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "$imageCount 张",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("数量", style = MaterialTheme.typography.bodySmall) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageCountExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = menuTextFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                ExposedDropdownMenu(
                    expanded = imageCountExpanded,
                    onDismissRequest = { onImageCountExpandedChange(false) }
                ) {
                    imageCountOptions.forEach { count ->
                        DropdownMenuItem(
                            text = { Text("$count 张") },
                            onClick = {
                                onImageCountChange(count)
                                onImageCountExpandedChange(false)
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = imageSizeExpanded,
                onExpandedChange = onImageSizeExpandedChange,
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = imageSize,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("尺寸", style = MaterialTheme.typography.bodySmall) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageSizeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = menuTextFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                ExposedDropdownMenu(
                    expanded = imageSizeExpanded,
                    onDismissRequest = { onImageSizeExpandedChange(false) }
                ) {
                    imageSizeOptions.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size) },
                            onClick = {
                                onImageSizeChange(size)
                                onImageSizeExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isImageEditMode) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "图生图模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isImageEditMode,
                onCheckedChange = onImageEditModeChange,
                modifier = Modifier.size(36.dp, 20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "上传图片后开启",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun menuTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)


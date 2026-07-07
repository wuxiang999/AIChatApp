package com.aichat.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import coil.compose.AsyncImage
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    isLoading: Boolean,
    error: String?,
    currentModel: String,
    availableModels: List<String>,
    endpoints: List<ApiEndpoint>,
    currentEndpointId: Long?,
    imageCount: Int,
    imageSize: String,
    imageModel: String,
    isImageEditMode: Boolean,
    onSendMessage: (String, List<String>) -> Unit,
    onStopGeneration: () -> Unit,
    onClearConversation: () -> Unit,
    onModelChange: (String) -> Unit,
    onEndpointChange: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onGenerateImage: (String) -> Unit,
    onEditImage: (String, String) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onImageSizeChange: (String) -> Unit,
    onImageModelChange: (String) -> Unit,
    onImageEditModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf("") }
    var isImageMode by remember { mutableStateOf(false) }
    val selectedImageUris = remember { mutableStateListOf<String>() }
    val selectedFileContents = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
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
            if (!isImageEditMode) {
                onImageEditModeChange(true)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                inputStream?.bufferedReader()?.use { reader ->
                    val content = reader.readText()
                    if (content.length > 50000) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("文件内容过长，请选择小于50KB的文件")
                        }
                    } else {
                        val fileName = fileUri.lastPathSegment ?: "unknown.txt"
                        selectedFileContents.add("📄 $fileName:\n$content")
                        text = "${text}${if (text.isNotEmpty()) "\n" else ""}📄 $fileName\n"
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("读取文件失败: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    if (endpoints.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = endpointExpanded,
                            onExpandedChange = { endpointExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = endpoints.find { it.id == currentEndpointId }?.name ?: "选择端点",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("API端点", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endpointExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(
                                expanded = endpointExpanded,
                                onDismissRequest = { endpointExpanded = false }
                            ) {
                                endpoints.forEach { endpoint ->
                                    DropdownMenuItem(
                                        text = { Text(endpoint.name) },
                                        onClick = {
                                            onEndpointChange(endpoint.id)
                                            endpointExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (availableModels.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentModel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("当前模型", style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            ExposedDropdownMenu(
                                expanded = modelExpanded,
                                onDismissRequest = { modelExpanded = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            onModelChange(model)
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedImageUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedImageUris.forEachIndexed { idx, uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    IconButton(
                                        onClick = { selectedImageUris.removeAt(idx) },
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
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "上传图片",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "上传文件",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isImageMode = !isImageMode
                                if (!isImageMode) {
                                    onImageEditModeChange(false)
                                    selectedImageUris.clear()
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = if (isImageMode) "图片生成模式" else "切换图片生成",
                                tint = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f).height(56.dp),
                            placeholder = {
                                Text(
                                    text = if (isImageMode) "输入图片描述生成图片..." else "输入消息...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            shape = RoundedCornerShape(28.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isLoading) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onStopGeneration()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "停止",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (isImageMode) {
                                        if (isImageEditMode && selectedImageUris.isNotEmpty() && text.isNotBlank()) {
                                            onEditImage(selectedImageUris.first(), text)
                                            selectedImageUris.clear()
                                            onImageEditModeChange(false)
                                            text = ""
                                        } else if (!isImageEditMode && text.isNotBlank()) {
                                            onGenerateImage(text)
                                            text = ""
                                        }
                                    } else {
                                        if (text.isNotBlank() || selectedFileContents.isNotEmpty()) {
                                            val fullText = buildString {
                                                append(text)
                                                selectedFileContents.forEach { fileContent ->
                                                    append("\n\n").append(fileContent)
                                                }
                                            }
                                            onSendMessage(fullText, selectedImageUris.toList())
                                            selectedImageUris.clear()
                                            selectedFileContents.clear()
                                            text = ""
                                        }
                                    }
                                },
                                enabled = if (isImageMode) {
                                    if (isImageEditMode) text.isNotBlank() && selectedImageUris.isNotEmpty()
                                    else text.isNotBlank()
                                } else {
                                    text.isNotBlank() || selectedFileContents.isNotEmpty()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (isImageMode) {
                                            if (isImageEditMode && text.isNotBlank() && selectedImageUris.isNotEmpty())
                                                MaterialTheme.colorScheme.primary
                                            else if (!isImageEditMode && text.isNotBlank())
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        } else {
                                            if (text.isNotBlank() || selectedFileContents.isNotEmpty())
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        },
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = if (isImageMode) "生成图片" else "发送",
                                    tint = if (isImageMode) {
                                        if (isImageEditMode && text.isNotBlank() && selectedImageUris.isNotEmpty())
                                            MaterialTheme.colorScheme.onPrimary
                                        else if (!isImageEditMode && text.isNotBlank())
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    } else {
                                        if (text.isNotBlank() || selectedFileContents.isNotEmpty())
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (isImageMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                    expanded = imageCountExpanded,
                                    onExpandedChange = { imageCountExpanded = it },
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
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                ExposedDropdownMenu(
                                    expanded = imageCountExpanded,
                                    onDismissRequest = { imageCountExpanded = false }
                                ) {
                                    imageCountOptions.forEach { count ->
                                        DropdownMenuItem(
                                            text = { Text("$count 张") },
                                            onClick = {
                                                onImageCountChange(count)
                                                imageCountExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                    expanded = imageSizeExpanded,
                                    onExpandedChange = { imageSizeExpanded = it },
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
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                ExposedDropdownMenu(
                                    expanded = imageSizeExpanded,
                                    onDismissRequest = { imageSizeExpanded = false }
                                ) {
                                    imageSizeOptions.forEach { size ->
                                        DropdownMenuItem(
                                            text = { Text(size) },
                                            onClick = {
                                                onImageSizeChange(size)
                                                imageSizeExpanded = false
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
                            Text(
                                text = "图生图模式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Switch(
                                checked = isImageEditMode,
                                onCheckedChange = { onImageEditModeChange(it) },
                                modifier = Modifier.size(36.dp, 20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "上传图片后开启",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        if (isImageEditMode && selectedImageUris.isEmpty()) {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ParticleBackground(modifier = Modifier.fillMaxSize())

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "月下AI",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "本地AI聊天助手",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "支持多轮对话 · 流式响应 · 图片上传\n图片生成 · 模型切换",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.index }) { message ->
                        MessageBubble(message = message)
                    }
                    if (isLoading && messages.lastOrNull()?.role != "assistant") {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    val imageUrls = message.imageUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    var showReasoning by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun downloadImage(url: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (url.startsWith("data:image")) {
                    val base64Data = url.substringAfter("base64,")
                    val decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val fileName = "AI_生成图_${System.currentTimeMillis()}.png"
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/月下AI")
                    }
                    val uri = context.contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(decodedString)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val urlObj = java.net.URL(url)
                    val connection = urlObj.openConnection()
                    val inputStream = connection.getInputStream()
                    val fileName = "AI_生成图_${System.currentTimeMillis()}.jpg"
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/月下AI")
                    }
                    val uri = context.contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                        }
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB2EBF2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF006064),
                    fontWeight = FontWeight.Bold
                )
            }
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
                Card(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 18.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE9DDFF)
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
                                tint = Color(0xFF7C4DFF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "思考过程",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7C4DFF),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { showReasoning = !showReasoning },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (showReasoning) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showReasoning) "收起" else "展开",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF7C4DFF)
                                )
                            }
                        }
                        if (showReasoning) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message.reasoningContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5D3FD3)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.content.ifEmpty { "\u200B" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (imageUrls.isNotEmpty()) {
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
                        AsyncImage(
                            model = url,
                            contentDescription = "图片",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "我",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.aichat.app.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.Message
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val apiManager: ApiManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId")
        ?: throw IllegalArgumentException("Conversation ID is required")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageCount = MutableStateFlow(1)
    val imageCount: StateFlow<Int> = _imageCount.asStateFlow()

    private val _imageSize = MutableStateFlow("1024x1024")
    val imageSize: StateFlow<String> = _imageSize.asStateFlow()

    private val _imageModel = MutableStateFlow("dall-e-3")
    val imageModel: StateFlow<String> = _imageModel.asStateFlow()

    private val _isImageEditMode = MutableStateFlow(false)
    val isImageEditMode: StateFlow<Boolean> = _isImageEditMode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentModel = MutableStateFlow("gpt-3.5-turbo")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _endpoints = MutableStateFlow<List<ApiEndpoint>>(emptyList())
    val endpoints: StateFlow<List<ApiEndpoint>> = _endpoints.asStateFlow()

    private val _currentEndpointId = MutableStateFlow<Long?>(null)
    val currentEndpointId: StateFlow<Long?> = _currentEndpointId.asStateFlow()

    private var currentStreamJob: Job? = null
    private var streamCall: Call<ResponseBody>? = null

    init {
        initializeAgents()
        loadMessages()
        loadConversationInfo()
        loadModels()
        loadEndpoints()
    }

    private fun initializeAgents() {
        viewModelScope.launch {
            try {
                repository.initializeAgents()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "initializeAgents error", e)
            }
        }
    }

    private fun loadEndpoints() {
        viewModelScope.launch {
            try {
                apiManager.getAllEndpoints().collect { endpointList ->
                    _endpoints.value = endpointList
                    _currentEndpointId.value = endpointList.find { it.isSelected }?.id
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "loadEndpoints error", e)
            }
        }
    }

    fun selectEndpoint(endpointId: Long) {
        viewModelScope.launch {
            try {
                apiManager.selectEndpoint(endpointId)
                loadModels()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "selectEndpoint error", e)
                _error.value = "切换端点失败: ${e.message}"
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                repository.getMessagesForConversation(conversationId).collect { messages ->
                    _messages.value = messages
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "loadMessages error", e)
            }
        }
    }

    private fun loadConversationInfo() {
        viewModelScope.launch {
            try {
                val conv = repository.getConversation(conversationId)
                conv?.let {
                    _currentModel.value = it.model
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "loadConversationInfo error", e)
            }
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val models = repository.getModels()
                _availableModels.value = models
            } catch (e: Exception) {
                Log.e("ChatViewModel", "loadModels error", e)
            }
        }
    }

    /**
     * 手动刷新当前端点的模型列表（供 UI 刷新按钮调用）
     */
    fun refreshModels() {
        loadModels()
    }

    fun sendMessage(text: String, imageUris: List<String> = emptyList()) {
        if (text.isBlank() && imageUris.isEmpty()) return
        if (_isLoading.value || _isGeneratingImage.value) return

        // 检测图片生成命令 /img
        if (text.startsWith("/img ")) {
            val prompt = text.removePrefix("/img ").trim()
            generateImage(prompt)
            return
        }

        _error.value = null
        val model = _currentModel.value

        viewModelScope.launch {
            repository.addUserMessage(conversationId, text, imageUris)
            val assistantIndex = repository.addAssistantMessage(conversationId, "", isStreaming = true)

            _isLoading.value = true

            withContext(Dispatchers.IO) {
                try {
                    val call = repository.sendMessageStream(
                        conversationId = conversationId,
                        currentMessage = text,
                        imageUris = imageUris,
                        model = model
                    )
                    streamCall = call

                    val response = call.execute()
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            processStreamResponse(body, assistantIndex)
                        }
                    } else {
                        val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                        repository.updateAssistantMessage(
                            conversationId,
                            assistantIndex,
                            "错误: $errorMsg",
                            isStreaming = false
                        )
                        _error.value = errorMsg
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Stream error", e)
                    repository.updateAssistantMessage(
                        conversationId,
                        assistantIndex,
                        "错误: ${e.message ?: "未知错误"}",
                        isStreaming = false
                    )
                    _error.value = e.message
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun generateImage(prompt: String) {
        if (prompt.isBlank() || _isGeneratingImage.value || _isLoading.value) return

        _error.value = null

        viewModelScope.launch {
            repository.addUserMessage(conversationId, "生成图片: $prompt")
            val assistantIndex = repository.addAssistantMessage(conversationId, "正在生成图片...", isStreaming = true)
            _isGeneratingImage.value = true
            _isLoading.value = true

            withContext(Dispatchers.IO) {
                try {
                    val result = repository.generateImage(
                        prompt = prompt,
                        n = _imageCount.value,
                        size = _imageSize.value,
                        model = _imageModel.value
                    )
                    result.onSuccess { urls ->
                        repository.updateAssistantMessage(
                            conversationId, assistantIndex,
                            "图片生成完成", isStreaming = false, imageUrls = urls
                        )
                    }.onFailure { error ->
                        repository.updateAssistantMessage(
                            conversationId, assistantIndex,
                            "图片生成失败: ${error.message}", isStreaming = false
                        )
                        _error.value = error.message
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "generateImage error", e)
                    repository.updateAssistantMessage(
                        conversationId, assistantIndex,
                        "图片生成错误: ${e.message}", isStreaming = false
                    )
                    _error.value = e.message
                } finally {
                    _isGeneratingImage.value = false
                    _isLoading.value = false
                }
            }
        }
    }

    fun editImage(imageUri: String, prompt: String) {
        if (prompt.isBlank() || _isGeneratingImage.value || _isLoading.value) return

        _error.value = null
        _isGeneratingImage.value = true
        _isLoading.value = true

        viewModelScope.launch {
            repository.addUserMessage(conversationId, "图生图: $prompt")
            val assistantIndex = repository.addAssistantMessage(conversationId, "正在生成图片...", isStreaming = true)

            withContext(Dispatchers.IO) {
                try {
                    val result = repository.editImage(
                        imageUri = imageUri,
                        prompt = prompt,
                        n = _imageCount.value,
                        size = _imageSize.value,
                        model = _imageModel.value
                    )
                    result.onSuccess { urls ->
                        repository.updateAssistantMessage(
                            conversationId, assistantIndex,
                            "图片生成完成", isStreaming = false, imageUrls = urls
                        )
                    }.onFailure { error ->
                        repository.updateAssistantMessage(
                            conversationId, assistantIndex,
                            "图片生成失败: ${error.message}", isStreaming = false
                        )
                        _error.value = error.message
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "editImage error", e)
                    repository.updateAssistantMessage(
                        conversationId, assistantIndex,
                        "图片生成错误: ${e.message}", isStreaming = false
                    )
                    _error.value = e.message
                } finally {
                    _isGeneratingImage.value = false
                    _isLoading.value = false
                }
            }
        }
    }

    fun setImageCount(count: Int) {
        _imageCount.value = count
    }

    fun setImageSize(size: String) {
        _imageSize.value = size
    }

    fun setImageModel(model: String) {
        _imageModel.value = model
    }

    fun setImageEditMode(isEdit: Boolean) {
        _isImageEditMode.value = isEdit
    }

    private suspend fun processStreamResponse(body: ResponseBody, assistantIndex: Int) {
        val fullMessage = StringBuilder()
        val fullReasoning = StringBuilder()
        try {
            val reader = body.charStream().buffered()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val dataLine = line ?: continue
                if (dataLine.startsWith("data: ")) {
                    val jsonData = dataLine.substring(6)
                    if (jsonData == "[DONE]") break

                    val streamResponse = repository.parseSseData(jsonData)
                    if (streamResponse != null) {
                        if (streamResponse.error != null) {
                            fullMessage.append("\n[错误: ${streamResponse.error.message}]")
                            repository.updateAssistantMessage(
                                conversationId,
                                assistantIndex,
                                fullMessage.toString(),
                                isStreaming = false
                            )
                            _error.value = streamResponse.error.message
                            return
                        }

                        val choice = streamResponse.choices?.firstOrNull()

                        if (choice != null) {
                            val delta = choice.delta
                            val message = choice.message

                            val contentFromDelta = delta?.content
                            val contentFromMessage = message?.content

                            val reasoningFromDelta = delta?.reasoning_content
                                ?: delta?.thinking_content
                                ?: delta?.reasoning
                                ?: delta?.thought

                            val reasoningFromMessage = message?.reasoning_content
                                ?: message?.thinking_content
                                ?: message?.reasoning
                                ?: message?.thought

                            if (contentFromDelta != null && contentFromDelta.isNotEmpty()) {
                                fullMessage.append(contentFromDelta)
                            } else if (contentFromMessage != null && contentFromMessage.isNotEmpty()) {
                                fullMessage.clear()
                                fullMessage.append(contentFromMessage)
                            }

                            if (reasoningFromDelta != null && reasoningFromDelta.isNotEmpty()) {
                                fullReasoning.append(reasoningFromDelta)
                            } else if (reasoningFromMessage != null && reasoningFromMessage.isNotEmpty()) {
                                fullReasoning.clear()
                                fullReasoning.append(reasoningFromMessage)
                            }

                            repository.updateAssistantMessage(
                                conversationId,
                                assistantIndex,
                                fullMessage.toString(),
                                isStreaming = true,
                                reasoningContent = fullReasoning.takeIf { it.isNotEmpty() }?.toString()
                            )

                            if (choice.finish_reason.isNullOrEmpty().not()) {
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Stream processing error", e)
        } finally {
            repository.updateAssistantMessage(
                conversationId,
                assistantIndex,
                fullMessage.toString(),
                isStreaming = false,
                reasoningContent = fullReasoning.takeIf { it.isNotEmpty() }?.toString()
            )
            try {
                body.close()
            } catch (_: Exception) {}
        }
    }

    fun stopGeneration() {
        streamCall?.cancel()
        currentStreamJob?.cancel()
        _isLoading.value = false
        _isGeneratingImage.value = false
    }

    fun clearConversation() {
        viewModelScope.launch {
            try {
                repository.clearConversation(conversationId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "clearConversation error", e)
            }
        }
    }

    fun setModel(model: String) {
        _currentModel.value = model
        viewModelScope.launch {
            try {
                repository.updateConversationModel(conversationId, model)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "setModel error", e)
            }
        }
    }

    fun revokeMessage(index: Int) {
        viewModelScope.launch {
            try {
                repository.revokeMessage(conversationId, index)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "revokeMessage error", e)
            }
        }
    }

    fun getConversationId(): String = conversationId

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
    }
}

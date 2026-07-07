package com.aichat.app.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Message
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentModel = MutableStateFlow("gpt-3.5-turbo")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private var currentStreamJob: Job? = null
    private var streamCall: Call<ResponseBody>? = null

    init {
        loadMessages()
        loadConversationInfo()
        loadModels()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            repository.getMessagesForConversation(conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    private fun loadConversationInfo() {
        viewModelScope.launch {
            val conv = repository.getConversation(conversationId)
            conv?.let {
                _currentModel.value = it.model
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
        if (prompt.isBlank() || _isGeneratingImage.value) return

        _error.value = null

        viewModelScope.launch {
            repository.addUserMessage(conversationId, "生成图片: $prompt")
            val assistantIndex = repository.addAssistantMessage(conversationId, "正在生成图片...", isStreaming = true)
            _isGeneratingImage.value = true

            withContext(Dispatchers.IO) {
                try {
                    val result = repository.generateImage(prompt = prompt, n = 1, size = "1024x1024")
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
                }
            }
        }
    }

    private suspend fun processStreamResponse(body: ResponseBody, assistantIndex: Int) {
        val fullMessage = StringBuilder()
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

                        streamResponse.choices?.firstOrNull()?.delta?.content?.let { content ->
                            fullMessage.append(content)
                            repository.updateAssistantMessage(
                                conversationId,
                                assistantIndex,
                                fullMessage.toString(),
                                isStreaming = true
                            )
                        }

                        if (streamResponse.choices?.firstOrNull()?.finish_reason != null) {
                            break
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
                isStreaming = false
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
            repository.clearConversation(conversationId)
        }
    }

    fun setModel(model: String) {
        _currentModel.value = model
        viewModelScope.launch {
            repository.updateConversationModel(conversationId, model)
        }
    }

    fun getConversationId(): String = conversationId

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
    }
}

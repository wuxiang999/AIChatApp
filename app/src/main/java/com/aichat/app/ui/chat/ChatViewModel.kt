package com.aichat.app.ui.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Message
import com.aichat.app.data.repository.ChatRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

data class StreamResponse(
    val choices: List<StreamChoice>?,
    val error: String?
)

data class StreamChoice(
    val delta: StreamDelta?,
    val finish_reason: String?
)

data class StreamDelta(
    val content: String?
)

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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentStreamJob: Job? = null
    private var streamCall: Call<ResponseBody>? = null

    private val gson = Gson()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            repository.getMessagesForConversation(conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(text: String, model: String = "gpt-3.5-turbo") {
        if (text.isBlank() || _isLoading.value) return

        _error.value = null

        viewModelScope.launch {
            val userIndex = repository.addUserMessage(conversationId, text)
            val assistantIndex = repository.addAssistantMessage(conversationId, "", isStreaming = true)

            _isLoading.value = true

            withContext(Dispatchers.IO) {
                try {
                    val call = repository.sendMessageStream(
                        conversationId = conversationId,
                        message = text,
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
                        "错误: ${e.message}",
                        isStreaming = false
                    )
                    _error.value = e.message
                } finally {
                    _isLoading.value = false
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

                    try {
                        val streamResponse = gson.fromJson(jsonData, StreamResponse::class.java)
                        if (streamResponse.error != null) {
                            fullMessage.append("\n[错误: ${streamResponse.error}]")
                            repository.updateAssistantMessage(
                                conversationId,
                                assistantIndex,
                                fullMessage.toString(),
                                isStreaming = false
                            )
                            _error.value = streamResponse.error
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
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Parse error: $jsonData", e)
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
    }

    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation(conversationId)
        }
    }

    fun getConversationId(): String = conversationId

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
    }
}

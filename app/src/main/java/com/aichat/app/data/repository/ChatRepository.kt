package com.aichat.app.data.repository

import android.net.Uri
import android.util.Log
import com.aichat.app.data.local.ConversationDao
import com.aichat.app.data.local.MessageDao
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.model.Message
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.aichat.app.data.remote.ImageGenerationResponse
import com.aichat.app.data.remote.StreamResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val apiManager: ApiManager
) {
    private val gson = Gson()
    private val maxHistoryMessages = 50

    fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations()

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun createConversation(title: String = "新对话", model: String = "gpt-3.5-turbo"): Conversation {
        val id = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        val now = Date()
        val conversation = Conversation(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            model = model
        )
        conversationDao.insertConversation(conversation)
        return conversation
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversationById(conversationId)
        messageDao.deleteMessagesForConversation(conversationId)
    }

    suspend fun clearConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(updatedAt = Date(), title = "新对话"))
        }
    }

    suspend fun updateConversationTitle(conversationId: String, title: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(title = title, updatedAt = Date()))
        }
    }

    suspend fun getConversation(conversationId: String): Conversation? =
        conversationDao.getConversationById(conversationId)

    suspend fun addUserMessage(conversationId: String, content: String): Int {
        val maxIndex = messageDao.getMaxIndexForConversation(conversationId)
        val newIndex = (maxIndex ?: -1) + 1
        val message = Message(
            conversationId = conversationId,
            index = newIndex,
            role = "user",
            content = content,
            timestamp = Date()
        )
        messageDao.insertMessage(message)
        updateConversationTimestamp(conversationId)
        if (maxIndex == null || maxIndex < 0) {
            autoGenerateTitle(conversationId, content)
        }
        return newIndex
    }

    suspend fun addAssistantMessage(conversationId: String, content: String, isStreaming: Boolean = false): Int {
        val maxIndex = messageDao.getMaxIndexForConversation(conversationId)
        val newIndex = (maxIndex ?: -1) + 1
        val message = Message(
            conversationId = conversationId,
            index = newIndex,
            role = "assistant",
            content = content,
            timestamp = Date(),
            isStreaming = isStreaming
        )
        messageDao.insertMessage(message)
        updateConversationTimestamp(conversationId)
        return newIndex
    }

    suspend fun updateAssistantMessage(conversationId: String, index: Int, content: String, isStreaming: Boolean = false) {
        messageDao.updateMessageContent(conversationId, index, content, isStreaming)
        updateConversationTimestamp(conversationId)
    }

    private suspend fun updateConversationTimestamp(conversationId: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(updatedAt = Date()))
        }
    }

    private suspend fun autoGenerateTitle(conversationId: String, firstMessage: String) {
        val title = if (firstMessage.length > 20) {
            firstMessage.substring(0, 20) + "..."
        } else {
            firstMessage
        }
        updateConversationTitle(conversationId, title)
    }

    suspend fun getMessagesList(conversationId: String): List<Message> {
        return try {
            val flow = messageDao.getMessagesForConversation(conversationId)
            val result = flow.first()
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildChatMessages(messages: List<Message>): List<ChatMessage> {
        val limitedMessages = if (messages.size > maxHistoryMessages) {
            messages.takeLast(maxHistoryMessages)
        } else {
            messages
        }

        return limitedMessages.map { msg ->
            val content = parseMessageContent(msg)
            ChatMessage(role = msg.role, content = content)
        }
    }

    private fun parseMessageContent(message: Message): Any {
        return message.content
    }

    suspend fun sendMessage(
        conversationId: String,
        message: String,
        model: String = "gpt-3.5-turbo"
    ): Result<String> {
        return try {
            val historyMessages = getMessagesList(conversationId)
            val chatMessages = buildChatMessages(historyMessages)

            val request = ChatRequest(
                model = model,
                messages = chatMessages,
                stream = false
            )

            val response = apiManager.getApiService().chatCompletion(
                auth = apiManager.getAuthHeader(),
                request = request
            )

            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                val reply = response.choices?.firstOrNull()?.message?.content
                    ?: return Result.failure(Exception("空响应"))
                Result.success(reply)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessage error", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessageStream(
        conversationId: String,
        message: String,
        model: String = "gpt-3.5-turbo"
    ): Call<ResponseBody> {
        val historyMessages = getMessagesList(conversationId)
        val chatMessages = buildChatMessages(historyMessages)

        val request = ChatRequest(
            model = model,
            messages = chatMessages,
            stream = true
        )

        return apiManager.getApiService().chatCompletionStream(
            auth = apiManager.getAuthHeader(),
            request = request
        )
    }

    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }

    fun parseSseData(line: String): StreamResponse? {
        return try {
            gson.fromJson(line, StreamResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getModels(): List<String> {
        return try {
            val response = apiManager.getApiService().getModels(apiManager.getAuthHeader())
            response.data.map { it.id }.sorted()
        } catch (e: Exception) {
            Log.e("ChatRepository", "getModels error", e)
            emptyList()
        }
    }

    suspend fun generateImage(
        prompt: String,
        n: Int = 1,
        size: String = "1024x1024",
        model: String = "gpt-image-2",
        quality: String? = null,
        imagePath: String? = null
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val promptBody = prompt.toRequestBody("text/plain".toMediaType())
                val nBody = n.toString().toRequestBody("text/plain".toMediaType())
                val sizeBody = size.toRequestBody("text/plain".toMediaType())
                val modelBody = model.toRequestBody("text/plain".toMediaType())
                val qualityBody = quality?.toRequestBody("text/plain".toMediaType())

                val response: ImageGenerationResponse = if (imagePath != null && File(imagePath).exists()) {
                    val file = File(imagePath)
                    val requestFile = file.asRequestBody("image/*".toMediaType())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    apiManager.getApiService().editImage(
                        auth = apiManager.getAuthHeader(),
                        image = imagePart,
                        prompt = promptBody,
                        n = nBody,
                        size = sizeBody,
                        model = modelBody
                    )
                } else {
                    apiManager.getApiService().generateImage(
                        auth = apiManager.getAuthHeader(),
                        prompt = promptBody,
                        n = nBody,
                        size = sizeBody,
                        model = modelBody,
                        quality = qualityBody
                    )
                }

                if (response.error != null) {
                    Result.failure(Exception(response.error.message))
                } else {
                    val urls = response.data?.mapNotNull { data ->
                        data.url ?: data.b64_json?.let { "data:image/png;base64,$it" }
                    } ?: emptyList()
                    Result.success(urls)
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "generateImage error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testEndpoint(url: String, apiKey: String): Result<Int> {
        return apiManager.testEndpoint(url, apiKey)
    }
}

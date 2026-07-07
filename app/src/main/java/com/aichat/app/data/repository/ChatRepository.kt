package com.aichat.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.aichat.app.data.local.ConversationDao
import com.aichat.app.data.local.MessageDao
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.model.Message
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.aichat.app.data.remote.ContentPart
import com.aichat.app.data.remote.ImageGenerationRequest
import com.aichat.app.data.remote.ImageGenerationResponse
import com.aichat.app.data.remote.ImageUrlData
import com.aichat.app.data.remote.StreamResponse
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val apiManager: ApiManager,
    @ApplicationContext private val context: Context
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

    suspend fun updateConversationModel(conversationId: String, model: String) {
        val conversation = conversationDao.getConversationById(conversationId)
        conversation?.let {
            conversationDao.updateConversation(it.copy(model = model, updatedAt = Date()))
        }
    }

    suspend fun getConversation(conversationId: String): Conversation? =
        conversationDao.getConversationById(conversationId)

    suspend fun addUserMessage(conversationId: String, content: String, imageUris: List<String> = emptyList()): Int {
        val maxIndex = messageDao.getMaxIndexForConversation(conversationId)
        val newIndex = (maxIndex ?: -1) + 1
        val displayContent = if (imageUris.isNotEmpty()) {
            "$content\n[图片 x${imageUris.size}]"
        } else content
        val message = Message(
            conversationId = conversationId,
            index = newIndex,
            role = "user",
            content = displayContent,
            timestamp = Date(),
            imageUris = imageUris.joinToString(",")
        )
        messageDao.insertMessage(message)
        updateConversationTimestamp(conversationId)
        if (maxIndex == null || maxIndex < 0) {
            autoGenerateTitle(conversationId, content)
        }
        return newIndex
    }

    suspend fun addAssistantMessage(conversationId: String, content: String, isStreaming: Boolean = false, imageUrls: List<String> = emptyList(), reasoningContent: String? = null): Int {
        val maxIndex = messageDao.getMaxIndexForConversation(conversationId)
        val newIndex = (maxIndex ?: -1) + 1
        val message = Message(
            conversationId = conversationId,
            index = newIndex,
            role = "assistant",
            content = content,
            timestamp = Date(),
            isStreaming = isStreaming,
            imageUris = imageUrls.joinToString(",")
                .takeIf { it.isNotEmpty() },
            reasoningContent = reasoningContent
        )
        messageDao.insertMessage(message)
        updateConversationTimestamp(conversationId)
        return newIndex
    }

    suspend fun updateAssistantMessage(conversationId: String, index: Int, content: String, isStreaming: Boolean = false, imageUrls: List<String> = emptyList(), reasoningContent: String? = null) {
        messageDao.updateMessageContent(conversationId, index, content, isStreaming)
        if (imageUrls.isNotEmpty()) {
            messageDao.updateMessageImages(conversationId, index, imageUrls.joinToString(","))
        }
        if (reasoningContent != null) {
            messageDao.updateMessageReasoning(conversationId, index, reasoningContent)
        }
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

    suspend fun sendMessageStream(
        conversationId: String,
        currentMessage: String,
        imageUris: List<String> = emptyList(),
        model: String = "gpt-3.5-turbo"
    ): Call<ResponseBody> {
        val historyMessages = getMessagesList(conversationId)

        val displayContent = if (imageUris.isNotEmpty()) {
            "$currentMessage\n[图片 x${imageUris.size}]"
        } else currentMessage

        val userMessage = Message(
            conversationId = conversationId,
            index = (historyMessages.lastOrNull()?.index ?: -1) + 1,
            role = "user",
            content = displayContent,
            timestamp = Date(),
            imageUris = imageUris.joinToString(",")
        )

        val allMessages = historyMessages + userMessage
        val chatMessages = buildChatMessages(allMessages)

        Log.d("ChatRepository", "Sending ${chatMessages.size} messages to API, model: $model")

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

    private suspend fun getMessagesList(conversationId: String): List<Message> {
        return try {
            val flow = messageDao.getMessagesForConversation(conversationId)
            flow.first()
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
            val imageUris = msg.imageUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            if (imageUris.isNotEmpty()) {
                val parts = mutableListOf<ContentPart>()
                parts.add(ContentPart(type = "text", text = msg.content.replace("\n\\[图片 x\\d+\\]".toRegex(), "")))
                imageUris.forEach { uri ->
                    val base64 = try { uriToBase64(uri) } catch (_: Exception) { null }
                    if (base64 != null) {
                        parts.add(ContentPart(type = "image_url", image_url = ImageUrlData("data:image/jpeg;base64,$base64")))
                    }
                }
                ChatMessage(role = msg.role, content = parts)
            } else {
                ChatMessage(role = msg.role, content = msg.content)
            }
        }
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "uriToBase64 error", e)
            null
        }
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
        model: String = "dall-e-3",
        quality: String? = null
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageGenerationRequest(
                    prompt = prompt,
                    model = model,
                    n = n,
                    size = size,
                    quality = quality
                )

                val response: ImageGenerationResponse = apiManager.getApiService().generateImage(
                    auth = apiManager.getAuthHeader(),
                    request = request
                )

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

    suspend fun editImage(
        imageUri: String,
        prompt: String,
        n: Int = 1,
        size: String = "1024x1024",
        model: String = "gpt-image-2"
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(imageUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageBytes = inputStream?.readBytes() ?: return@withContext Result.failure(Exception("无法读取图片"))
                inputStream.close()

                val requestBody = okhttp3.RequestBody.create(
                    "image/*".toMediaType(),
                    imageBytes
                )
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    "image.png",
                    requestBody
                )

                val promptBody = prompt.toRequestBody("text/plain".toMediaType())
                val nBody = n.toString().toRequestBody("text/plain".toMediaType())
                val sizeBody = size.toRequestBody("text/plain".toMediaType())
                val modelBody = model.toRequestBody("text/plain".toMediaType())

                val response = apiManager.getApiService().editImage(
                    auth = apiManager.getAuthHeader(),
                    image = imagePart,
                    prompt = promptBody,
                    n = nBody,
                    size = sizeBody,
                    model = modelBody
                )

                if (response.error != null) {
                    Result.failure(Exception(response.error.message))
                } else {
                    val urls = response.data?.mapNotNull { data ->
                        data.url ?: data.b64_json?.let { "data:image/png;base64,$it" }
                    } ?: emptyList()
                    Result.success(urls)
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "editImage error", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testEndpoint(url: String, apiKey: String): Result<Int> {
        return apiManager.testEndpoint(url, apiKey)
    }
}

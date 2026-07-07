package com.aichat.app.data.repository

import android.util.Log
import com.aichat.app.data.local.ApiEndpointDao
import com.aichat.app.data.local.ConversationDao
import com.aichat.app.data.local.MessageDao
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.model.Message
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatResponse
import com.aichat.app.data.remote.HistoryMessage
import kotlinx.coroutines.flow.Flow
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
    private val apiEndpointDao: ApiEndpointDao,
    private val apiManager: ApiManager
) {

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

    suspend fun sendMessage(
        conversationId: String,
        message: String,
        model: String = "gpt-3.5-turbo",
        apiEndpoint: String? = null
    ): ChatResponse {
        return try {
            apiManager.getApiService().chat(
                sessionId = conversationId,
                message = message,
                model = model,
                apiEndpoint = apiEndpoint
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessage error", e)
            ChatResponse(success = false, error = e.message ?: "网络错误")
        }
    }

    fun sendMessageStream(
        conversationId: String,
        message: String,
        model: String = "gpt-3.5-turbo",
        apiEndpoint: String? = null
    ): Call<ResponseBody> {
        return apiManager.getApiService().chatStream(
            sessionId = conversationId,
            message = message,
            model = model,
            apiEndpoint = apiEndpoint
        )
    }

    suspend fun getModels(endpoint: String? = null): List<String> {
        return try {
            val response = apiManager.getApiService().getModels(endpoint = endpoint)
            response.models
        } catch (e: Exception) {
            Log.e("ChatRepository", "getModels error", e)
            emptyList()
        }
    }

    suspend fun getAnnouncement(): com.aichat.app.data.remote.AnnouncementResponse? {
        return try {
            apiManager.getApiService().getAnnouncement()
        } catch (e: Exception) {
            Log.e("ChatRepository", "getAnnouncement error", e)
            null
        }
    }

    suspend fun getApiEndpoints(): Map<String, String> {
        return try {
            val response = apiManager.getApiService().getEndpoints()
            response.endpoints
        } catch (e: Exception) {
            Log.e("ChatRepository", "getApiEndpoints error", e)
            emptyMap()
        }
    }

    fun parseSseData(line: String): String? {
        if (line.startsWith("data: ")) {
            return line.substring(6)
        }
        return null
    }

    suspend fun loadHistoryFromServer(conversationId: String): List<HistoryMessage> {
        return try {
            val response = apiManager.getApiService().getHistory(sessionId = conversationId)
            response.history
        } catch (e: Exception) {
            Log.e("ChatRepository", "loadHistory error", e)
            emptyList()
        }
    }

    suspend fun generateImage(
        prompt: String,
        n: Int = 1,
        size: String = "1024x1024",
        model: String = "gpt-image-2",
        apiEndpoint: String? = null
    ): com.aichat.app.data.remote.ImageGenerationResponse {
        return try {
            val promptBody = okhttp3.RequestBody.create(null, prompt)
            val nBody = okhttp3.RequestBody.create(null, n.toString())
            val sizeBody = okhttp3.RequestBody.create(null, size)
            val modelBody = okhttp3.RequestBody.create(null, model)
            val endpointBody = okhttp3.RequestBody.create(null, apiEndpoint ?: "")

            apiManager.getApiService().generateImage(
                prompt = promptBody,
                n = nBody,
                size = sizeBody,
                model = modelBody,
                apiEndpoint = endpointBody
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "generateImage error", e)
            com.aichat.app.data.remote.ImageGenerationResponse(
                success = false,
                error = e.message ?: "图片生成失败"
            )
        }
    }
}

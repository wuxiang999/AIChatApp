package com.aichat.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.aichat.app.data.local.AgentDao
import com.aichat.app.data.local.ConversationDao
import com.aichat.app.data.local.MessageDao
import com.aichat.app.data.model.Agent
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
import com.aichat.app.data.terminal.TerminalLogBuffer
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
    private val agentDao: AgentDao,
    private val apiManager: ApiManager,
    private val memoryRepository: MemoryRepository,
    private val skillsRepository: SkillsRepository,
    private val mcpRepository: McpRepository,
    private val terminal: TerminalLogBuffer,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val maxHistoryMessages = 50

    companion object {
        private const val TAG = "Chat"
    }

    private val presetAgents = listOf(
        Agent(
            id = "preset_general",
            name = "通用助手",
            description = "全能AI助手，回答各种问题",
            systemPrompt = "你是一个有用的AI助手，请用友好、专业的语气回答用户的问题。",
            isPreset = true,
            isSelected = true
        ),
        Agent(
            id = "preset_programmer",
            name = "程序员助手",
            description = "代码专家，帮你写代码、找Bug、优化性能",
            systemPrompt = "你是一位资深程序员助手，精通各种编程语言。请帮助用户编写、调试和优化代码。回答时提供清晰的代码示例和解释。",
            isPreset = true
        ),
        Agent(
            id = "preset_writer",
            name = "写作助手",
            description = "文案大师，帮你写文章、邮件、创意内容",
            systemPrompt = "你是一位专业的写作助手，擅长各种文体写作。请帮助用户撰写、润色和优化文章、邮件、文案等内容。语言要流畅优美，符合中文表达习惯。",
            isPreset = true
        ),
        Agent(
            id = "preset_translator",
            name = "翻译官",
            description = "多语言翻译专家，精准互译",
            systemPrompt = "你是一位专业翻译官，精通中英日等多种语言。请准确、自然地翻译用户提供的内容，保持原文的语气和风格。",
            isPreset = true
        ),
        Agent(
            id = "preset_teacher",
            name = "学习导师",
            description = "耐心的老师，帮你学习新知识",
            systemPrompt = "你是一位耐心的学习导师，擅长用通俗易懂的方式解释复杂概念。请帮助用户学习新知识，解答学习中的疑问，循序渐进地引导。",
            isPreset = true
        ),
        Agent(
            id = "preset_analyst",
            name = "数据分析师",
            description = "数据分析专家，帮你解读数据",
            systemPrompt = "你是一位专业的数据分析师，擅长数据分析和可视化。请帮助用户分析数据、解读趋势、提供洞察建议。",
            isPreset = true
        ),
        Agent(
            id = "preset_psychologist",
            name = "心理咨询师",
            description = "温暖的倾听者，给你情感支持",
            systemPrompt = "你是一位温暖的心理咨询师，善于倾听和共情。请以理解、包容的态度回应用户，给予情感支持和建设性建议。注意：你不能替代专业心理治疗。",
            isPreset = true
        ),
        Agent(
            id = "preset_marketer",
            name = "营销策划",
            description = "营销专家，帮你策划推广方案",
            systemPrompt = "你是一位资深营销策划专家，擅长品牌营销、内容策划和推广方案。请帮助用户制定营销策略、撰写营销文案、策划活动方案。",
            isPreset = true
        )
    )

    suspend fun initializeAgents() {
        val count = agentDao.getAgentCount()
        if (count == 0) {
            presetAgents.forEach { agent ->
                agentDao.insertAgent(agent)
            }
        }
    }

    fun getAllAgents(): Flow<List<Agent>> = agentDao.getAllAgents()

    suspend fun getSelectedAgent(): Agent? = agentDao.getSelectedAgent()

    suspend fun selectAgent(agentId: String) {
        agentDao.clearAllSelected()
        agentDao.selectAgent(agentId)
    }

    suspend fun addCustomAgent(name: String, description: String, systemPrompt: String): Agent {
        val id = UUID.randomUUID().toString().replace("-", "").substring(0, 32)
        val agent = Agent(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            isPreset = false
        )
        agentDao.insertAgent(agent)
        return agent
    }

    suspend fun deleteCustomAgent(agentId: String) {
        agentDao.deleteAgentById(agentId)
    }

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
        terminal.info(TAG, "发送请求 -> 模型=$model, 消息数=${chatMessages.size}")

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

    private suspend fun buildContextBlock(): String {
        val parts = mutableListOf<String>()

        // 记忆系统：长期记忆
        val memories = memoryRepository.getAllMemoriesOnce()
        if (memories.isNotEmpty()) {
            parts += buildString {
                append("【长期记忆】以下是关于用户的长期记忆，请在回复时参考：\n")
                memories.forEach { append("- ").append(it.content).append("\n") }
            }
            terminal.info(TAG, "注入记忆 ${memories.size} 条")
        }

        // 技能系统：可用技能清单
        val skills = skillsRepository.getEnabledSkills()
        if (skills.isNotEmpty()) {
            parts += buildString {
                append("【可用技能】以下是可调用的技能，请根据用户意图选用并展开对应提示词：\n")
                skills.forEach { s ->
                    append("- ").append(s.name).append("：").append(s.description)
                    if (s.promptTemplate.isNotBlank()) {
                        append("（模板：").append(s.promptTemplate.take(80)).append("…）")
                    }
                    append("\n")
                }
            }
            terminal.info(TAG, "注入技能 ${skills.size} 个")
        }

        // MCP 系统：外部资源
        val servers = mcpRepository.getEnabledServers()
        if (servers.isNotEmpty()) {
            parts += buildString {
                append("【MCP 外部资源】以下外部工具/数据源已接入，可按需调用：\n")
                servers.forEach { srv ->
                    append("- ").append(srv.name).append("（").append(srv.url).append("）\n")
                }
            }
            terminal.info(TAG, "注入 MCP 服务器 ${servers.size} 个")
        }

        return parts.joinToString("\n").trim()
    }

    private suspend fun buildChatMessages(messages: List<Message>): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        val selectedAgent = getSelectedAgent()
        if (selectedAgent != null && messages.isNotEmpty()) {
            result.add(ChatMessage(role = "system", content = selectedAgent.systemPrompt))
            Log.d("ChatRepository", "Agent system prompt applied: ${selectedAgent.name} (id=${selectedAgent.id})")
        } else {
            Log.d("ChatRepository", "No agent selected or no messages, system prompt skipped")
        }

        // 注入记忆 / 技能 / MCP 外部资源上下文
        val contextBlock = buildContextBlock()
        if (contextBlock.isNotBlank()) {
            result.add(ChatMessage(role = "system", content = contextBlock))
        }

        val limitedMessages = if (messages.size > maxHistoryMessages) {
            messages.takeLast(maxHistoryMessages)
        } else {
            messages
        }

        result.addAll(limitedMessages.map { msg ->
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
        })

        return result
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

    suspend fun revokeMessage(conversationId: String, index: Int) {
        messageDao.revokeMessage(conversationId, index)
    }

    suspend fun testEndpoint(url: String, apiKey: String): Result<Int> {
        return apiManager.testEndpoint(url, apiKey)
    }
}

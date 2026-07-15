package com.aichat.app.ui.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.agent.AgentLoop
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolRegistry
import com.aichat.app.mcp.McpClientManager
import com.aichat.app.permission.PermissionEffect
import com.aichat.app.permission.PermissionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentStatus(
    val phase: String = "idle",
    val currentTool: String? = null,
    val toolProgress: Int = 0,
    val totalTools: Int = 0,
    val tokensUsed: Int = 0,
    val elapsedMs: Long = 0
)

data class PendingPermission(
    val action: String,
    val resource: String,
    val args: String
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentLoop: AgentLoop,
    private val toolRegistry: ToolRegistry,
    private val permissionEngine: PermissionEngine,
    private val mcpClientManager: McpClientManager
) : ViewModel() {

    companion object {
        private const val TAG = "AgentViewModel"
    }

    data class UiState(
        val messages: List<ChatItem> = emptyList(),
        val isLoading: Boolean = false,
        val isThinking: Boolean = false,
        val toolCount: Int = 0,
        val error: String? = null,
        val availableTools: List<String> = emptyList(),
        val currentTask: String = "",
        val agentStatus: AgentStatus = AgentStatus()
    )

    data class ChatItem(
        val role: String,
        val content: String,
        val isStreaming: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _pendingPermission = MutableStateFlow<PendingPermission?>(null)
    val pendingPermission: StateFlow<PendingPermission?> = _pendingPermission.asStateFlow()

    private var permissionDeferred: CompletableDeferred<Boolean>? = null

    init {
        loadTools()
    }

    private fun loadTools() {
        val tools = toolRegistry.materialize(permissionEngine)
        val toolNames = tools.map { it.definition.name }
        _uiState.value = _uiState.value.copy(availableTools = toolNames)
        Log.d(TAG, "Loaded ${tools.size} tools: ${toolNames.joinToString(", ")}")

        viewModelScope.launch {
            try {
                mcpClientManager.discoverAllTools()
                val mcpTools = mcpClientManager.toTools()
                toolRegistry.registerAll(mcpTools)
                val allTools = toolRegistry.materialize(permissionEngine)
                _uiState.value = _uiState.value.copy(
                    availableTools = allTools.map { it.definition.name }
                )
                Log.d(TAG, "Registered ${mcpTools.size} MCP tools")
            } catch (e: Exception) {
                Log.e(TAG, "MCP discovery failed", e)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatItem(role = "user", content = text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            isThinking = true,
            error = null,
            toolCount = 0,
            agentStatus = AgentStatus(phase = "thinking")
        )

        viewModelScope.launch {
            try {
                val context = ToolContext(
                    sessionId = "agent_${System.currentTimeMillis()}",
                    conversationId = "agent_conv",
                    workingDirectory = "/storage/emulated/0/.Download/Python/APP/AIchat"
                )

                val config = AgentLoop.AgentConfig(
                    model = "gpt-4o",
                    systemPrompt = "你是一个 AI 编程代理助手",
                    tools = toolRegistry.materialize(permissionEngine)
                )

                var fullResponse = ""

                val result = agentLoop.run(
                    userMessage = text,
                    config = config,
                    context = context,
                    onStream = { chunk ->
                        fullResponse += chunk
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages.dropLast(1) + ChatItem(
                                role = "assistant",
                                content = fullResponse,
                                isStreaming = true
                            ),
                            isThinking = false
                        )
                    },
                    onToolCall = { toolName, args ->
                        val toolMsg = ChatItem(
                            role = "system",
                            content = "调用工具: $toolName(${args.keys.joinToString(", ")})"
                        )
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + toolMsg
                        )
                    },
                    onPermissionRequest = { action, resource, args ->
                        val deferred = CompletableDeferred<Boolean>()
                        permissionDeferred = deferred
                        _pendingPermission.value = PendingPermission(action, resource, args)
                        deferred.await()
                    },
                    onStatusChange = { status ->
                        _uiState.value = _uiState.value.copy(
                            agentStatus = status,
                            isThinking = status.phase == "thinking" || status.phase == "planning",
                            isLoading = status.phase != "idle" && status.phase != "done" && status.phase != "error"
                        )
                    }
                )

                if (fullResponse.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.dropLast(1) + ChatItem(
                            role = "assistant",
                            content = fullResponse,
                            isStreaming = false
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isThinking = false,
                    toolCount = _uiState.value.toolCount + result.toolCalls,
                    error = result.error
                )
            } catch (e: Exception) {
                Log.e(TAG, "Agent run failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isThinking = false,
                    error = "运行失败: ${e.message}",
                    agentStatus = AgentStatus(phase = "error")
                )
            }
        }
    }

    fun allowPermission() {
        permissionDeferred?.complete(true)
        _pendingPermission.value = null
    }

    fun allowPermissionAlways() {
        val pending = _pendingPermission.value ?: return
        permissionEngine.remember(pending.action, pending.resource, PermissionEffect.ALLOW)
        permissionDeferred?.complete(true)
        _pendingPermission.value = null
    }

    fun denyPermission() {
        permissionDeferred?.complete(false)
        _pendingPermission.value = null
    }

    fun clearChat() {
        _uiState.value = UiState(availableTools = _uiState.value.availableTools)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

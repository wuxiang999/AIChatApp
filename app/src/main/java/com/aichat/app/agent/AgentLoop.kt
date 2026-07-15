package com.aichat.app.agent

import android.util.Log
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.aichat.app.data.terminal.TerminalLogBuffer
import com.aichat.app.permission.PermissionEngine
import com.aichat.app.permission.PermissionResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentLoop @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val permissionEngine: PermissionEngine,
    private val apiManager: ApiManager,
    private val terminal: TerminalLogBuffer
) {
    companion object {
        private const val TAG = "AgentLoop"
        private const val MAX_ITERATIONS = 10
        private const val MAX_TOOL_RESULT_LENGTH = 10000
        private val READ_ONLY_TOOLS = setOf("read_file", "web_search", "memory_search")
    }

    private val gson = Gson()

    data class AgentConfig(
        val maxIterations: Int = MAX_ITERATIONS,
        val model: String = "gpt-4o",
        val systemPrompt: String = "",
        val tools: List<ITool> = emptyList(),
        val temperature: Double = 0.7
    )

    data class AgentResult(
        val response: String,
        val toolCalls: Int = 0,
        val iterations: Int = 0,
        val error: String? = null
    )

    data class AgentStatus(
        val phase: String = "idle",
        val currentTool: String? = null,
        val toolProgress: Int = 0,
        val totalTools: Int = 0,
        val tokensUsed: Int = 0,
        val elapsedMs: Long = 0
    )

    suspend fun run(
        userMessage: String,
        config: AgentConfig,
        context: ToolContext,
        onStream: (String) -> Unit = {},
        onToolCall: (String, Map<String, Any?>) -> Unit = { _, _ -> },
        onPermissionRequest: suspend (String, String, String) -> Boolean = { _, _, _ -> true },
        onStatusChange: (AgentStatus) -> Unit = {}
    ): AgentResult {
        val messages = mutableListOf<Map<String, Any?>>()
        var toolCallCount = 0
        var iteration = 0
        val startTime = System.currentTimeMillis()

        terminal.info("Agent", "Agent \u5F00\u59CB\u6267\u884C\u4EFB\u52A1: ${userMessage.take(100)}")

        val systemPrompt = buildSystemPrompt(config)
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        messages.add(mapOf("role" to "user", "content" to userMessage))

        onStatusChange(AgentStatus(phase = "thinking", elapsedMs = System.currentTimeMillis() - startTime))

        while (iteration < config.maxIterations) {
            iteration++
            terminal.info("Agent", "\u8FED\u4EE3 #$iteration \u5F00\u59CB")

            terminal.network("LLM", "\u8C03\u7528\u6A21\u578B ${config.model}")
            val response = callLlm(messages, config)

            if (response.error != null) {
                terminal.error("LLM", "\u6A21\u578B\u8C03\u7528\u5931\u8D25: ${response.error}")
            }

            when {
                response.toolCalls != null -> {
                    toolCallCount += response.toolCalls.size
                    terminal.info("Agent", "LLM \u8FD4\u56DE ${response.toolCalls.size} \u4E2A\u5DE5\u5177\u8C03\u7528")

                    val permittedCalls = mutableListOf<Triple<Map<String, Any?>, String, Map<String, Any?>>>()
                    var hasBlocked = false

                    for (toolCall in response.toolCalls) {
                        val toolName = extractToolName(toolCall) ?: continue
                        val rawArgs = (toolCall["function"] as? Map<*, *>)?.get("arguments")?.toString() ?: "{}"
                        val args = try {
                            gson.fromJson(rawArgs, Map::class.java) as? Map<String, Any?> ?: emptyMap()
                        } catch (e: Exception) {
                            emptyMap<String, Any?>()
                        }

                        terminal.toolCall("Agent", toolName, rawArgs.take(200))
                        Log.d(TAG, "Tool call: $toolName($args)")
                        onToolCall(toolName, args)

                        when (val perm = permissionEngine.evaluate(toolName, "*", rawArgs)) {
                            is PermissionResult.Denied -> {
                                terminal.warn("Permission", "\u6743\u9650\u88AB\u62D2: $toolName - ${perm.reason}")
                                messages.add(mapOf("role" to "tool", "tool_call_id" to (toolCall["id"]?:""), "content" to "\u274C \u6743\u9650\u4E0D\u8DB3: ${perm.reason}"))
                                hasBlocked = true
                                continue
                            }
                            is PermissionResult.NeedsApproval -> {
                                terminal.warn("Permission", "\u9700\u8981\u5BA1\u6279: $toolName")
                                val approved = onPermissionRequest(perm.action, perm.resource, rawArgs)
                                if (!approved) {
                                    terminal.warn("Permission", "\u7528\u6237\u62D2\u7EDD\u4E86\u6743\u9650\u8BF7\u6C42: ${perm.action}")
                                    messages.add(mapOf("role" to "tool", "tool_call_id" to (toolCall["id"]?:""), "content" to "\u274C \u7528\u6237\u62D2\u7EDD\u4E86\u6743\u9650\u8BF7\u6C42: ${perm.action}"))
                                    hasBlocked = true
                                    continue
                                }
                                terminal.info("Permission", "\u7528\u6237\u5DF2\u6279\u51C6: $toolName")
                            }
                            is PermissionResult.Allowed -> {}
                        }
                        permittedCalls.add(Triple(toolCall, toolName, args))
                    }

                    if (permittedCalls.isEmpty() && hasBlocked) continue

                    onStatusChange(AgentStatus(
                        phase = "planning", totalTools = permittedCalls.size, tokensUsed = response.tokensUsed,
                        elapsedMs = System.currentTimeMillis() - startTime
                    ))

                    val batches = if (permittedCalls.size > 1) planToolBatches(permittedCalls) else listOf(permittedCalls)
                    var executed = 0

                    for (batch in batches) {
                        val isConcurrent = batch.size > 1 && batch.all { isReadOnly(it.second) }

                        if (isConcurrent) {
                            coroutineScope {
                                val deferreds = batch.map { (tc, tn, ta) ->
                                    async {
                                        val r = executeWithRetry(tn, context, ta)
                                        val s = smartTruncate(toResultString(r))
                                        Triple(tc["id"]?.toString() ?: "", s, tn)
                                    }
                                }
                                for (d in deferreds) {
                                    val (id, resultStr, tn) = d.await()
                                    terminal.toolResult("Agent", tn, resultStr.take(200))
                                    messages.add(mapOf("role" to "tool", "tool_call_id" to id, "content" to resultStr))
                                    executed++
                                    onStatusChange(AgentStatus(phase = "executing", currentTool = tn, toolProgress = executed, totalTools = permittedCalls.size, tokensUsed = response.tokensUsed, elapsedMs = System.currentTimeMillis() - startTime))
                                }
                            }
                        } else {
                            for ((tc, tn, ta) in batch) {
                                onStatusChange(AgentStatus(phase = "executing", currentTool = tn, toolProgress = executed + 1, totalTools = permittedCalls.size, tokensUsed = response.tokensUsed, elapsedMs = System.currentTimeMillis() - startTime))
                                val r = executeWithRetry(tn, context, ta)
                                val s = smartTruncate(toResultString(r))
                                terminal.toolResult("Agent", tn, s.take(200))
                                messages.add(mapOf("role" to "tool", "tool_call_id" to (tc["id"]?:""), "content" to s))
                                executed++
                            }
                        }
                    }
                }

                response.content != null -> {
                    onStream(response.content!!)
                    onStatusChange(AgentStatus(phase = "done", tokensUsed = response.tokensUsed, elapsedMs = System.currentTimeMillis() - startTime))
                    terminal.info("Agent", "\u4EFB\u52A1\u5B8C\u6210\uFF0C\u8C03\u7528 $toolCallCount \u4E2A\u5DE5\u5177\uFF0C\u8FED\u4EE3 $iteration \u6B21")
                    return AgentResult(response = response.content!!, toolCalls = toolCallCount, iterations = iteration)
                }

                else -> {
                    if (iteration >= config.maxIterations) {
                        onStatusChange(AgentStatus(phase = "error", elapsedMs = System.currentTimeMillis() - startTime))
                        terminal.error("Agent", "\u8FED\u4EE3\u8D85\u51FA\u4E0A\u9650")
                        return AgentResult(response = "\u62B1\u6B49\uFF0C\u5904\u7406\u8D85\u65F6\u3002\u8BF7\u91CD\u8BD5\u6216\u7B80\u5316\u60A8\u7684\u8981\u6C42\u3002", toolCalls = toolCallCount, iterations = iteration, error = "Max iterations reached")
                    }
                    terminal.info("Agent", "\u6CA1\u6709\u5DE5\u5177\u8C03\u7528\u4E5F\u6CA1\u6709\u5185\u5BB9\uFF0C\u63D0\u793A\u6A21\u578B\u7EE7\u7EED")
                    messages.add(mapOf("role" to "user", "content" to "\u8BF7\u7EE7\u7EED\u3002"))
                }
            }
        }

        onStatusChange(AgentStatus(phase = "done", elapsedMs = System.currentTimeMillis() - startTime))
        terminal.info("Agent", "\u4EFB\u52A1\u5B8C\u6210\uFF0C\u8C03\u7528 $toolCallCount \u4E2A\u5DE5\u5177")
        return AgentResult(response = "\u5DF2\u5904\u7406\u5B8C\u6210\uFF0C\u5171\u8C03\u7528\u4E86 $toolCallCount \u4E2A\u5DE5\u5177\u3002", toolCalls = toolCallCount, iterations = iteration)
    }

    private fun extractToolName(toolCall: Map<String, Any?>): String? {
        return (toolCall["function"] as? Map<*, *>)?.get("name")?.toString()
    }

    private fun isReadOnly(name: String): Boolean = name in READ_ONLY_TOOLS

    private fun planToolBatches(
        calls: List<Triple<Map<String, Any?>, String, Map<String, Any?>>>
    ): List<List<Triple<Map<String, Any?>, String, Map<String, Any?>>>> {
        val batches = mutableListOf<MutableList<Triple<Map<String, Any?>, String, Map<String, Any?>>>>()
        var current = mutableListOf<Triple<Map<String, Any?>, String, Map<String, Any?>>>()
        var currentIsReadOnly = false
        for (call in calls) {
            val readOnly = isReadOnly(call.second)
            if (current.isEmpty()) {
                current.add(call)
                currentIsReadOnly = readOnly
            } else if (currentIsReadOnly && readOnly) {
                current.add(call)
            } else {
                batches.add(current)
                current = mutableListOf(call)
                currentIsReadOnly = readOnly
            }
        }
        if (current.isNotEmpty()) batches.add(current)
        return batches
    }

    private suspend fun executeWithRetry(toolName: String, context: ToolContext, args: Map<String, Any?>): ToolResult {
        var retries = 0
        while (true) {
            val result = toolRegistry.execute(toolName, context, args)
            if (result is ToolResult.Success || retries >= 1) return result
            retries++
            Log.w(TAG, "Retrying tool $toolName (attempt $retries)")
            terminal.warn("Agent", "\u91CD\u8BD5\u5DE5\u5177 $toolName (\u7B2C $retries \u6B21)")
        }
    }

    private fun toResultString(result: ToolResult): String {
        return when (result) {
            is ToolResult.Success -> result.data
            is ToolResult.Error -> "\u274C ${result.message}"
            is ToolResult.PermissionDenied -> "\u274C ${result.reason}"
        }
    }

    private fun smartTruncate(text: String): String {
        return when {
            text.length <= 500 -> text
            text.length <= 5000 -> {
                text.take(400) + "\n\n... (\u771F\u7565 ${text.length - 400} \u5B57\u7B26) ..."
            }
            else -> {
                val head = 500
                val tail = 500
                text.take(head) + "\n\n... (\u771F\u7565 ${text.length - head - tail} \u5B57\u7B26) ...\n\n" + text.takeLast(tail)
            }
        }
    }

    private fun buildSystemPrompt(config: AgentConfig): String {
        val sb = StringBuilder()

        sb.appendLine("\u4F60\u662F\u4E00\u4E2A AI \u7F16\u7A0B\u52A9\u624B\uFF0C\u8FD0\u884C\u5728 Android \u8BBE\u5907\u4E0A\u3002")
        sb.appendLine("\u4F60\u5177\u5907\u4EE5\u4E0B\u80FD\u529B\uFF1A\u8BFB\u5199\u6587\u4EF6\u3001\u641C\u7D22\u7F51\u7EDC\u3001\u6267\u884C\u547D\u4EE4\u3001\u5206\u6790\u4EE3\u7801\u3002")
        sb.appendLine("\u8BF7\u4F7F\u7528\u5DE5\u5177\u6765\u5B8C\u6210\u4EFB\u52A1\uFF0C\u6BCF\u4E00\u6B65\u9009\u62E9\u6700\u5408\u9002\u7684\u5DE5\u5177\u3002")
        sb.appendLine()

        if (config.tools.isNotEmpty()) {
            sb.appendLine("## \u53EF\u7528\u5DE5\u5177")
            sb.appendLine("\u4EE5\u4E0B\u662F\u4F60\u53EF\u4EE5\u8C03\u7528\u7684\u5DE5\u5177\uFF0C\u4F7F\u7528\u51FD\u6570\u8C03\u7528\u683C\u5F0F\uFF1A")
            sb.appendLine()
            for (tool in config.tools) {
                val schema = tool.definition.toJsonSchema()
                val json = gson.toJson(schema)
                sb.appendLine(json)
                sb.appendLine()
            }
        }

        sb.appendLine("## \u884C\u4E3A\u51C6\u5219")
        sb.appendLine("1. \u5206\u6790\u7528\u6237\u9700\u6C42\uFF0C\u89C4\u5212\u6240\u9700\u6B65\u9AA4")
        sb.appendLine("2. \u6BCF\u6B21\u8C03\u7528\u4E00\u4E2A\u5DE5\u5177\uFF0C\u89C2\u5BDF\u7ED3\u679C\u540E\u518D\u51B3\u5B9A\u4E0B\u4E00\u6B65")
        sb.appendLine("3. \u8BFB\u53D6\u6587\u4EF6\u524D\u5148\u67E5\u770B\u76EE\u5F55\u7ED3\u6784")
        sb.appendLine("4. \u5199\u5165\u91CD\u8981\u6587\u4EF6\u524D\u5148\u5907\u4EFD")
        sb.appendLine("5. \u6267\u884C\u5B8C\u6210\u540E\u7528\u81EA\u7136\u8BED\u8A00\u603B\u7ED3\u7ED3\u679C")

        return sb.toString()
    }

    private suspend fun callLlm(
        messages: List<Map<String, Any?>>,
        config: AgentConfig
    ): LlmResponse {
        return withContext(Dispatchers.IO) {
            try {
                val chatMessages = messages.map { msg ->
                    val content = msg["content"]?.toString() ?: ""
                    val role = msg["role"]?.toString() ?: "user"
                    val toolCallId = msg["tool_call_id"]?.toString()
                    ChatMessage(role = role, content = content, tool_call_id = toolCallId)
                }

                val toolSchemas = if (config.tools.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    config.tools.map { it.definition.toJsonSchema() as Map<String, Any?> }
                } else null

                val request = ChatRequest(
                    model = config.model,
                    messages = chatMessages,
                    stream = false,
                    tools = toolSchemas,
                    tool_choice = if (toolSchemas != null) "auto" else null,
                    parallel_tool_calls = true
                )

                val response = apiManager.getApiService().chatCompletion(
                    auth = apiManager.getAuthHeader(),
                    request = request
                )

                val choice = response.choices?.firstOrNull()
                val message = choice?.message
                val tokensUsed = response.usage?.total_tokens ?: 0

                if (response.error != null) {
                    return@withContext LlmResponse(null, null, error = response.error.message, tokensUsed = tokensUsed)
                }

                if (message == null) {
                    return@withContext LlmResponse(null, null, tokensUsed = tokensUsed)
                }

                if (message.tool_calls != null && message.tool_calls.isNotEmpty()) {
                    val calls = message.tool_calls.map { tc ->
                        mapOf(
                            "id" to tc.id,
                            "type" to "function",
                            "function" to mapOf(
                                "name" to tc.function.name,
                                "arguments" to tc.function.arguments
                            )
                        )
                    }
                    return@withContext LlmResponse(content = null, toolCalls = calls, tokensUsed = tokensUsed)
                }

                LlmResponse(content = message.content, toolCalls = null, tokensUsed = tokensUsed)
            } catch (e: Exception) {
                Log.e(TAG, "LLM call failed", e)
                LlmResponse(null, null, error = e.message)
            }
        }
    }

    data class LlmResponse(
        val content: String?,
        val toolCalls: List<Map<String, Any?>>?,
        val error: String? = null,
        val tokensUsed: Int = 0
    )
}

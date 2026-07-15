package com.aichat.app.agent

/**
 * Interface for all executable tools.
 * Inspired by OpenCode's Tool system and Hermes' tool registry pattern.
 */
interface ITool {
    val definition: ToolDefinition

    /**
     * Execute the tool with given arguments and context.
     * @return Result string (usually JSON) that will be sent back to the LLM
     */
    suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult

    /**
     * Check if this tool is available in the current environment.
     * Similar to Hermes' check_fn with TTL caching.
     */
    fun isAvailable(): Boolean = true
}

/**
 * Result of a tool execution.
 */
sealed class ToolResult {
    data class Success(val data: String) : ToolResult()
    data class Error(val message: String, val code: Int = 500) : ToolResult()
    data class PermissionDenied(val action: String, val reason: String = "权限不足") : ToolResult()
}

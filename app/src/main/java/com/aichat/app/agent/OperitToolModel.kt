package com.aichat.app.agent

data class ToolParameter(val name: String, val value: String)

data class AITool(
    val name: String,
    val parameters: List<ToolParameter> = emptyList(),
    val description: String = ""
)

data class ToolInvocation(
    val tool: AITool,
    val rawText: String,
    val responseLocation: IntRange
)

/**
 * Operit's internal tool result type, distinct from ITool.ToolResult.
 */
data class OperitToolResult(
    val toolName: String,
    val success: Boolean,
    val result: String = "",
    val error: String? = null
)

data class ToolValidationResult(val valid: Boolean, val errorMessage: String = "")

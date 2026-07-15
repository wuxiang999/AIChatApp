package com.aichat.app.agent

import com.aichat.app.agent.OperitToolResults.ToolResultData

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

data class ToolResult(
    val toolName: String,
    val success: Boolean,
    val result: ToolResultData,
    val error: String? = null
)

data class ToolValidationResult(val valid: Boolean, val errorMessage: String = "")

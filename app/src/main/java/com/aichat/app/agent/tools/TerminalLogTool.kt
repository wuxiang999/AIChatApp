package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import com.aichat.app.data.terminal.TerminalLogBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalLogTool @Inject constructor(
    private val terminal: TerminalLogBuffer
) : ITool {

    override val definition = ToolDefinition(
        name = "terminal_log",
        description = "\u67E5\u770B\u7CFB\u7EDF\u7EC8\u7AEF\u65E5\u5FD7\u3002\u53EF\u7528\u4E8E\u8C03\u8BD5\u3001\u67E5\u770B\u5386\u53F2\u64CD\u4F5C\u8BB0\u5F55\u3001\u4E86\u89E3\u4E4B\u524D\u53D1\u751F\u4E86\u4EC0\u4E48",
        parameters = listOf(
            ParameterSchema("query", ParameterType.STRING, "\u641C\u7D22\u5173\u952E\u8BCD\uFF08\u53EF\u9009\uFF09", required = false),
            ParameterSchema("level", ParameterType.STRING, "\u8FC7\u6EE4\u7EA7\u522B: DEBUG/INFO/WARN/ERROR/TOOL_CALL",
                required = false, enumValues = listOf("DEBUG", "INFO", "WARN", "ERROR", "TOOL_CALL")),
            ParameterSchema("limit", ParameterType.INTEGER, "\u8FD4\u56DE\u6761\u6570\uFF08\u9ED8\u8BA420\uFF09", required = false),
            ParameterSchema("source", ParameterType.STRING, "\u6765\u6E90: system/agent/tool/network/memory",
                required = false)
        ),
        action = "terminal_log",
        toolset = "core",
        emoji = ""
    )

    override fun isAvailable(): Boolean = true

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val query = args["query"]?.toString()
        val level = args["level"]?.toString()?.let {
            try { TerminalLogBuffer.LogLevel.valueOf(it) } catch (_: Exception) { null }
        }
        val limit = (args["limit"] as? Number)?.toInt() ?: 20
        val source = args["source"]?.toString()

        val logs = when {
            query != null -> terminal.search(query)
            level != null -> terminal.getLogsByLevel(level)
            source != null -> terminal.getLogsBySource(source)
            else -> emptyList()
        }

        val result = logs.takeLast(limit).joinToString("\n") { entry ->
            "[${entry.formattedTime()}] [${entry.level}] [${entry.tag}] ${entry.message}"
        }

        val summary = "\n---\n\u5171 ${logs.size} \u6761\u5339\u914D\uFF0C\u663E\u793A ${minOf(limit, logs.size)} \u6761"
        return ToolResult.Success(
            result.ifEmpty { "\u7EC8\u7AEF\u65E5\u5FD7\u4E3A\u7A7A\u6216\u6CA1\u6709\u5339\u914D\u7684\u6761\u76EE" } + summary
        )
    }
}

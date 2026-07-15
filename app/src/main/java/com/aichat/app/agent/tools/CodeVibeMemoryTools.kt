package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryMemoryTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "query_memory",
        description = "查询长期记忆。搜索之前保存的知识、用户偏好、项目信息等",
        parameters = listOf(
            ParameterSchema("query", ParameterType.STRING, "搜索关键词", required = true),
            ParameterSchema("limit", ParameterType.INTEGER, "返回条数（默认5）", required = false)
        ),
        action = "query_memory",
        toolset = "memory"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val query = args["query"]?.toString() ?: return ToolResult.Error("Missing query parameter")
        val limit = (args["limit"] as? Number)?.toInt() ?: 5

        val memoriesDir = java.io.File(context.workingDirectory, ".memories")
        if (!memoriesDir.exists()) return ToolResult.Success("No memories found")

        val results = mutableListOf<Pair<String, String>>()
        memoriesDir.listFiles()?.filter { it.name.endsWith(".md") }?.forEach { file ->
            val content = try { file.readText() } catch (_: Exception) { return@forEach }
            val title = file.nameWithoutExtension
            if (content.contains(query, ignoreCase = true) ||
                title.contains(query, ignoreCase = true)) {
                results.add(title to content)
            }
        }

        if (results.isEmpty()) return ToolResult.Success("No relevant memories found")

        val sorted = results.sortedByDescending { (_, content) ->
            content.count { it.lowercase().contains(query.lowercase()) }
        }.take(limit)

        val output = sorted.joinToString("\n\n---\n\n") { (title, content) ->
            "Title: $title\nContent: ${content.take(2000)}"
        }
        return ToolResult.Success("Found ${sorted.size} memories:\n\n$output")
    }
}

@Singleton
class CreateMemoryTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "create_memory",
        description = "创建长期记忆。保存重要的项目信息、用户偏好、代码决策等",
        parameters = listOf(
            ParameterSchema("title", ParameterType.STRING, "记忆标题", required = true),
            ParameterSchema("content", ParameterType.STRING, "记忆内容", required = true),
            ParameterSchema("tags", ParameterType.STRING, "标签（逗号分隔）", required = false)
        ),
        action = "create_memory",
        toolset = "memory"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val title = args["title"]?.toString() ?: return ToolResult.Error("Missing title parameter")
        val content = args["content"]?.toString() ?: return ToolResult.Error("Missing content parameter")
        val tags = args["tags"]?.toString() ?: ""

        val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val memoriesDir = java.io.File(context.workingDirectory, ".memories")
        memoriesDir.mkdirs()

        val file = java.io.File(memoriesDir, "$sanitizedTitle.md")
        val entry = buildString {
            appendLine("---")
            appendLine("title: $title")
            appendLine("created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            if (tags.isNotBlank()) appendLine("tags: $tags")
            appendLine("---")
            appendLine()
            append(content.trim())
        }

        file.writeText(entry)
        return ToolResult.Success("Memory saved: $title -> ${file.absolutePath}")
    }
}

@Singleton
class DeleteMemoryTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "delete_memory",
        description = "删除指定的长期记忆",
        parameters = listOf(
            ParameterSchema("title", ParameterType.STRING, "记忆标题", required = true)
        ),
        action = "delete_memory",
        toolset = "memory"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val title = args["title"]?.toString() ?: return ToolResult.Error("Missing title parameter")
        val sanitizedTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val file = java.io.File(context.workingDirectory, ".memories/$sanitizedTitle.md")
        if (!file.exists()) return ToolResult.Error("Memory not found: $title")
        file.delete()
        return ToolResult.Success("Memory deleted: $title")
    }
}

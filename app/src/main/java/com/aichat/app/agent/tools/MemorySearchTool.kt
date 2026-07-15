package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import com.aichat.app.data.model.Memory
import com.aichat.app.data.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemorySearchTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ITool {

    override val definition = ToolDefinition(
        name = "memory_search",
        description = "搜索长期记忆。在回答用户问题前可先搜索相关记忆。支持按关键词和分类筛选。",
        parameters = listOf(
            ParameterSchema("query", ParameterType.STRING, "搜索关键词", required = true),
            ParameterSchema("limit", ParameterType.INTEGER, "返回条数(默认5)", required = false),
            ParameterSchema("category", ParameterType.STRING, "分类筛选(fact/preference/identity/knowledge/task)", required = false)
        ),
        action = "memory_search",
        toolset = "core",
        emoji = ""
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val query = args["query"]?.toString() ?: return ToolResult.Error("缺少 query 参数")
        val limit = (args["limit"] as? Number)?.toInt() ?: 5
        val category = args["category"]?.toString()

        return try {
            val memories = if (category != null) {
                memoryRepository.getMemoriesByCategory(category)
            } else {
                memoryRepository.searchMemories(query)
            }

            memories.forEach { memoryRepository.recordAccess(it.id) }

            if (memories.isEmpty()) {
                ToolResult.Success("未找到相关记忆")
            } else {
                val result = memories.take(limit).joinToString("\n") { mem ->
                    "[${mem.importance}★][${mem.category}] ${mem.content}"
                }
                val total = memories.size
                val note = if (total > limit) "（共$total条，仅显示前$limit条）" else ""
                ToolResult.Success("找到 $total 条相关记忆：\n$result${note}")
            }
        } catch (e: Exception) {
            ToolResult.Error("搜索记忆失败: ${e.message}")
        }
    }
}

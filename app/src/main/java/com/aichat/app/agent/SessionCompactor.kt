package com.aichat.app.agent

import android.util.Log
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCompactor @Inject constructor(
    private val apiManager: ApiManager
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "SessionCompactor"
        private const val DEFAULT_KEEP_RECENT = 8
        private const val DEFAULT_MODEL = "gpt-4o-mini"
    }

    data class CompactionResult(
        val summary: String,
        val preservedMessages: Int,
        val compressedTokens: Int
    )

    suspend fun compact(
        messages: List<ChatMessage>,
        keepRecent: Int = DEFAULT_KEEP_RECENT,
        model: String = DEFAULT_MODEL
    ): CompactionResult {
        if (messages.size <= keepRecent) {
            return CompactionResult(
                summary = "",
                preservedMessages = messages.size,
                compressedTokens = 0
            )
        }

        val toCompress = messages.dropLast(keepRecent)
        val preserve = messages.takeLast(keepRecent)

        val oldTokens = toCompress.sumOf { estimateTokens(it.content.toString()) }

        val summary = if (toCompress.isNotEmpty()) {
            compressMessages(toCompress, model)
        } else ""

        val summaryTokens = estimateTokens(summary)

        return CompactionResult(
            summary = summary,
            preservedMessages = preserve.size,
            compressedTokens = oldTokens - summaryTokens
        )
    }

    private suspend fun compressMessages(
        messages: List<ChatMessage>,
        model: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val dialogue = messages.joinToString("\n") { msg ->
                "[${msg.role}] ${msg.content}"
            }

            val compressPrompt = """
            你是一个对话压缩助手。请将以下对话压缩为结构化摘要，保留所有关键信息。
            
            对话内容：
            $dialogue
            
            请按以下格式输出：
            ## Objective
            对话的核心目标/用户请求
            
            ## Key Decisions
            - 已做出的重要决策
            
            ## Findings
            - 发现的关键信息/搜索结果
            
            ## Pending
            - 待办事项/未完成的任务
            
            ## Relevant Files
            - 涉及的文件或代码路径
            """.trimIndent()

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage("system", "你是一个专业的对话压缩助手，擅长保留关键信息的同时大幅缩减篇幅。使用中文输出。"),
                    ChatMessage("user", compressPrompt)
                ),
                temperature = 0.3f,
                max_tokens = 1024,
                stream = false
            )

            val response = apiManager.getApiService().chatCompletion(
                auth = apiManager.getAuthHeader(),
                request = request
            )

            response.choices?.firstOrNull()?.message?.content
                ?: "对话摘要生成失败"
        } catch (e: Exception) {
            Log.e(TAG, "压缩对话失败", e)
            "[摘要生成失败: ${e.message}]"
        }
    }

    fun estimateTokens(text: String): Int {
        var tokens = 0
        for (char in text) {
            tokens += when {
                char.code in 0x4E00..0x9FFF || char.code in 0x3400..0x4DBF -> 2
                char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' -> 1
                else -> 1
            }
        }
        return (tokens * 0.75).toInt().coerceAtLeast(1)
    }

    fun buildSummaryBlock(
        previousSummary: String?,
        newCompaction: CompactionResult
    ): String {
        val sb = StringBuilder()

        if (!previousSummary.isNullOrBlank()) {
            sb.appendLine("## 历史摘要")
            sb.appendLine(previousSummary)
            sb.appendLine()
        }

        sb.appendLine("## 新的对话摘要")
        sb.appendLine(newCompaction.summary)

        return sb.toString()
    }
}

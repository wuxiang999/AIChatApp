package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web search tool using the web.
 * Executes search queries and returns results.
 *
 * Inspired by OpenCode's websearch tool + Hermes' web_search tool.
 */
@Singleton
class WebSearchTool @Inject constructor() : ITool {

    override val definition = ToolDefinition(
        name = "web_search",
        description = "搜索网络信息。用于获取实时信息、查文档、找代码示例等",
        parameters = listOf(
            ParameterSchema("query", ParameterType.STRING, "搜索关键词", required = true),
            ParameterSchema("count", ParameterType.INTEGER, "返回结果数量（默认5）", required = false)
        ),
        action = "web_search",
        toolset = "core",
        emoji = ""
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val query = args["query"]?.toString() ?: return ToolResult.Error("缺少 query 参数")
        val count = (args["count"] as? Number)?.toInt() ?: 5

        return try {
            val escapedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$escapedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 Android AIChatApp/2.4")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return ToolResult.Error("搜索无响应")

            // Simple HTML parsing to extract search results
            val results = parseResults(html, count)

            if (results.isEmpty()) {
                ToolResult.Success("未找到相关结果")
            } else {
                ToolResult.Success(results.joinToString("\n\n"))
            }
        } catch (e: Exception) {
            android.util.Log.e("WebSearchTool", "搜索失败", e)
            ToolResult.Error("搜索失败: ${e.message}")
        }
    }

    private fun parseResults(html: String, max: Int): List<String> {
        val results = mutableListOf<String>()
        var idx = 0

        // DuckDuckGo HTML result extraction
        while (true) {
            val resultStart = html.indexOf("class=\"result__body\"", idx)
            if (resultStart == -1 || results.size >= max) break

            val titleStart = html.indexOf("class=\"result__title\"", resultStart)
            val snippetStart = html.indexOf("class=\"result__snippet\"", resultStart)

            if (titleStart == -1) { idx = resultStart + 1; continue }

            val titleLinkStart = html.indexOf("<a ", titleStart)
            val titleHrefEnd = if (titleLinkStart != -1) {
                val hrefStart = html.indexOf("href=\"", titleLinkStart)
                if (hrefStart != -1) {
                    val hrefEnd = html.indexOf("\"", hrefStart + 6)
                    if (hrefEnd != -1) html.substring(hrefStart + 6, hrefEnd) else ""
                } else ""
            } else ""

            val titleTextStart = html.indexOf(">", titleLinkStart)
            val titleTextEnd = html.indexOf("</a>", if (titleTextStart != -1) titleTextStart else titleStart)
            val title = if (titleTextStart != -1 && titleTextEnd != -1) {
                html.substring(titleTextStart + 1, titleTextEnd)
                    .replace("<[^>]*>".toRegex(), "")
                    .trim()
            } else ""

            val snippet = if (snippetStart != -1) {
                val snippetClose = html.indexOf("</a>", snippetStart)
                val snippetEnd = html.indexOf("</div>", snippetStart)
                val end = if (snippetClose != -1 && snippetClose < snippetEnd) snippetClose else snippetEnd
                if (end != -1) {
                    html.substring(snippetStart, end)
                        .replace("<[^>]*>".toRegex(), "")
                        .trim()
                } else ""
            } else ""

            if (title.isNotEmpty()) {
                results.add(buildString {
                    append("$title\n")
                    append("$titleHrefEnd\n")
                    if (snippet.isNotEmpty()) append("$snippet")
                })
            }

            idx = if (snippetStart != -1) snippetStart + 1 else resultStart + 1
        }

        return results
    }
}

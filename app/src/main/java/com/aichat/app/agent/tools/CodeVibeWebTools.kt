package com.aichat.app.agent.tools

import android.util.Log
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CodeVibeWebTools"

@Singleton
class VisitWebTool @Inject constructor() : ITool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override val definition = ToolDefinition(
        name = "visit_web",
        description = "访问网页并获取内容。用于在线文档查阅、API文档浏览等",
        parameters = listOf(
            ParameterSchema("url", ParameterType.STRING, "要访问的完整URL", required = true)
        ),
        action = "visit_web",
        toolset = "web"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val url = args["url"]?.toString() ?: return ToolResult.Error("Missing url parameter")

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) AIChatApp/2.4")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return ToolResult.Error("Empty response")
            val contentType = response.header("Content-Type", "text/html") ?: "text/html"
            val content = extractText(body, contentType)
            val truncated = if (content.length > 50000)
                content.take(50000) + "\n\n...(truncated, ${content.length} total chars)"
            else content

            val result = buildString {
                appendLine("URL: $url")
                appendLine("Status: ${response.code} ${response.message}")
                appendLine("Content-Type: $contentType")
                appendLine()
                append(truncated)
            }
            ToolResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to visit web", e)
            ToolResult.Error("Failed to visit URL: ${e.message}")
        }
    }

    private fun extractText(html: String, contentType: String): String {
        return if (contentType.contains("text/html")) {
            html.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        } else html
    }
}

@Singleton
class HttpRequestTool @Inject constructor() : ITool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override val definition = ToolDefinition(
        name = "http_request",
        description = "发送HTTP请求（GET/POST/PUT/DELETE等）。用于调用API、提交数据",
        parameters = listOf(
            ParameterSchema("url", ParameterType.STRING, "请求URL", required = true),
            ParameterSchema("method", ParameterType.STRING, "HTTP方法（GET/POST/PUT/DELETE/PATCH）", required = false),
            ParameterSchema("headers", ParameterType.OBJECT, "请求头JSON对象", required = false),
            ParameterSchema("body", ParameterType.STRING, "请求体内容", required = false),
            ParameterSchema("content_type", ParameterType.STRING, "Content-Type（默认application/json）", required = false)
        ),
        action = "http_request",
        toolset = "web"
    )

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val url = args["url"]?.toString() ?: return ToolResult.Error("Missing url parameter")
        val method = (args["method"]?.toString() ?: "GET").uppercase()
        val contentType = args["content_type"]?.toString() ?: "application/json"
        val bodyStr = args["body"]?.toString()

        return try {
            val builder = Request.Builder().url(url)
            builder.header("User-Agent", "Mozilla/5.0 (Android) AIChatApp/2.4")

            val rawHeaders = args["headers"]
            if (rawHeaders is Map<*, *>) {
                for ((key, value) in rawHeaders) {
                    if (key != null && value != null)
                        builder.header(key.toString(), value.toString())
                }
            }

            when (method) {
                "GET" -> builder.get()
                "DELETE" -> builder.delete()
                else -> {
                    val requestBody = (bodyStr ?: "").toRequestBody(contentType.toMediaTypeOrNull())
                    when (method) {
                        "POST" -> builder.post(requestBody)
                        "PUT" -> builder.put(requestBody)
                        "PATCH" -> builder.patch(requestBody)
                        else -> builder.get()
                    }
                }
            }

            val response = client.newCall(builder.build()).execute()
            val body = response.body?.string() ?: ""
            val truncated = if (body.length > 50000)
                body.take(50000) + "\n\n...(truncated, ${body.length} total chars)"
            else body

            val headers = response.headers.joinToString("\n") { "${it.first}: ${it.second}" }
            val result = buildString {
                appendLine("Status: ${response.code} ${response.message}")
                appendLine("Headers:")
                appendLine(headers)
                appendLine()
                append(truncated)
            }
            ToolResult.Success(result.trim())
        } catch (e: Exception) {
            Log.e(TAG, "HTTP request failed", e)
            ToolResult.Error("HTTP request failed: ${e.message}")
        }
    }
}

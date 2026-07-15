package com.aichat.app.mcp

import android.util.Log
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP (Model Context Protocol) client for AIChatApp.
 *
 * Supports:
 * - Stdio transport (local subprocess)
 * - HTTP/SSE transport (remote server)
 * - Tool discovery via tools/list
 * - Tool calling via tools/call
 *
 * Inspired by:
 * - Hermes Agent: tools/mcp_tool.py (5833 lines)
 * - OpenClaw: src/mcp/ (full MCP server + client)
 * - OpenCode: MCP.Service
 */
@Singleton
class McpClientManager @Inject constructor() {

    companion object {
        private const val TAG = "McpClient"
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }

    data class McpServerConfig(
        val name: String,
        val command: String? = null,      // for stdio transport
        val args: List<String> = emptyList(),
        val url: String? = null,          // for HTTP/SSE transport
        val headers: Map<String, String> = emptyMap(),
        val enabled: Boolean = true
    )

    data class McpToolSchema(
        val name: String,
        val description: String,
        val inputSchema: Map<String, Any?>? = null
    )

    private val gson = Gson()
    private val servers = mutableListOf<McpServerConfig>()
    private val discoveredTools = ConcurrentHashMap<String, List<McpToolSchema>>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Configure MCP servers.
     */
    fun configure(servers: List<McpServerConfig>) {
        this.servers.clear()
        this.servers.addAll(servers.filter { it.enabled })
        Log.d(TAG, "Configured ${this.servers.size} MCP servers")
    }

    /**
     * Discover tools from all configured MCP servers.
     * Returns tools prefixed with "mcp_<server>_" to avoid naming conflicts.
     */
    suspend fun discoverAllTools(): List<McpToolSchema> {
        discoveredTools.clear()
        val allTools = mutableListOf<McpToolSchema>()

        for (server in servers) {
            val tools = discoverServerTools(server)
            discoveredTools[server.name] = tools
            allTools.addAll(tools)
        }

        Log.d(TAG, "Discovered ${allTools.size} MCP tools total")
        return allTools
    }

    /**
     * Discover tools from a single MCP server using tools/list.
     */
    private suspend fun discoverServerTools(server: McpServerConfig): List<McpToolSchema> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = """{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}"""
                val response = when {
                    server.url != null -> httpListTools(server.url, server.headers, requestBody)
                    server.command != null -> stdioListTools(server.command, server.args, requestBody)
                    else -> throw IllegalArgumentException("MCP server ${server.name}: no url or command")
                }

                val tools = mutableListOf<McpToolSchema>()
                val json = JsonParser.parseString(response).asJsonObject
                val result = json?.get("result")?.asJsonObject
                val toolArray = result?.get("tools")?.asJsonArray

                if (toolArray != null) {
                    for (element in toolArray) {
                        val obj = element.asJsonObject
                        val name = obj.get("name")?.asString ?: continue
                        val desc = obj.get("description")?.asString ?: ""
                        val schema = obj.get("inputSchema")?.asJsonObject?.let {
                            gson.fromJson(it, Map::class.java) as? Map<String, Any?>
                        }

                        tools.add(McpToolSchema(
                            name = name,
                            description = desc,
                            inputSchema = schema
                        ))
                    }
                }

                Log.d(TAG, "Server '${server.name}': discovered ${tools.size} tools")
                tools
            } catch (e: Exception) {
                Log.e(TAG, "Failed to discover tools from '${server.name}'", e)
                emptyList()
            }
        }
    }

    /**
     * Convert MCP tool schemas to ITool instances.
     * Each tool is prefixed with "mcp_<serverName>_" to avoid naming conflicts.
     */
    fun toTools(serverNameFilter: String? = null): List<ITool> {
        val result = mutableListOf<ITool>()

        for ((serverName, tools) in discoveredTools) {
            if (serverNameFilter != null && serverName != serverNameFilter) continue

            for (schema in tools) {
                val prefixedName = "mcp_${serverName}_${schema.name}"
                result.add(McpToolWrapper(
                    definition = buildDefinition(prefixedName, schema),
                    serverName = serverName,
                    toolName = schema.name,
                    manager = this
                ))
            }
        }

        return result
    }

    /**
     * Execute an MCP tool on a specific server.
     */
    suspend fun callTool(serverName: String, toolName: String, args: Map<String, Any?>): String {
        return withContext(Dispatchers.IO) {
            val server = servers.find { it.name == serverName }
                ?: return@withContext "{\"error\":\"Server not found: $serverName\"}"

            val requestBody = gson.toJson(mapOf(
                "jsonrpc" to "2.0",
                "id" to System.currentTimeMillis(),
                "method" to "tools/call",
                "params" to mapOf(
                    "name" to toolName,
                    "arguments" to args
                )
            ))

            val response = when {
                server.url != null -> httpCallTool(server.url, server.headers, requestBody)
                server.command != null -> stdioCallTool(server.command, server.args, requestBody)
                else -> "{\"error\":\"No transport for $serverName\"}"
            }

            // Extract result content
            try {
                val json = JsonParser.parseString(response).asJsonObject
                val result = json?.get("result")?.asJsonObject
                val content = result?.get("content")?.asJsonArray

                if (content != null) {
                    content.map { it.asJsonObject.get("text")?.asString ?: "" }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
                } else {
                    json?.get("error")?.asJsonObject?.let {
                        "MCP 错误: ${it.get("message")?.asString}"
                    } ?: response
                }
            } catch (e: Exception) {
                "MCP 解析失败: ${e.message}"
            }
        }
    }

    // ---- HTTP transport ----

    private fun httpListTools(url: String, headers: Map<String, String>, body: String): String {
        val req = Request.Builder().url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return httpClient.newCall(req).execute().body?.string() ?: "{}"
    }

    private fun httpCallTool(url: String, headers: Map<String, String>, body: String): String {
        val req = Request.Builder().url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return httpClient.newCall(req).execute().body?.string() ?: "{}"
    }

    // ---- Stdio transport ----

    private fun stdioListTools(command: String, args: List<String>, body: String): String {
        return stdioCall(command, args, body)
    }

    private fun stdioCallTool(command: String, args: List<String>, body: String): String {
        return stdioCall(command, args, body)
    }

    private fun stdioCall(command: String, cmdArgs: List<String>, body: String): String {
        val process = ProcessBuilder(command, *cmdArgs.toTypedArray())
            .redirectErrorStream(true)
            .start()
        process.outputStream.write(body.toByteArray())
        process.outputStream.flush()
        process.outputStream.close()
        val result = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return "{\"error\":\"MCP 服务器超时\"}"
        }
        process.destroy()
        return result
    }

    // ---- Helpers ----

    private fun buildDefinition(name: String, schema: McpToolSchema): ToolDefinition {
        val params = parseParametersFromSchema(schema.inputSchema)
        return ToolDefinition(
            name = name,
            description = schema.description,
            parameters = params,
            action = "mcp_tool",
            toolset = "mcp",
            emoji = ""
        )
    }

    private fun parseParametersFromSchema(schema: Map<String, Any?>?): List<ParameterSchema> {
        if (schema == null) return emptyList()
        val properties = (schema["properties"] as? Map<*, *>) ?: return emptyList()
        val required = (schema["required"] as? List<*>)?.map { it.toString() } ?: emptyList()

        return properties.map { (key, value) ->
            val prop = value as? Map<*, *> ?: return@map null
            val name = key?.toString() ?: return@map null
            val type = when (prop["type"]?.toString()) {
                "string" -> ParameterType.STRING
                "integer" -> ParameterType.INTEGER
                "number" -> ParameterType.NUMBER
                "boolean" -> ParameterType.BOOLEAN
                "array" -> ParameterType.ARRAY
                "object" -> ParameterType.OBJECT
                else -> ParameterType.STRING
            }
            ParameterSchema(
                name = name,
                type = type,
                description = prop["description"]?.toString() ?: "",
                required = name in required
            )
        }.filterNotNull()
    }

    /**
     * Wrapper to make MCP tools compatible with ITool interface.
     */
    class McpToolWrapper(
        override val definition: ToolDefinition,
        private val serverName: String,
        private val toolName: String,
        private val manager: McpClientManager
    ) : ITool {
        override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
            val result = manager.callTool(serverName, toolName, args)
            return if (result.startsWith("MCP 错误")) {
                ToolResult.Error(result.removePrefix("MCP 错误: "))
            } else {
                ToolResult.Success(result)
            }
        }

        override fun isAvailable(): Boolean = true
    }
}

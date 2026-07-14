package com.aichat.app.data.repository

import com.aichat.app.data.local.McpServerDao
import com.aichat.app.data.model.McpServer
import com.aichat.app.data.terminal.TerminalLogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpRepository @Inject constructor(
    private val mcpServerDao: McpServerDao,
    private val terminal: TerminalLogBuffer
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun getAllServers(): Flow<List<McpServer>> = mcpServerDao.getAllServers()

    suspend fun getEnabledServers(): List<McpServer> = mcpServerDao.getEnabledServers()

    suspend fun addServer(name: String, url: String): Long =
        mcpServerDao.insertServer(McpServer(name = name, url = url))

    suspend fun setEnabled(id: Long, enabled: Boolean) = mcpServerDao.setEnabled(id, enabled)

    suspend fun deleteServer(id: Long) = mcpServerDao.deleteServerById(id)

    /** 测试 MCP 服务器连通性，结果写入终端 */
    suspend fun testConnection(server: McpServer): Boolean = withContext(Dispatchers.IO) {
        terminal.info(TAG, "测试连接: ${server.name} -> ${server.url}")
        try {
            val request = Request.Builder().url(server.url).head().build()
            client.newCall(request).execute().use { resp ->
                val ok = resp.isSuccessful
                if (ok) terminal.success(TAG, "连接成功 [${resp.code}]: ${server.name}")
                else terminal.warn(TAG, "连接返回非 2xx [${resp.code}]: ${server.name}")
                ok
            }
        } catch (e: Exception) {
            terminal.error(TAG, "连接失败: ${server.name} - ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "MCP"
    }
}

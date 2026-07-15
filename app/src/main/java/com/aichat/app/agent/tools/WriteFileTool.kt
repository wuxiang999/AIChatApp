package com.aichat.app.agent.tools

import android.content.Context
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Write content to a file. Creates parent directories if needed.
 * Handles path safety and file size limits.
 *
 * Inspired by OpenCode's write tool.
 */
@Singleton
class WriteFileTool @Inject constructor(
    @ApplicationContext private val context: Context
) : ITool {

    override val definition = ToolDefinition(
        name = "write_file",
        description = "写入文件内容。如果文件不存在则创建，支持覆盖写入",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件路径", required = true),
            ParameterSchema("content", ParameterType.STRING, "文件内容", required = true),
            ParameterSchema("append", ParameterType.BOOLEAN, "是否追加（默认false=覆盖）", required = false)
        ),
        action = "write_file",
        toolset = "core",
        emoji = ""
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("缺少 path 参数")
        val content = args["content"]?.toString() ?: return ToolResult.Error("缺少 content 参数")
        val append = (args["append"] as? Boolean) ?: false

        val file = resolvePath(context.workingDirectory, path)
            ?: return ToolResult.Error("路径不安全或被禁止访问: $path")

        // 5MB limit
        if (content.length > 5 * 1024 * 1024) {
            return ToolResult.Error("内容太大 (>5MB)")
        }

        return try {
            file.parentFile?.mkdirs()
            if (append) {
                file.appendText(content)
            } else {
                file.writeText(content)
            }
            ToolResult.Success("已写入 ${file.absolutePath} (${content.length} 字符)")
        } catch (e: Exception) {
            android.util.Log.e("WriteFileTool", "写入失败", e)
            ToolResult.Error("写入失败: ${e.message}")
        }
    }

    private fun resolvePath(workingDir: String, rawPath: String): File? {
        val path = rawPath.trim()

        // Absolute path
        val file = if (path.startsWith("/")) {
            File(path)
        } else {
            File(File(workingDir), path)
        }

        // Prevent directory traversal
        val canonical = file.canonicalPath
        val allowedBase = File(workingDir).canonicalPath
        val dataDir = context.filesDir.canonicalPath

        return if (canonical.startsWith(allowedBase) || canonical.startsWith(dataDir)) {
            file
        } else {
            null
        }
    }
}

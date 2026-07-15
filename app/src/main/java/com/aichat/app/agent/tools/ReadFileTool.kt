package com.aichat.app.agent.tools

import android.content.Context
import android.util.Log
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
 * Read file contents from the file system.
 * Handles path safety: prevents directory traversal outside allowed working directory.
 *
 * Inspired by OpenCode's read tool + Hermes' path scope safety.
 */
@Singleton
class ReadFileTool @Inject constructor(
    @ApplicationContext private val context: Context
) : ITool {

    override val definition = ToolDefinition(
        name = "read_file",
        description = "读取文件内容。支持多种编码（UTF-8, GBK等）",
        parameters = listOf(
            ParameterSchema("path", ParameterType.STRING, "文件路径（相对于工作目录或绝对路径）", required = true),
            ParameterSchema("offset", ParameterType.INTEGER, "起始行号（从1开始，默认从头）", required = false),
            ParameterSchema("limit", ParameterType.INTEGER, "最大读取行数（默认全部）", required = false)
        ),
        action = "read_file",
        toolset = "core",
        emoji = ""
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val path = args["path"]?.toString() ?: return ToolResult.Error("缺少 path 参数")
        val offset = (args["offset"] as? Number)?.toInt() ?: 1
        val limit = (args["limit"] as? Number)?.toInt() ?: Int.MAX_VALUE

        // Path safety: resolve relative to working directory
        val file = resolvePath(context.workingDirectory, path)
            ?: return ToolResult.Error("路径不安全或被禁止访问: $path")

        if (!file.exists()) {
            return ToolResult.Error("文件不存在: ${file.absolutePath}")
        }
        if (!file.isFile) {
            return ToolResult.Error("路径不是一个文件: ${file.absolutePath}")
        }
        if (file.length() > 10 * 1024 * 1024) {
            return ToolResult.Error("文件太大 (>10MB)，无法读取: ${file.absolutePath}")
        }

        return try {
            val lines = file.readLines()
            val totalLines = lines.size

            val startIndex = (offset - 1).coerceIn(0, totalLines - 1)
            val endIndex = (startIndex + limit).coerceIn(0, totalLines)

            val selectedLines = lines.subList(startIndex, endIndex)
            val content = selectedLines.joinToString("\n")
            val lineRange = "${startIndex + 1}-${endIndex}"

            val summary = if (totalLines > limit) {
                "\n\n[显示 $lineRange / 共 $totalLines 行]"
            } else {
                "\n\n[共 $totalLines 行]"
            }

            ToolResult.Success(content + summary)
        } catch (e: Exception) {
            Log.e("ReadFileTool", "读取失败", e)
            ToolResult.Error("读取失败: ${e.message}")
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

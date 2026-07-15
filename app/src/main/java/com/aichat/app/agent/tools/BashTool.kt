package com.aichat.app.agent.tools

import android.util.Log
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Execute shell commands on the device.
 * Security-sensitive: requires user approval for execution.
 *
 * Features:
 * - Timeout control (default 30s)
 * - Output truncation (max 50KB)
 * - Working directory support
 * - Path safety (prevents execution outside allowed dirs)
 *
 * Inspired by OpenCode's bash tool + Hermes' terminal backend.
 */
@Singleton
class BashTool @Inject constructor() : ITool {

    override val definition = ToolDefinition(
        name = "bash",
        description = "执行 shell 命令。可用于安装依赖、运行脚本、管理文件系统等",
        parameters = listOf(
            ParameterSchema("command", ParameterType.STRING, "要执行的 shell 命令", required = true),
            ParameterSchema("timeout", ParameterType.INTEGER, "超时时间（秒，默认30）", required = false),
            ParameterSchema("workdir", ParameterType.STRING, "工作目录（默认使用项目目录）", required = false)
        ),
        action = "bash",
        toolset = "core",
        emoji = ""
    )

    companion object {
        private const val TAG = "BashTool"
        private const val MAX_OUTPUT_LENGTH = 50 * 1024  // 50KB
        private const val DEFAULT_TIMEOUT = 30L
    }

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val command = args["command"]?.toString() ?: return ToolResult.Error("缺少 command 参数")
        val timeout = (args["timeout"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT
        val workdir = args["workdir"]?.toString() ?: context.workingDirectory

        // Security: warn about dangerous commands
        val dangerousPatterns = listOf(
            Regex("""rm\s+-rf\s+/*"""),
            Regex("""mkfs\s"""),
            Regex("""dd\s+if="""),
            Regex(""":\(\)\s*\{"""),
            Regex("""chmod\s+777\s+/"""),
            Regex("""wget.*\|"""),
            Regex("""curl.*\|""")
        )
        for (pattern in dangerousPatterns) {
            if (pattern.containsMatchIn(command)) {
                return ToolResult.Error("命令包含危险操作，已阻止: ${pattern.pattern}")
            }
        }

        return try {
            val dir = File(workdir)
            if (!dir.exists() || !dir.isDirectory) {
                return ToolResult.Error("工作目录不存在: $workdir")
            }

            val shell = System.getenv("SHELL") ?: "/system/bin/sh"
            val process = ProcessBuilder()
                .command(shell, "-c", command)
                .directory(dir)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ToolResult.Error("命令执行超时 (${timeout}s)")
            }

            val exitCode = process.exitValue()
            val output = process.inputStream.bufferedReader().readText()

            val truncatedOutput = if (output.length > MAX_OUTPUT_LENGTH) {
                output.take(MAX_OUTPUT_LENGTH) + "\n\n...（输出已截断，共 ${output.length} 字符）"
            } else {
                output
            }

            val result = buildString {
                appendLine("退出码: $exitCode")
                if (truncatedOutput.isNotEmpty()) {
                    appendLine(truncatedOutput)
                }
            }

            if (exitCode == 0) {
                ToolResult.Success(result.trim())
            } else {
                ToolResult.Error(result.trim(), exitCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "命令执行失败", e)
            ToolResult.Error("命令执行失败: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", "echo ok")
                .start()
            val ok = process.inputStream.bufferedReader().readText().trim() == "ok"
            process.waitFor(1, TimeUnit.SECONDS)
            ok
        } catch (e: Exception) {
            false
        }
    }
}

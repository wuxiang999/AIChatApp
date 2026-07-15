package com.aichat.app.agent.tools

import android.util.Log
import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CodeVibeShellTools"
private const val MAX_OUTPUT_LENGTH = 50 * 1024
private const val DEFAULT_TIMEOUT = 30L

@Singleton
class ExecuteShellTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "execute_shell",
        description = "执行 shell 命令。用于安装依赖、编译代码、运行脚本、Git操作等",
        parameters = listOf(
            ParameterSchema("command", ParameterType.STRING, "要执行的 shell 命令", required = true),
            ParameterSchema("timeout", ParameterType.INTEGER, "超时时间（秒，默认30）", required = false),
            ParameterSchema("workdir", ParameterType.STRING, "工作目录", required = false)
        ),
        action = "execute_shell",
        toolset = "terminal"
    )

    private val dangerousPatterns = listOf(
        Regex("""rm\s+-rf\s+/*"""),
        Regex("""mkfs\s"""),
        Regex("""dd\s+if="""),
        Regex(""":\(\)\s*\{"""),
        Regex("""chmod\s+777\s+/"""),
        Regex("""wget.*\|"""),
        Regex("""curl.*\|""")
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val command = args["command"]?.toString() ?: return ToolResult.Error("Missing command parameter")
        val timeout = (args["timeout"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT
        val workdir = args["workdir"]?.toString() ?: context.workingDirectory

        for (pattern in dangerousPatterns) {
            if (pattern.containsMatchIn(command))
                return ToolResult.Error("Blocked dangerous command: ${pattern.pattern}")
        }

        return try {
            val dir = File(workdir)
            if (!dir.exists() || !dir.isDirectory)
                return ToolResult.Error("Working directory does not exist: $workdir")

            val shell = System.getenv("SHELL") ?: "/system/bin/sh"
            val process = ProcessBuilder()
                .command(shell, "-c", command)
                .directory(dir)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ToolResult.Error("Command timed out after ${timeout}s")
            }

            val exitCode = process.exitValue()
            val output = process.inputStream.bufferedReader().readText()
            val truncated = if (output.length > MAX_OUTPUT_LENGTH)
                output.take(MAX_OUTPUT_LENGTH) + "\n\n...(truncated, ${output.length} total chars)"
            else output

            val result = buildString {
                appendLine("Exit code: $exitCode")
                if (truncated.isNotEmpty()) append(truncated)
            }.trim()

            if (exitCode == 0) ToolResult.Success(result)
            else ToolResult.Error(result, exitCode)
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            ToolResult.Error("Command failed: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean = try {
        val p = ProcessBuilder("/system/bin/sh", "-c", "echo ok").start()
        val ok = p.inputStream.bufferedReader().readText().trim() == "ok"
        p.waitFor(1, TimeUnit.SECONDS); ok
    } catch (_: Exception) { false }
}

@Singleton
class ExecuteInTerminalSessionTool @Inject constructor() : ITool {
    private val sessionNameToId = ConcurrentHashMap<String, String>()

    override val definition = ToolDefinition(
        name = "execute_in_terminal_session",
        description = "在持久化终端会话中执行命令。支持交互式命令和长时运行进程",
        parameters = listOf(
            ParameterSchema("session_id", ParameterType.STRING, "会话ID（用create_terminal_session创建）", required = true),
            ParameterSchema("command", ParameterType.STRING, "要执行的命令", required = true),
            ParameterSchema("timeout_ms", ParameterType.INTEGER, "超时时间（毫秒）", required = false)
        ),
        action = "execute_in_terminal_session",
        toolset = "terminal"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val sessionId = args["session_id"]?.toString() ?: return ToolResult.Error("Missing session_id parameter")
        val command = args["command"]?.toString() ?: return ToolResult.Error("Missing command parameter")
        val timeoutMs = (args["timeout_ms"] as? Number)?.toLong() ?: 1800000L

        return try {
            val shell = System.getenv("SHELL") ?: "/system/bin/sh"
            val sessionDir = File(context.workingDirectory, ".terminal_sessions/$sessionId").also { it.mkdirs() }
            val logFile = File(sessionDir, "output.log")

            val process = ProcessBuilder()
                .command(shell, "-c", command)
                .directory(File(context.workingDirectory))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                .start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ToolResult.Error("Command timed out after ${timeoutMs}ms")
            }

            val exitCode = process.exitValue()
            val output = if (logFile.exists()) logFile.readText() else ""
            val truncated = if (output.length > MAX_OUTPUT_LENGTH)
                output.take(MAX_OUTPUT_LENGTH) + "\n\n...(truncated)"
            else output

            val result = "Session: $sessionId\nExit code: $exitCode\n$truncated"
            if (exitCode == 0) ToolResult.Success(result.trim())
            else ToolResult.Error(result.trim(), exitCode)
        } catch (e: Exception) {
            Log.e(TAG, "Session command failed", e)
            ToolResult.Error("Session command failed: ${e.message}")
        }
    }
}

@Singleton
class CreateTerminalSessionTool @Inject constructor() : ITool {
    private val sessionNameToId = ConcurrentHashMap<String, String>()

    override val definition = ToolDefinition(
        name = "create_terminal_session",
        description = "创建一个持久的终端会话，用于在多个步骤中保持状态",
        parameters = listOf(
            ParameterSchema("session_name", ParameterType.STRING, "会话名称", required = true)
        ),
        action = "create_terminal_session",
        toolset = "terminal"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val sessionName = args["session_name"]?.toString() ?: return ToolResult.Error("Missing session_name parameter")
        val sessionId = "session_${System.currentTimeMillis()}_${sessionName.replace("\\s+".toRegex(), "_")}"
        sessionNameToId[sessionName] = sessionId

        val sessionDir = File(context.workingDirectory, ".terminal_sessions/$sessionId")
        sessionDir.mkdirs()

        return ToolResult.Success("Created terminal session: name=$sessionName, id=$sessionId")
    }
}

@Singleton
class CloseTerminalSessionTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "close_terminal_session",
        description = "关闭终端会话",
        parameters = listOf(
            ParameterSchema("session_id", ParameterType.STRING, "会话ID", required = true)
        ),
        action = "close_terminal_session",
        toolset = "terminal"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val sessionId = args["session_id"]?.toString() ?: return ToolResult.Error("Missing session_id parameter")
        val sessionDir = File(context.workingDirectory, ".terminal_sessions/$sessionId")
        if (sessionDir.exists()) sessionDir.deleteRecursively()
        return ToolResult.Success("Closed terminal session: $sessionId")
    }
}

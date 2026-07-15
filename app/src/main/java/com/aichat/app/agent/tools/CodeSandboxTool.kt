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

@Singleton
class CodeSandboxTool @Inject constructor() : ITool {

    override val definition = ToolDefinition(
        name = "code_sandbox",
        description = "在隔离沙箱中运行代码。支持 Python、JavaScript、Kotlin、Shell。",
        parameters = listOf(
            ParameterSchema("language", ParameterType.STRING, "编程语言: python/js/kotlin/sh", required = true),
            ParameterSchema("code", ParameterType.STRING, "要执行的代码", required = true),
            ParameterSchema("timeout", ParameterType.INTEGER, "超时秒数(默认15)", required = false)
        ),
        action = "code_sandbox",
        toolset = "core",
        emoji = ""
    )

    companion object {
        private const val TAG = "CodeSandboxTool"
        private const val MAX_OUTPUT_LENGTH = 100 * 1024
        private const val DEFAULT_TIMEOUT = 15L
        private const val MAX_TIMEOUT = 60L

        private val DANGEROUS_PATTERNS = listOf(
            Regex("""rm\s+-rf\s+/\s"""),
            Regex("""mkfs"""),
            Regex("""dd\s+if=""", RegexOption.IGNORE_CASE),
            Regex(""":\s*\(\s*\)\s*\{"""),
            Regex("""\|\s*sh"""),
            Regex("""\|\s*bash"""),
            Regex("""chmod\s+777\s+/"""),
            Regex("""fork\s*bomb"""),
            Regex("""\bkexec\b"""),
            Regex("""\bpoweroff\b"""),
            Regex("""\breboot\b"""),
            Regex("""\bhalt\b"""),
            Regex("""\binit\s+0\b"""),
            Regex("""\binit\s+6\b"""),
            Regex(""">/\s+"""),
            Regex("""/dev/[sh]d[a-z]"""),
            Regex("""/dev/nvme"""),
            Regex(""":\(\)\s*\{""")
        )

        private val SANDBOX_DIR = File(
            "/storage/emulated/0/.Download/Python/APP/AIchat/AIChatApp/sandbox"
        )
    }

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val language = args["language"]?.toString()?.lowercase() ?: return ToolResult.Error("缺少 language 参数")
        val code = args["code"]?.toString() ?: return ToolResult.Error("缺少 code 参数")
        val timeout = ((args["timeout"] as? Number)?.toLong() ?: DEFAULT_TIMEOUT)
            .coerceAtMost(MAX_TIMEOUT)

        if (code.isBlank()) return ToolResult.Error("代码为空")

        for (pattern in DANGEROUS_PATTERNS) {
            if (pattern.containsMatchIn(code)) {
                return ToolResult.Error("代码包含危险操作，已阻止: ${pattern.pattern}")
            }
        }

        return try {
            SANDBOX_DIR.mkdirs()

            val sandboxEnv = mapOf(
                "SANDBOX" to "1",
                "HOME" to SANDBOX_DIR.absolutePath,
                "TMPDIR" to SANDBOX_DIR.absolutePath,
                "NO_NETWORK" to "1"
            )

            val cmd = buildCommand(language, code)
                ?: return ToolResult.Error("不支持的语言: $language，支持: python/js/kotlin/sh")

            val process = ProcessBuilder()
                .command(cmd)
                .directory(SANDBOX_DIR)
                .redirectErrorStream(false)
                .apply {
                    environment().putAll(sandboxEnv)
                }
                .start()

            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ToolResult.Error("代码执行超时 (${timeout}s)")
            }

            val stdout = readStream(process.inputStream)
            val stderr = readStream(process.errorStream)
            val exitCode = process.exitValue()

            val truncated = if (stdout.length > MAX_OUTPUT_LENGTH) {
                stdout.take(MAX_OUTPUT_LENGTH) + "\n\n...（输出已截断，共 ${stdout.length} 字符）"
            } else stdout

            val result = buildString {
                appendLine("退出码: $exitCode")
                if (truncated.isNotBlank()) {
                    appendLine("标准输出:")
                    appendLine(truncated)
                }
                if (stderr.isNotBlank()) {
                    appendLine("标准错误:")
                    appendLine(stderr.take(MAX_OUTPUT_LENGTH / 2))
                }
            }

            if (exitCode == 0) {
                ToolResult.Success(result.trim())
            } else {
                ToolResult.Error(result.trim(), exitCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "沙箱执行失败", e)
            ToolResult.Error("沙箱执行失败: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            SANDBOX_DIR.mkdirs()
            val sh = ProcessBuilder("/system/bin/sh", "-c", "echo sandbox_ok").start()
            val ok = sh.inputStream.bufferedReader().readText().trim() == "sandbox_ok"
            sh.waitFor(1, TimeUnit.SECONDS)
            ok
        } catch (e: Exception) {
            false
        }
    }

    private fun buildCommand(language: String, code: String): List<String>? {
        return when (language) {
            "python", "py" -> {
                val candidates = listOf("/data/data/com.termux/files/usr/bin/python3", "/system/bin/python3")
                val python = candidates.firstOrNull { File(it).exists() }
                if (python != null) listOf(python, "-c", code) else null
            }
            "sh", "shell", "bash" -> listOf("/system/bin/sh", "-c", code)
            "js", "javascript" -> {
                val candidates = listOf("/system/bin/node", "/data/data/com.termux/files/usr/bin/node")
                val node = candidates.firstOrNull { File(it).exists() }
                if (node != null) listOf(node, "-e", code) else null
            }
            "kotlin", "kt" -> {
                val candidates = listOf("/system/bin/kotlinc", "/data/data/com.termux/files/usr/bin/kotlinc")
                val kc = candidates.firstOrNull { File(it).exists() }
                if (kc != null) listOf(kc, "-script", "-e", code) else null
            }
            else -> null
        }
    }

    private fun readStream(stream: java.io.InputStream): String {
        return try {
            stream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "[读取输出流失败]"
        }
    }
}

package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class SleepTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "sleep",
        description = "等待指定的毫秒数。用于在连续操作间添加延迟",
        parameters = listOf(
            ParameterSchema("duration_ms", ParameterType.INTEGER, "等待时间（毫秒）", required = true)
        ),
        action = "sleep",
        toolset = "utility"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val durationMs = (args["duration_ms"] as? Number)?.toLong() ?: 1000L
        if (durationMs < 0) return ToolResult.Error("duration_ms must be non-negative")
        val safeDuration = durationMs.coerceAtMost(300000L)
        delay(safeDuration)
        return ToolResult.Success("Slept for ${safeDuration}ms")
    }
}

@Singleton
class CalculateTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "calculate",
        description = "执行数学计算。支持四则运算、幂运算、三角函数等",
        parameters = listOf(
            ParameterSchema("expression", ParameterType.STRING, "数学表达式", required = true)
        ),
        action = "calculate",
        toolset = "utility"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val expression = args["expression"]?.toString()?.trim() ?: return ToolResult.Error("Missing expression parameter")
        return try {
            val result = evaluateExpression(expression)
            ToolResult.Success("$expression = $result")
        } catch (e: Exception) {
            ToolResult.Error("Calculation error: ${e.message}")
        }
    }

    private fun evaluateExpression(expr: String): Double {
        val sanitized = expr
            .replace("×", "*").replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace(" ", "")
        return try {
            evalSimpleMath(sanitized)
        } catch (_: Exception) {
            expr.toDoubleOrNull() ?: throw IllegalArgumentException("Cannot evaluate expression: $expr")
        }
    }

    private fun evalSimpleMath(expr: String): Double {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            when {
                expr[i].isWhitespace() -> i++
                expr[i] in '0'..'9' || expr[i] == '.' -> {
                    var num = ""
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) { num += expr[i]; i++ }
                    tokens.add(num)
                }
                expr[i] in "+-*/()" -> { tokens.add(expr[i].toString()); i++ }
                else -> throw IllegalArgumentException("Unknown character: ${expr[i]}")
            }
        }

        val output = mutableListOf<String>()
        val ops = mutableListOf<String>()
        val prec = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)

        fun applyOp() {
            if (ops.isEmpty()) return
            val op = ops.removeAt(ops.lastIndex)
            val b = output.removeAt(output.lastIndex).toDouble()
            val a = output.removeAt(output.lastIndex).toDouble()
            output.add(when (op) { "+" -> a + b; "-" -> a - b; "*" -> a * b; else -> a / b }.toString())
        }

        for (tok in tokens) {
            when {
                tok.toDoubleOrNull() != null -> output.add(tok)
                tok == "(" -> ops.add(tok)
                tok == ")" -> { while (ops.isNotEmpty() && ops.last() != "(") applyOp(); if (ops.lastOrNull() == "(") ops.removeAt(ops.lastIndex) }
                tok in prec -> {
                    while (ops.isNotEmpty() && ops.last() != "(" && (prec[ops.last()] ?: 0) >= (prec[tok] ?: 0)) applyOp()
                    ops.add(tok)
                }
            }
        }
        while (ops.isNotEmpty()) applyOp()
        return output.single().toDouble()
    }
}

@Singleton
class DeviceInfoTool @Inject constructor() : ITool {
    override val definition = ToolDefinition(
        name = "device_info",
        description = "获取当前设备的基本信息（型号、Android版本、内存等）",
        parameters = emptyList(),
        action = "device_info",
        toolset = "utility"
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val info = buildString {
            appendLine("OS: Android")
            appendLine("OS Version: ${System.getProperty("os.version") ?: "unknown"}")
            appendLine("Architecture: ${System.getProperty("os.arch") ?: "unknown"}")
            appendLine("Java Version: ${System.getProperty("java.version") ?: "unknown"}")
            appendLine("Available Processors: ${Runtime.getRuntime().availableProcessors()}")
            appendLine("Max Memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
            appendLine("Total Memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
            appendLine("Free Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
            appendLine("Data Directory: ${context.workingDirectory}")
        }
        return ToolResult.Success(info.trim())
    }
}

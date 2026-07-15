package com.aichat.app.agent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class AIToolHandler {

    private val availableTools = ConcurrentHashMap<String, ToolExecutor>()
    private val toolHooks = CopyOnWriteArrayList<AIToolHook>()

    fun registerTool(
        name: String,
        executor: ToolExecutor
    ) {
        availableTools[name] = executor
    }

    fun registerTool(
        name: String,
        executor: (AITool) -> OperitToolResult
    ) {
        registerTool(
            name = name,
            executor = object : ToolExecutor {
                override fun invoke(tool: AITool): OperitToolResult {
                    return executor(tool)
                }
            }
        )
    }

    fun unregisterTool(toolName: String) {
        availableTools.remove(toolName)
    }

    fun addToolHook(hook: AIToolHook) {
        if (!toolHooks.contains(hook)) {
            toolHooks.add(hook)
        }
    }

    fun removeToolHook(hook: AIToolHook) {
        toolHooks.remove(hook)
    }

    fun clearToolHooks() {
        toolHooks.clear()
    }

    private inline fun notifyHooks(eventName: String, action: (AIToolHook) -> Unit) {
        toolHooks.forEach { hook ->
            try {
                action(hook)
            } catch (_: Exception) { }
        }
    }

    fun notifyToolCallRequested(tool: AITool) {
        notifyHooks("onToolCallRequested") { it.onToolCallRequested(tool) }
    }

    fun checkToolInterception(tool: AITool): AIToolHookDecision {
        toolHooks.forEach { hook ->
            val decision =
                try {
                    hook.onToolCallIntercept(tool)
                } catch (e: Exception) {
                    return AIToolHookDecision.Block(
                        "Tool hook callback failed at onToolCallIntercept."
                    )
                }
            when (decision) {
                AIToolHookDecision.Allow -> Unit
                is AIToolHookDecision.Block -> return decision
            }
        }
        return AIToolHookDecision.Allow
    }

    fun buildToolInterceptionResult(
        toolName: String,
        decision: AIToolHookDecision.Block
    ): OperitToolResult {
        return OperitToolResult(
            toolName = toolName,
            success = false,
            result = "",
            error = decision.reason
        )
    }

    fun notifyToolPermissionChecked(tool: AITool, granted: Boolean, reason: String? = null) {
        notifyHooks("onToolPermissionChecked") { it.onToolPermissionChecked(tool, granted, reason) }
    }

    fun notifyToolExecutionStarted(tool: AITool) {
        notifyHooks("onToolExecutionStarted") { it.onToolExecutionStarted(tool) }
    }

    fun notifyToolExecutionResult(tool: AITool, result: OperitToolResult) {
        notifyHooks("onToolExecutionResult") { it.onToolExecutionResult(tool, result) }
    }

    fun notifyToolExecutionError(tool: AITool, throwable: Throwable) {
        notifyHooks("onToolExecutionError") { it.onToolExecutionError(tool, throwable) }
    }

    fun notifyToolExecutionFinished(tool: AITool) {
        notifyHooks("onToolExecutionFinished") { it.onToolExecutionFinished(tool) }
    }

    fun getAllToolNames(): List<String> {
        return availableTools.keys.toList().sorted()
    }

    fun getToolExecutor(toolName: String): ToolExecutor? {
        return availableTools[toolName]
    }

    fun executeTool(tool: AITool): OperitToolResult {
        notifyToolCallRequested(tool)
        when (val interception = checkToolInterception(tool)) {
            AIToolHookDecision.Allow -> Unit
            is AIToolHookDecision.Block -> {
                val interceptedResult = buildToolInterceptionResult(tool.name, interception)
                notifyToolExecutionResult(tool, interceptedResult)
                notifyToolExecutionFinished(tool)
                return interceptedResult
            }
        }

        val executor = availableTools[tool.name]

        if (executor == null) {
            val notFoundResult = OperitToolResult(
                toolName = tool.name,
                success = false,
                result = "",
                error = "Tool not found: ${tool.name}"
            )
            notifyToolExecutionResult(tool, notFoundResult)
            notifyToolExecutionFinished(tool)
            return notFoundResult
        }

        val validationResult = executor.validateParameters(tool)
        if (!validationResult.valid) {
            val validationFailedResult = OperitToolResult(
                toolName = tool.name,
                success = false,
                result = "",
                error = validationResult.errorMessage
            )
            notifyToolExecutionResult(tool, validationFailedResult)
            notifyToolExecutionFinished(tool)
            return validationFailedResult
        }

        notifyToolExecutionStarted(tool)
        return try {
            val result = executor.invoke(tool)
            notifyToolExecutionResult(tool, result)
            result
        } catch (e: Exception) {
            notifyToolExecutionError(tool, e)
            throw e
        } finally {
            notifyToolExecutionFinished(tool)
        }
    }

    fun executeToolAndStream(tool: AITool): Flow<OperitToolResult> = flow {
        notifyToolCallRequested(tool)
        when (val interception = checkToolInterception(tool)) {
            AIToolHookDecision.Allow -> Unit
            is AIToolHookDecision.Block -> {
                val interceptedResult = buildToolInterceptionResult(tool.name, interception)
                notifyToolExecutionResult(tool, interceptedResult)
                notifyToolExecutionFinished(tool)
                emit(interceptedResult)
                return@flow
            }
        }

        val executor = availableTools[tool.name]

        if (executor == null) {
            val notFoundResult = OperitToolResult(
                toolName = tool.name,
                success = false,
                result = "",
                error = "Tool not found: ${tool.name}"
            )
            notifyToolExecutionResult(tool, notFoundResult)
            notifyToolExecutionFinished(tool)
            emit(notFoundResult)
            return@flow
        }

        val validationResult = executor.validateParameters(tool)
        if (!validationResult.valid) {
            val validationFailedResult = OperitToolResult(
                toolName = tool.name,
                success = false,
                result = "",
                error = validationResult.errorMessage
            )
            notifyToolExecutionResult(tool, validationFailedResult)
            notifyToolExecutionFinished(tool)
            emit(validationFailedResult)
            return@flow
        }

        notifyToolExecutionStarted(tool)
        try {
            executor.invokeAndStream(tool).collect { result ->
                notifyToolExecutionResult(tool, result)
                emit(result)
            }
        } catch (e: Exception) {
            notifyToolExecutionError(tool, e)
            throw e
        } finally {
            notifyToolExecutionFinished(tool)
        }
    }

    fun reset() {
        availableTools.clear()
    }
}

interface ToolExecutor {
    fun invoke(tool: AITool): OperitToolResult

    fun invokeAndStream(tool: AITool): Flow<OperitToolResult> = flowOf(invoke(tool))

    fun validateParameters(tool: AITool): ToolValidationResult {
        return ToolValidationResult(valid = true)
    }
}

interface AIToolHook {
    fun onToolCallRequested(tool: AITool) {}
    fun onToolCallIntercept(tool: AITool): AIToolHookDecision = AIToolHookDecision.Allow
    fun onToolPermissionChecked(tool: AITool, granted: Boolean, reason: String? = null) {}
    fun onToolExecutionStarted(tool: AITool) {}
    fun onToolExecutionResult(tool: AITool, result: OperitToolResult) {}
    fun onToolExecutionError(tool: AITool, throwable: Throwable) {}
    fun onToolExecutionFinished(tool: AITool) {}
}

sealed class AIToolHookDecision {
    data object Allow : AIToolHookDecision()
    data class Block(val reason: String) : AIToolHookDecision()
}

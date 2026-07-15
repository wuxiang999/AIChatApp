package com.aichat.app.agent

import android.util.Log
import com.aichat.app.permission.PermissionEngine
import com.aichat.app.permission.PermissionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry for all tools available to the AI agent.
 *
 * Inspired by:
 * - Hermes Agent: tools/registry.py — auto-discovery + check_fn gating + toolset aliases
 * - OpenCode: core/src/tool/registry.ts — materialize() + permission filtering
 *
 * Design:
 * - Singleton, thread-safe
 * - Tools auto-register via discovery or explicit registration
 * - materialize() filters by permissions to produce the list exposed to LLM
 */
@Singleton
class ToolRegistry @Inject constructor() {

    companion object {
        private const val TAG = "ToolRegistry"
    }

    // Thread-safe storage using synchronized
    private val tools = mutableMapOf<String, ITool>()

    /**
     * Register a tool. Thread-safe.
     */
    fun register(tool: ITool) {
        synchronized(tools) {
            val existing = tools.put(tool.definition.name, tool)
            if (existing != null) {
                Log.d(TAG, "Tool '${tool.definition.name}' re-registered (was: ${existing::class.simpleName})")
            } else {
                Log.d(TAG, "Tool registered: ${tool.definition.name} (${tool.definition.toolset})")
            }
        }
    }

    /**
     * Register multiple tools at once.
     */
    fun registerAll(vararg tools: ITool) {
        tools.forEach { register(it) }
    }

    /**
     * Register a collection of tools.
     */
    fun registerAll(tools: Collection<ITool>) {
        tools.forEach { register(it) }
    }

    /**
     * Find a tool by name.
     */
    fun find(name: String): ITool? {
        return synchronized(tools) { tools[name] }
    }

    /**
     * Get all registered tool names.
     */
    fun getToolNames(): Set<String> {
        return synchronized(tools) { tools.keys.toSet() }
    }

    /**
     * Get all tools (bypassed permission check).
     */
    fun getAllTools(): List<ITool> {
        return synchronized(tools) { tools.values.toList() }
    }

    /**
     * Materialize the tool list for a given permission engine.
     * Filters out:
     * 1. Tools that are not available (check_fn)
     * 2. Tools denied by permission engine
     *
     * This is the primary method used by AgentLoop to build the tool list for LLM.
     */
    fun materialize(permissions: PermissionEngine? = null, toolset: String? = null): List<ITool> {
        return synchronized(tools) {
            tools.values
                .filter { tool ->
                    // Filter by availability
                    if (!tool.isAvailable()) return@filter false

                    // Filter by toolset if specified
                    if (toolset != null && tool.definition.toolset != toolset) return@filter false

                    // Filter by permissions
                    if (permissions != null) {
                        val result = permissions.evaluate(tool.definition.action, "*")
                        result !is PermissionResult.Denied
                    } else {
                        true
                    }
                }
                .toList()
        }
    }

    /**
     * Execute a tool by name with given arguments.
     * Returns ToolResult — never throws.
     */
    suspend fun execute(name: String, context: ToolContext, args: Map<String, Any?>): ToolResult {
        val tool = find(name) ?: return ToolResult.Error("Tool not found: $name", 404)

        if (!tool.isAvailable()) {
            return ToolResult.Error("Tool '$name' is not available in current environment", 503)
        }

        return try {
            tool.execute(context, args)
        } catch (e: Exception) {
            Log.e(TAG, "Tool '$name' execution failed", e)
            ToolResult.Error("${e::class.simpleName}: ${e.message}", 500)
        }
    }

    /**
     * Clear all registered tools.
     */
    fun clear() {
        synchronized(tools) { tools.clear() }
        Log.d(TAG, "Tool registry cleared")
    }
}

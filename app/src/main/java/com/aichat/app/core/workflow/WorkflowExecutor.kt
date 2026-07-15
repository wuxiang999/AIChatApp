package com.aichat.app.core.workflow

import android.util.Log
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolRegistry
import com.aichat.app.data.model.workflow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DAG-based workflow executor for CodeVibe.
 *
 * Walks a workflow graph (trigger → execute/condition/extract → ...) in
 * topological order, dispatching ExecuteNode actions to the ToolRegistry
 * and evaluating condition/logic/extract nodes inline.
 *
 * Design:
 * - Single-run per instance; create a new builder() for each execution.
 * - Logs every step into WorkflowExecutionLogEntry for the UI timeline.
 * - Cancellation via `cancel()` — sets flag checked before each node.
 */
@Singleton
class WorkflowExecutor @Inject constructor(
    private val toolRegistry: ToolRegistry
) {

    companion object {
        private const val TAG = "WorkflowExecutor"

        /**
         * Human-readable action display names.
         */
        private val actionDisplayNames = mapOf(
            "list_files" to "List Files",
            "read_file" to "Read File",
            "write_file" to "Write File",
            "edit_file" to "Edit File",
            "delete_file" to "Delete File",
            "copy_file" to "Copy File",
            "move_file" to "Move File",
            "make_directory" to "Create Directory",
            "find_files" to "Find Files",
            "file_info" to "File Info",
            "file_exists" to "Check File Exists",
            "grep_code" to "Search Code",
            "zip_files" to "Zip Files",
            "unzip_files" to "Unzip Files",
            "execute_shell" to "Execute Shell",
            "visit_web" to "Visit Web Page",
            "http_request" to "HTTP Request",
            "query_memory" to "Query Memory",
            "create_memory" to "Save to Memory",
            "sleep" to "Sleep/Wait",
            "calculate" to "Calculate",
            "device_info" to "Device Info",
            "bash" to "Execute Bash",
            "terminal" to "Terminal",
            "read" to "Read File",
            "write" to "Write File",
            "search_web" to "Search Web"
        )
    }

    private var _cancelled = false
    private val _logs = mutableListOf<WorkflowExecutionLogEntry>()
    private val _logsFlow = MutableStateFlow<List<WorkflowExecutionLogEntry>>(emptyList())

    val logs: List<WorkflowExecutionLogEntry> get() = _logs
    val logsFlow: StateFlow<List<WorkflowExecutionLogEntry>> = _logsFlow.asStateFlow()

    private val _currentNodeId = MutableStateFlow<String?>(null)
    val currentNodeId: StateFlow<String?> = _currentNodeId.asStateFlow()

    // Runtime data storage for node references
    private val nodeOutputs = mutableMapOf<String, String>()

    fun cancel() {
        _cancelled = true
    }

    /**
     * Execute a complete workflow.
     * Returns WorkflowExecutionRecord with all logs and final status.
     */
    suspend fun execute(
        workflow: Workflow,
        toolContext: ToolContext,
        initialInputs: Map<String, String> = emptyMap()
    ): WorkflowExecutionRecord {
        _cancelled = false
        _logs.clear()
        _logsFlow.value = emptyList()
        nodeOutputs.clear()
        nodeOutputs.putAll(initialInputs)

        val runId = java.util.UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        addLog(WorkflowLogLevel.DEBUG, "Workflow execution started: ${workflow.name}")

        if (workflow.nodes.isEmpty()) {
            addLog(WorkflowLogLevel.WARN, "Workflow has no nodes")
            return record(runId, workflow, startedAt, true, "No nodes to execute")
        }

        try {
            // 1. Build adjacency graph
            val graph = buildGraph(workflow)
            addLog(WorkflowLogLevel.DEBUG, "Graph built: ${graph.size} nodes")

            // 2. Topological sort
            val sorted = topologicalSort(workflow, graph)
            if (sorted == null) {
                addLog(WorkflowLogLevel.ERROR, "Workflow contains a cycle or is invalid")
                return record(runId, workflow, startedAt, false, "Workflow contains a cycle")
            }
            addLog(WorkflowLogLevel.DEBUG, "Topological order: ${sorted.joinToString(" -> ") { it.name }}")

            // 3. Execute node by node
            for (node in sorted) {
                if (_cancelled) {
                    addLog(WorkflowLogLevel.WARN, "Execution cancelled at node: ${node.name}")
                    return record(runId, workflow, startedAt, false, "Cancelled at: ${node.name}")
                }

                _currentNodeId.value = node.id
                addLog(WorkflowLogLevel.DEBUG, "Executing node: ${node.name} (${node.type})", node.id, node.name)

                when (node) {
                    is TriggerNode -> executeTrigger(node, workflow)
                    is ExecuteNode -> executeActionNode(node, toolContext)
                    is ConditionNode -> executeCondition(node, workflow, graph)
                    is LogicNode -> executeLogic(node, workflow, graph)
                    is ExtractNode -> executeExtract(node)
                }
            }

            _currentNodeId.value = null
            addLog(WorkflowLogLevel.DEBUG, "Workflow completed successfully")
            return record(runId, workflow, startedAt, true, "Completed successfully")

        } catch (e: Exception) {
            _currentNodeId.value = null
            addLog(WorkflowLogLevel.ERROR, "Execution failed: ${e.message}")
            Log.e(TAG, "Workflow execution failed", e)
            return record(runId, workflow, startedAt, false, "Error: ${e.message}")
        }
    }

    // ─── Graph Construction ──────────────────────────────────────────

    private fun buildGraph(workflow: Workflow): Map<String, List<String>> {
        val graph = mutableMapOf<String, MutableList<String>>()
        val incoming = mutableMapOf<String, Int>()

        workflow.nodes.forEach { node ->
            graph.getOrPut(node.id) { mutableListOf() }
            incoming.getOrPut(node.id) { 0 }
        }

        workflow.connections.forEach { conn ->
            graph[conn.sourceNodeId]?.add(conn.targetNodeId)
            incoming[conn.targetNodeId] = (incoming[conn.targetNodeId] ?: 0) + 1
        }

        return graph
    }

    private fun topologicalSort(
        workflow: Workflow,
        graph: Map<String, List<String>>
    ): List<WorkflowNode>? {
        val nodeMap = workflow.nodes.associateBy { it.id }

        val inDegree = mutableMapOf<String, Int>()
        graph.keys.forEach { inDegree[it] = 0 }
        graph.forEach { (node, neighbors) ->
            neighbors.forEach { target ->
                inDegree[target] = (inDegree[target] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.forEach { (nodeId, degree) ->
            if (degree == 0) queue.add(nodeId)
        }

        val sorted = mutableListOf<WorkflowNode>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val node = nodeMap[current]
            if (node != null) sorted.add(node)

            graph[current]?.forEach { neighbor ->
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) queue.add(neighbor)
            }
        }

        // Cycle detection
        if (sorted.size != workflow.nodes.size) return null
        return sorted
    }

    // ─── Node Executors ──────────────────────────────────────────────

    private fun executeTrigger(node: TriggerNode, workflow: Workflow) {
        val log = when (node.triggerType) {
            "manual" -> "Manual trigger"
            "schedule" -> "Scheduled trigger"
            else -> "Trigger (${node.triggerType})"
        }
        addLog(WorkflowLogLevel.DEBUG, log, node.id, node.name)
        nodeOutputs[node.id] = "triggered"
    }

    private suspend fun executeActionNode(node: ExecuteNode, context: ToolContext) {
        val actionType = node.actionType
        val displayName = actionDisplayNames[actionType] ?: actionType
        addLog(WorkflowLogLevel.DEBUG, "Running action: $displayName", node.id, node.name)

        // Build args from actionConfig, resolving node references
        val args = mutableMapOf<String, Any?>()
        node.actionConfig.forEach { (key, value) ->
            args[key] = resolveParameterValue(value)
        }

        addLog(WorkflowLogLevel.DEBUG, "Action args: ${args.entries.joinToString(", ") { "${it.key}=${it.value}" }}")

        // Find the tool and execute
        val tool = toolRegistry.find(actionType)
        if (tool == null) {
            addLog(WorkflowLogLevel.WARN, "Tool not found: $actionType", node.id, node.name)
            // Still store something so downstream nodes get data
            nodeOutputs[node.id] = "{\"error\": \"Tool not found: $actionType\"}"
            return
        }

        try {
            val result = tool.execute(context, args)
            when (result) {
                is com.aichat.app.agent.ToolResult.Success -> {
                    addLog(WorkflowLogLevel.DEBUG, "Action completed successfully", node.id, node.name)
                    nodeOutputs[node.id] = result.data
                }
                is com.aichat.app.agent.ToolResult.Error -> {
                    addLog(WorkflowLogLevel.WARN, "Action failed: ${result.message}", node.id, node.name)
                    nodeOutputs[node.id] = "{\"error\": \"${result.message}\"}"
                }
                is com.aichat.app.agent.ToolResult.PermissionDenied -> {
                    addLog(WorkflowLogLevel.ERROR, "Permission denied: ${result.action}", node.id, node.name)
                    nodeOutputs[node.id] = "{\"error\": \"Permission denied: ${result.action}\"}"
                }
            }
        } catch (e: Exception) {
            addLog(WorkflowLogLevel.ERROR, "Action threw: ${e.message}", node.id, node.name)
            nodeOutputs[node.id] = "{\"error\": \"${e.message}\"}"
        }
    }

    private fun executeCondition(node: ConditionNode, workflow: Workflow, graph: Map<String, List<String>>) {
        val leftVal = resolveParameterValue(node.left)
        val rightVal = resolveParameterValue(node.right)
        val result = evaluateCondition(leftVal, rightVal, node.operator)

        addLog(
            WorkflowLogLevel.DEBUG,
            "Condition: $leftVal ${node.operator.name} $rightVal → $result",
            node.id, node.name
        )

        nodeOutputs[node.id] = result.toString()

        // Determine which branch to follow based on result
        val connections = workflow.connections.filter { it.sourceNodeId == node.id }
        if (connections.isNotEmpty()) {
            val path = if (result) "true" else "false"
            val branchNames = connections.map { conn ->
                val target = workflow.nodes.find { it.id == conn.targetNodeId }
                target?.name ?: conn.targetNodeId
            }
            addLog(WorkflowLogLevel.DEBUG, "Following -> $result branch: ${branchNames.joinToString(", ")}")
        }
    }

    private fun executeLogic(node: LogicNode, workflow: Workflow, graph: Map<String, List<String>>) {
        // Find incoming connections to get the inputs
        val incomingConnections = workflow.connections.filter { it.targetNodeId == node.id }
        val inputValues = incomingConnections.mapNotNull { conn ->
            nodeOutputs[conn.sourceNodeId]?.toBooleanStrictOrNull()
        }

        val result = when (node.operator) {
            LogicOperator.AND -> inputValues.all { it }
            LogicOperator.OR -> inputValues.any { it }
        }

        addLog(
            WorkflowLogLevel.DEBUG,
            "Logic ${node.operator.name}: [${inputValues.joinToString(", ")}] → $result",
            node.id, node.name
        )

        nodeOutputs[node.id] = result.toString()
    }

    private fun executeExtract(node: ExtractNode) {
        val source = resolveParameterValue(node.source)
        addLog(WorkflowLogLevel.DEBUG, "Extracting from source", node.id, node.name)

        val result = try {
            when (node.mode) {
                ExtractMode.REGEX -> {
                    val regex = node.expression.toRegex()
                    val match = regex.find(source)
                    match?.groupValues?.getOrElse(node.group) { "" } ?: node.defaultValue
                }
                ExtractMode.JSON -> {
                    // Simple JSON path extraction (field lookup)
                    val cleaned = source.trim()
                    val pattern = "\"${node.expression}\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val match = pattern.find(cleaned)
                    match?.groupValues?.getOrElse(1) { node.defaultValue } ?: node.defaultValue
                }
                ExtractMode.SUB -> {
                    val start = node.startIndex.coerceIn(0, source.length)
                    val end = if (node.length < 0) source.length
                    else (start + node.length).coerceAtMost(source.length)
                    source.substring(start, end)
                }
                ExtractMode.CONCAT -> {
                    node.fixedValue
                }
                ExtractMode.RANDOM_INT -> {
                    val min = node.randomMin
                    val max = node.randomMax.coerceAtLeast(min + 1)
                    (min..max).random().toString()
                }
                ExtractMode.RANDOM_STRING -> {
                    val chars = node.randomStringCharset
                    (1..node.randomStringLength)
                        .map { chars.random() }
                        .joinToString("")
                }
            }
        } catch (e: Exception) {
            addLog(WorkflowLogLevel.WARN, "Extract failed: ${e.message}", node.id, node.name)
            node.defaultValue
        }

        nodeOutputs[node.id] = result
        addLog(WorkflowLogLevel.DEBUG, "Extracted: '$result'", node.id, node.name)
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private fun evaluateCondition(left: String, right: String, operator: ConditionOperator): Boolean {
        return try {
            when (operator) {
                ConditionOperator.EQ -> left == right
                ConditionOperator.NE -> left != right
                ConditionOperator.GT -> left.toDouble() > right.toDouble()
                ConditionOperator.GTE -> left.toDouble() >= right.toDouble()
                ConditionOperator.LT -> left.toDouble() < right.toDouble()
                ConditionOperator.LTE -> left.toDouble() <= right.toDouble()
                ConditionOperator.CONTAINS -> left.contains(right)
                ConditionOperator.NOT_CONTAINS -> !left.contains(right)
                ConditionOperator.IN -> right.split(",").any { it.trim() == left }
                ConditionOperator.NOT_IN -> right.split(",").none { it.trim() == left }
            }
        } catch (e: NumberFormatException) {
            // Fall back to string comparison for GT/LT families
            when (operator) {
                ConditionOperator.GT -> left > right
                ConditionOperator.GTE -> left >= right
                ConditionOperator.LT -> left < right
                ConditionOperator.LTE -> left <= right
                else -> left == right
            }
        }
    }

    private fun resolveParameterValue(value: ParameterValue): String {
        return when (value) {
            is ParameterValue.StaticValue -> value.value
            is ParameterValue.NodeReference -> nodeOutputs[value.nodeId] ?: ""
        }
    }

    private fun addLog(
        level: WorkflowLogLevel,
        message: String,
        nodeId: String? = null,
        nodeName: String? = null
    ) {
        val entry = WorkflowExecutionLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message,
            nodeId = nodeId,
            nodeName = nodeName
        )
        _logs.add(entry)
        _logsFlow.value = _logs.toList()
    }

    private fun record(
        runId: String,
        workflow: Workflow,
        startedAt: Long,
        success: Boolean,
        message: String
    ): WorkflowExecutionRecord {
        val finishedAt = System.currentTimeMillis()
        return WorkflowExecutionRecord(
            runId = runId,
            workflowId = workflow.id,
            workflowName = workflow.name,
            triggerNodeId = workflow.nodes.firstOrNull { it is TriggerNode }?.id,
            startedAt = startedAt,
            finishedAt = finishedAt,
            success = success,
            message = message,
            logs = _logs.toList()
        )
    }
}

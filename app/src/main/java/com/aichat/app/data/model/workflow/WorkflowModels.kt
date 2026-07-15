package com.aichat.app.data.model.workflow

import java.util.UUID

// ─── Core Workflow ───────────────────────────────────────────────────

data class Workflow(
    val id: String,
    var name: String = "",
    var description: String = "",
    var nodes: List<WorkflowNode> = emptyList(),
    var connections: List<WorkflowNodeConnection> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var enabled: Boolean = true,
    var lastExecutionTime: Long? = null,
    var lastExecutionStatus: ExecutionStatus? = null,
    var totalExecutions: Int = 0,
    var successfulExecutions: Int = 0,
    var failedExecutions: Int = 0
)

enum class ExecutionStatus { SUCCESS, FAILED, RUNNING }

// ─── Node Types ─────────────────────────────────────────────────────

sealed class WorkflowNode {
    abstract val id: String
    abstract val type: String
    abstract var name: String
    abstract var description: String
    abstract var position: NodePosition
}

data class NodePosition(var x: Float = 0f, var y: Float = 0f)

data class TriggerNode(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "trigger",
    override var name: String = "",
    override var description: String = "",
    override var position: NodePosition = NodePosition(0f, 0f),
    var triggerType: String = "manual" // manual, schedule
) : WorkflowNode()

data class ExecuteNode(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "execute",
    override var name: String = "",
    override var description: String = "",
    override var position: NodePosition = NodePosition(0f, 0f),
    var actionType: String = "",
    var actionConfig: Map<String, ParameterValue> = emptyMap()
) : WorkflowNode()

enum class ConditionOperator {
    EQ, NE, GT, GTE, LT, LTE, CONTAINS, NOT_CONTAINS, IN, NOT_IN
}

data class ConditionNode(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "condition",
    override var name: String = "",
    override var description: String = "",
    override var position: NodePosition = NodePosition(0f, 0f),
    var left: ParameterValue = ParameterValue.StaticValue(""),
    var operator: ConditionOperator = ConditionOperator.EQ,
    var right: ParameterValue = ParameterValue.StaticValue("")
) : WorkflowNode()

enum class LogicOperator { AND, OR }

data class LogicNode(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "logic",
    override var name: String = "",
    override var description: String = "",
    override var position: NodePosition = NodePosition(0f, 0f),
    var operator: LogicOperator = LogicOperator.AND
) : WorkflowNode()

enum class ExtractMode { REGEX, JSON, SUB, CONCAT, RANDOM_INT, RANDOM_STRING }

data class ExtractNode(
    override val id: String = UUID.randomUUID().toString(),
    override val type: String = "extract",
    override var name: String = "",
    override var description: String = "",
    override var position: NodePosition = NodePosition(0f, 0f),
    var source: ParameterValue = ParameterValue.StaticValue(""),
    var mode: ExtractMode = ExtractMode.REGEX,
    var expression: String = "",
    var group: Int = 0,
    var defaultValue: String = "",
    var startIndex: Int = 0,
    var length: Int = -1,
    var randomMin: Int = 0,
    var randomMax: Int = 100,
    var randomStringLength: Int = 8,
    var randomStringCharset: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
    var useFixed: Boolean = false,
    var fixedValue: String = ""
) : WorkflowNode()

// ─── Parameter Value Types ──────────────────────────────────────────

sealed class ParameterValue {
    data class StaticValue(val value: String) : ParameterValue()
    data class NodeReference(val nodeId: String) : ParameterValue()
}

// ─── Connections ────────────────────────────────────────────────────

data class WorkflowNodeConnection(
    val id: String = UUID.randomUUID().toString(),
    val sourceNodeId: String,
    val targetNodeId: String,
    var condition: String? = null
)

// ─── Execution Logging ──────────────────────────────────────────────

enum class WorkflowLogLevel { DEBUG, WARN, ERROR }

data class WorkflowExecutionLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: WorkflowLogLevel,
    val message: String,
    val nodeId: String? = null,
    val nodeName: String? = null
)

data class WorkflowExecutionRecord(
    val runId: String = UUID.randomUUID().toString(),
    val workflowId: String,
    val workflowName: String,
    val triggerNodeId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = System.currentTimeMillis(),
    val success: Boolean,
    val message: String,
    val logs: List<WorkflowExecutionLogEntry> = emptyList()
)

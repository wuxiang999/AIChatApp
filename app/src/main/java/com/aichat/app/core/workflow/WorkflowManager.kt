package com.aichat.app.core.workflow

import android.util.Log
import com.aichat.app.data.model.workflow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages workflow CRUD, execution history, and state.
 *
 * Thread-safe, in-memory storage. Designed as a singleton that can later
 * be backed by Room or DataStore for persistence.
 */
@Singleton
class WorkflowManager @Inject constructor() {

    companion object {
        private const val TAG = "WorkflowManager"
    }

    private val workflows = mutableMapOf<String, Workflow>()
    private val executionHistory = mutableListOf<WorkflowExecutionRecord>()

    private val _workflows = mutableListOf<Workflow>()
    private val listeners = mutableListOf<() -> Unit>()

    // ─── Workflow CRUD ───────────────────────────────────────────────

    fun getAllWorkflows(): List<Workflow> = synchronized(workflows) {
        workflows.values.sortedByDescending { it.updatedAt }
    }

    fun getAllWorkflowsList(): List<Workflow> = synchronized(workflows) {
        _workflows.toList()
    }

    fun getWorkflow(id: String): Workflow? = synchronized(workflows) {
        workflows[id]
    }

    fun createWorkflow(name: String = "New Workflow", description: String = ""): Workflow {
        val workflow = Workflow(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            nodes = listOf(
                TriggerNode(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "Trigger",
                    description = "Start the workflow",
                    triggerType = "manual"
                )
            )
        )
        synchronized(workflows) {
            workflows[workflow.id] = workflow
            _workflows.add(workflow)
        }
        notifyListeners()
        Log.d(TAG, "Created workflow: ${workflow.id} - ${workflow.name}")
        return workflow
    }

    fun updateWorkflow(workflow: Workflow): Boolean = synchronized(workflows) {
        if (workflows.containsKey(workflow.id)) {
            workflows[workflow.id] = workflow.copy(updatedAt = System.currentTimeMillis())
            val idx = _workflows.indexOfFirst { it.id == workflow.id }
            if (idx >= 0) _workflows[idx] = workflow
            notifyListeners()
            Log.d(TAG, "Updated workflow: ${workflow.id}")
            true
        } else {
            Log.w(TAG, "Workflow not found for update: ${workflow.id}")
            false
        }
    }

    fun deleteWorkflow(id: String): Boolean = synchronized(workflows) {
        val removed = workflows.remove(id) != null
        if (removed) {
            _workflows.removeAll { it.id == id }
            notifyListeners()
            Log.d(TAG, "Deleted workflow: $id")
        }
        removed
    }

    fun duplicateWorkflow(id: String): Workflow? = synchronized(workflows) {
        val original = workflows[id] ?: return@synchronized null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            totalExecutions = 0,
            successfulExecutions = 0,
            failedExecutions = 0
        )
        workflows[copy.id] = copy
        _workflows.add(copy)
        notifyListeners()
        Log.d(TAG, "Duplicated workflow: ${original.id} -> ${copy.id}")
        copy
    }

    // ─── Execution History ───────────────────────────────────────────

    fun getExecutionHistory(limit: Int = 50): List<WorkflowExecutionRecord> = synchronized(executionHistory) {
        executionHistory.sortedByDescending { it.startedAt }.take(limit)
    }

    fun getExecutionHistoryForWorkflow(workflowId: String, limit: Int = 20): List<WorkflowExecutionRecord> = synchronized(executionHistory) {
        executionHistory.filter { it.workflowId == workflowId }
            .sortedByDescending { it.startedAt }
            .take(limit)
    }

    fun addExecutionRecord(record: WorkflowExecutionRecord) = synchronized(executionHistory) {
        executionHistory.add(record)

        // Update workflow stats
        val wf = workflows[record.workflowId]
        if (wf != null) {
            val updated = wf.copy(
                lastExecutionTime = record.startedAt,
                lastExecutionStatus = if (record.success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,
                totalExecutions = wf.totalExecutions + 1,
                successfulExecutions = wf.successfulExecutions + (if (record.success) 1 else 0),
                failedExecutions = wf.failedExecutions + (if (record.success) 0 else 1),
                updatedAt = System.currentTimeMillis()
            )
            workflows[record.workflowId] = updated
        }

        // Cap history at 500 entries
        while (executionHistory.size > 500) {
            executionHistory.removeFirst()
        }
        notifyListeners()
    }

    // ─── Listeners ───────────────────────────────────────────────────

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }

    // ─── Built-in Templates ──────────────────────────────────────────

    fun createFromTemplate(templateId: String): Workflow {
        val template = when (templateId) {
            "file_backup" -> Workflow(
                id = java.util.UUID.randomUUID().toString(),
                name = "File Backup",
                description = "Backup files to another directory",
                nodes = listOf(
                    TriggerNode(name = "Manual Start", triggerType = "manual"),
                    ExecuteNode(name = "List Source", actionType = "list_files", actionConfig = mapOf(
                        "path" to ParameterValue.StaticValue("/storage/emulated/0/Documents")
                    )),
                    ExtractNode(name = "Extract File Count", mode = ExtractMode.REGEX, expression = "\"count\":\\s*(\\d+)", defaultValue = "0"),
                    ConditionNode(name = "Has Files?", operator = ConditionOperator.GT, left = ParameterValue.StaticValue("0")),
                    ExecuteNode(name = "Zip Files", actionType = "zip_files"),
                    ExecuteNode(name = "Copy to Backup", actionType = "copy_file")
                )
            )
            "web_monitor" -> Workflow(
                id = java.util.UUID.randomUUID().toString(),
                name = "Web Monitor",
                description = "Check a webpage and notify if content changes",
                nodes = listOf(
                    TriggerNode(name = "Scheduled Check", triggerType = "schedule"),
                    ExecuteNode(name = "Fetch Page", actionType = "visit_web"),
                    ExecuteNode(name = "Save Snapshot", actionType = "write_file")
                )
            )
            "code_search_pipeline" -> Workflow(
                id = java.util.UUID.randomUUID().toString(),
                name = "Code Search Pipeline",
                description = "Search code patterns and save results",
                nodes = listOf(
                    TriggerNode(name = "Manual Trigger", triggerType = "manual"),
                    ExecuteNode(name = "Search Code", actionType = "grep_code"),
                    ExtractNode(name = "Extract Results", mode = ExtractMode.REGEX, expression = "\"matches\":\\s*\\[(.*?)\\]"),
                    ExecuteNode(name = "Save Report", actionType = "write_file")
                )
            )
            else -> {
                Log.w(TAG, "Unknown template: $templateId")
                return createWorkflow()
            }
        }
        synchronized(workflows) {
            workflows[template.id] = template
            _workflows.add(template)
        }
        notifyListeners()
        return template
    }
}

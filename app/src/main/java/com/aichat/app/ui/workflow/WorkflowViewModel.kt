package com.aichat.app.ui.workflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.agent.ToolContext
import com.aichat.app.core.workflow.WorkflowExecutor
import com.aichat.app.core.workflow.WorkflowManager
import com.aichat.app.data.model.workflow.Workflow
import com.aichat.app.data.model.workflow.WorkflowExecutionRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkflowViewModel @Inject constructor(
    val workflowManager: WorkflowManager,
    private val workflowExecutor: WorkflowExecutor
) : ViewModel() {

    private val _workflows = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _workflows.asStateFlow()

    private val _currentRun = MutableStateFlow<WorkflowExecutionRecord?>(null)
    val currentRun: StateFlow<WorkflowExecutionRecord?> = _currentRun.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showExecutionDialog = MutableStateFlow(false)
    val showExecutionDialog: StateFlow<Boolean> = _showExecutionDialog.asStateFlow()

    init {
        refresh()
        workflowManager.addListener { refresh() }
    }

    fun refresh() {
        _workflows.value = workflowManager.getAllWorkflowsList()
    }

    fun showExecution(record: WorkflowExecutionRecord?) {
        _currentRun.value = record
    }

    fun runWorkflow(workflow: Workflow) {
        if (_isRunning.value) return
        _isRunning.value = true

        viewModelScope.launch {
            try {
                val context = ToolContext(
                    sessionId = "workflow-${workflow.id}",
                    conversationId = "workflow-run",
                    workingDirectory = "/storage/emulated/0/.Download/Python/APP/AIchat"
                )

                val record = workflowExecutor.execute(workflow, context)
                workflowManager.addExecutionRecord(record)
                _currentRun.value = record
                _showExecutionDialog.value = true
            } catch (e: Exception) {
                _currentRun.value = null
                _showExecutionDialog.value = true
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun dismissExecutionDialog() {
        _showExecutionDialog.value = false
    }
}

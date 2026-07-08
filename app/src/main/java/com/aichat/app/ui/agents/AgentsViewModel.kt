package com.aichat.app.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Agent
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _selectedAgentId = MutableStateFlow<String?>(null)
    val selectedAgentId: StateFlow<String?> = _selectedAgentId.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val isAddDialogVisible: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeAgents()
            repository.getAllAgents().collect { agentList ->
                _agents.value = agentList
                _selectedAgentId.value = agentList.find { it.isSelected }?.id
            }
        }
    }

    fun selectAgent(agentId: String) {
        viewModelScope.launch {
            repository.selectAgent(agentId)
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun addCustomAgent(name: String, description: String, systemPrompt: String) {
        viewModelScope.launch {
            repository.addCustomAgent(name, description, systemPrompt)
            _showAddDialog.value = false
        }
    }

    fun deleteAgent(agentId: String) {
        viewModelScope.launch {
            repository.deleteCustomAgent(agentId)
        }
    }
}

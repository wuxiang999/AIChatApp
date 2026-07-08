package com.aichat.app.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.local.AgentDao
import com.aichat.app.data.model.Agent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val agentDao: AgentDao
) : ViewModel() {

    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _selectedAgent = MutableStateFlow<Agent?>(null)
    val selectedAgent: StateFlow<Agent?> = _selectedAgent.asStateFlow()

    init {
        loadAgents()
        loadSelectedAgent()
    }

    private fun loadAgents() {
        viewModelScope.launch {
            agentDao.getAllAgents().collect { agents ->
                _agents.value = agents
            }
        }
    }

    private fun loadSelectedAgent() {
        viewModelScope.launch {
            _selectedAgent.value = agentDao.getSelectedAgent()
        }
    }

    fun addAgent(name: String, description: String, systemPrompt: String, voice: String = "") {
        viewModelScope.launch {
            val agent = Agent(
                name = name,
                description = description,
                systemPrompt = systemPrompt,
                voice = voice,
                createdAt = Date()
            )
            agentDao.insertAgent(agent)
        }
    }

    fun updateAgent(id: Long, name: String, description: String, systemPrompt: String, voice: String) {
        viewModelScope.launch {
            val agent = agentDao.getAgentById(id) ?: return@launch
            agentDao.updateAgent(
                agent.copy(
                    name = name,
                    description = description,
                    systemPrompt = systemPrompt,
                    voice = voice
                )
            )
        }
    }

    fun deleteAgent(id: Long) {
        viewModelScope.launch {
            val wasSelected = _selectedAgent.value?.id == id
            agentDao.deleteAgent(id)
            if (wasSelected) {
                _selectedAgent.value = null
            }
        }
    }

    fun selectAgent(id: Long) {
        viewModelScope.launch {
            agentDao.clearSelected()
            agentDao.setSelected(id)
            _selectedAgent.value = agentDao.getAgentById(id)
        }
    }

    fun getSelectedAgentNow(): Agent? = _selectedAgent.value
}

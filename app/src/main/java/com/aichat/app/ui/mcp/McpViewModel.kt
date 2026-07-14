package com.aichat.app.ui.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.McpServer
import com.aichat.app.data.repository.McpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class McpViewModel @Inject constructor(
    private val repository: McpRepository
) : ViewModel() {

    private val _servers = MutableStateFlow<List<McpServer>>(emptyList())
    val servers: StateFlow<List<McpServer>> = _servers.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllServers().collect { _servers.value = it }
        }
    }

    fun addServer(name: String, url: String) {
        viewModelScope.launch { repository.addServer(name, url) }
    }

    fun toggleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch { repository.deleteServer(id) }
    }

    fun testConnection(server: McpServer) {
        viewModelScope.launch {
            _testing.value = true
            try {
                repository.testConnection(server)
            } finally {
                _testing.value = false
            }
        }
    }
}

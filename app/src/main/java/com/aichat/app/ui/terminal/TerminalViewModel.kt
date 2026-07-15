package com.aichat.app.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.terminal.TerminalLogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminal: TerminalLogBuffer
) : ViewModel() {

    val logs = terminal.logs

    private val _filterLevel = MutableStateFlow<TerminalLogBuffer.LogLevel?>(null)
    val filterLevel: StateFlow<TerminalLogBuffer.LogLevel?> = _filterLevel.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _expandedEntryId = MutableStateFlow<Long?>(null)
    val expandedEntryId: StateFlow<Long?> = _expandedEntryId.asStateFlow()

    val filteredLogs: StateFlow<List<TerminalLogBuffer.LogEntry>> = combine(
        logs, _filterLevel, _searchQuery
    ) { allLogs, level, query ->
        var result = allLogs
        if (level != null) {
            result = result.filter { it.level == level }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.message.lowercase().contains(q) ||
                it.tag.lowercase().contains(q) ||
                (it.toolName?.lowercase()?.contains(q) == true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<Map<TerminalLogBuffer.LogLevel, Int>> = logs.map { all ->
        all.groupBy { it.level }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(level: TerminalLogBuffer.LogLevel?) {
        _filterLevel.value = level
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleExpand(id: Long) {
        _expandedEntryId.value = if (_expandedEntryId.value == id) null else id
    }

    fun clear() {
        viewModelScope.launch { terminal.clear() }
    }

    fun exportLogs(): String = terminal.export()
}

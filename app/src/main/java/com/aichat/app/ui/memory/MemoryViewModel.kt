package com.aichat.app.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Memory
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.MemoryRepository
import com.aichat.app.memory.AutoMemoryExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MemorySortBy {
    TIME_DESC, IMPORTANCE_DESC, ACCESS_COUNT_DESC
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val autoMemoryExtractor: AutoMemoryExtractor,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _allMemories = MutableStateFlow<List<Memory>>(emptyList())
    val allMemories: StateFlow<List<Memory>> = _allMemories.asStateFlow()

    private val _filteredMemories = MutableStateFlow<List<Memory>>(emptyList())
    val filteredMemories: StateFlow<List<Memory>> = _filteredMemories.asStateFlow()

    private val _extractedCount = MutableStateFlow(0)
    val extractedCount: StateFlow<Int> = _extractedCount.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _isConsolidating = MutableStateFlow(false)
    val isConsolidating: StateFlow<Boolean> = _isConsolidating.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _sortBy = MutableStateFlow(MemorySortBy.TIME_DESC)
    val sortBy: StateFlow<MemorySortBy> = _sortBy.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _sources = MutableStateFlow<List<String>>(emptyList())
    val sources: StateFlow<List<String>> = _sources.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllMemories().collect { list ->
                _allMemories.value = list
                applyFilters()
                refreshStats()
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _totalCount.value = repository.getMemoryCount()
            _categories.value = repository.getAllCategories()
            _sources.value = repository.getAllSources()

            val all = repository.getAllMemoriesOnce()
            _categoryCounts.value = all.groupBy { it.category }.mapValues { it.value.size }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        applyFilters()
    }

    fun setSourceFilter(source: String?) {
        _selectedSource.value = if (_selectedSource.value == source) null else source
        applyFilters()
    }

    fun setSortBy(sort: MemorySortBy) {
        _sortBy.value = sort
        applyFilters()
    }

    private fun applyFilters() {
        var list = _allMemories.value

        val query = _searchQuery.value.trim()
        if (query.isNotEmpty()) {
            list = list.filter { it.content.contains(query, ignoreCase = true) }
        }

        _selectedCategory.value?.let { cat ->
            list = list.filter { it.category == cat }
        }

        _selectedSource.value?.let { src ->
            list = list.filter { it.source == src }
        }

        list = when (_sortBy.value) {
            MemorySortBy.TIME_DESC -> list.sortedByDescending { it.createdAt }
            MemorySortBy.IMPORTANCE_DESC -> list.sortedByDescending { it.importance }
            MemorySortBy.ACCESS_COUNT_DESC -> list.sortedByDescending { it.accessCount }
        }

        _filteredMemories.value = list
    }

    fun addMemory(content: String, category: String) {
        if (content.isBlank()) return
        viewModelScope.launch { repository.addMemory(content, category) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            refreshStats()
        }
    }

    fun updateMemory(id: Long, content: String, tags: String, importance: Int) {
        viewModelScope.launch {
            repository.updateMemory(id, content, tags, importance.coerceIn(1, 10))
        }
    }

    fun updateImportance(id: Long, importance: Int) {
        viewModelScope.launch {
            repository.updateImportance(id, importance.coerceIn(1, 10))
        }
    }

    fun extractFromLatestConversation() {
        if (_isExtracting.value) return
        _isExtracting.value = true
        viewModelScope.launch {
            try {
                val conversationsFlow = chatRepository.getAllConversations()
                val conversations = conversationsFlow.first()
                val latest = conversations.maxByOrNull { it.updatedAt }
                if (latest == null) {
                    _extractedCount.value = -1
                    return@launch
                }
                val messagesFlow = chatRepository.getMessagesForConversation(latest.id)
                val messages = messagesFlow.first()
                val count = autoMemoryExtractor.extractFromConversation(latest.id, messages).size
                _extractedCount.value = count
            } catch (e: Exception) {
                _extractedCount.value = -1
            } finally {
                _isExtracting.value = false
            }
        }
    }

    fun consolidateMemories() {
        if (_isConsolidating.value) return
        _isConsolidating.value = true
        viewModelScope.launch {
            try {
                autoMemoryExtractor.consolidate()
            } finally {
                _isConsolidating.value = false
            }
        }
    }

    fun resetExtractedCount() {
        _extractedCount.value = 0
    }
}

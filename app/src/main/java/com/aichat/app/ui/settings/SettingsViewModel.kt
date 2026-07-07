package com.aichat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiManager: ApiManager,
    private val repository: ChatRepository
) : ViewModel() {

    private val _endpoints = MutableStateFlow<List<ApiEndpoint>>(emptyList())
    val endpoints: StateFlow<List<ApiEndpoint>> = _endpoints.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<ApiEndpoint?>(null)
    val currentEndpoint: StateFlow<ApiEndpoint?> = _currentEndpoint.asStateFlow()

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _testResult = MutableStateFlow<Result<Int>?>(null)
    val testResult: StateFlow<Result<Int>?> = _testResult.asStateFlow()

    init {
        loadEndpoints()
        loadCurrentEndpoint()
    }

    private fun loadEndpoints() {
        viewModelScope.launch {
            apiManager.getAllEndpoints().collect { endpoints ->
                _endpoints.value = endpoints
            }
        }
    }

    private fun loadCurrentEndpoint() {
        _currentEndpoint.value = apiManager.getCurrentEndpoint()
    }

    fun loadModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            try {
                val models = repository.getModels()
                _models.value = models
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun addEndpoint(name: String, url: String, apiKey: String) {
        viewModelScope.launch {
            apiManager.addEndpoint(name, url, apiKey)
            loadCurrentEndpoint()
        }
    }

    fun updateEndpoint(id: Long, name: String, url: String, apiKey: String) {
        viewModelScope.launch {
            apiManager.updateEndpoint(id, name, url, apiKey)
            loadCurrentEndpoint()
        }
    }

    fun selectEndpoint(id: Long) {
        viewModelScope.launch {
            apiManager.selectEndpoint(id)
            loadCurrentEndpoint()
            _models.value = emptyList()
        }
    }

    fun deleteEndpoint(id: Long) {
        viewModelScope.launch {
            apiManager.deleteEndpoint(id)
            loadCurrentEndpoint()
        }
    }

    fun testEndpoint(url: String, apiKey: String) {
        viewModelScope.launch {
            _testResult.value = null
            val result = repository.testEndpoint(url, apiKey)
            _testResult.value = result
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }
}

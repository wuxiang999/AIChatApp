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

    // 每个端点的独立状态
    private val _endpointTestResults = MutableStateFlow<Map<Long, Result<Int>>>(emptyMap())
    val endpointTestResults: StateFlow<Map<Long, Result<Int>>> = _endpointTestResults.asStateFlow()

    private val _endpointModels = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    val endpointModels: StateFlow<Map<Long, List<String>>> = _endpointModels.asStateFlow()

    private val _loadingEndpoints = MutableStateFlow<Set<Long>>(emptySet())
    val loadingEndpoints: StateFlow<Set<Long>> = _loadingEndpoints.asStateFlow()

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

    fun testEndpointForId(endpoint: ApiEndpoint) {
        viewModelScope.launch {
            val current = _endpointTestResults.value.toMutableMap()
            current.remove(endpoint.id)
            _endpointTestResults.value = current

            val result = repository.testEndpoint(endpoint.url, endpoint.apiKey)

            val updated = _endpointTestResults.value.toMutableMap()
            updated[endpoint.id] = result
            _endpointTestResults.value = updated
        }
    }

    fun loadModelsForEndpoint(endpoint: ApiEndpoint) {
        viewModelScope.launch {
            val loading = _loadingEndpoints.value.toMutableSet()
            loading.add(endpoint.id)
            _loadingEndpoints.value = loading

            try {
                val result = apiManager.testEndpoint(endpoint.url, endpoint.apiKey)
                val models = if (result.isSuccess) {
                    // 临时切换到这个端点获取模型
                    apiManager.getApiServiceForEndpoint(endpoint.url, endpoint.apiKey)
                        .getModels("Bearer ${endpoint.apiKey}")
                        .data.map { it.id }.sorted()
                } else emptyList()

                val updated = _endpointModels.value.toMutableMap()
                updated[endpoint.id] = models
                _endpointModels.value = updated
            } catch (_: Exception) {
                val updated = _endpointModels.value.toMutableMap()
                updated[endpoint.id] = emptyList()
                _endpointModels.value = updated
            } finally {
                val loadingDone = _loadingEndpoints.value.toMutableSet()
                loadingDone.remove(endpoint.id)
                _loadingEndpoints.value = loadingDone
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

    fun clearEndpointTestResult(endpointId: Long) {
        val current = _endpointTestResults.value.toMutableMap()
        current.remove(endpointId)
        _endpointTestResults.value = current
    }
}

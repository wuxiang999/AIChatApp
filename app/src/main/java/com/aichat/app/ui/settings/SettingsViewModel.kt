package com.aichat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.local.SettingsDataStore
import com.aichat.app.data.model.ApiEndpoint
import com.aichat.app.data.model.AppSettings
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiManager: ApiManager,
    private val repository: ChatRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // --- API Endpoint State ---

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

    private val _endpointTestResults = MutableStateFlow<Map<Long, Result<Int>>>(emptyMap())
    val endpointTestResults: StateFlow<Map<Long, Result<Int>>> = _endpointTestResults.asStateFlow()

    private val _endpointModels = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    val endpointModels: StateFlow<Map<Long, List<String>>> = _endpointModels.asStateFlow()

    private val _loadingEndpoints = MutableStateFlow<Set<Long>>(emptySet())
    val loadingEndpoints: StateFlow<Set<Long>> = _loadingEndpoints.asStateFlow()

    // --- Settings State ---

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        loadEndpoints()
        loadCurrentEndpoint()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { s ->
                _settings.value = s
            }
        }
    }

    // --- Endpoint Methods ---

    private fun loadEndpoints() {
        viewModelScope.launch {
            try {
                apiManager.getAllEndpoints().collect { endpoints ->
                    _endpoints.value = endpoints
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "loadEndpoints error", e)
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
            try {
                val current = _endpointTestResults.value.toMutableMap()
                current.remove(endpoint.id)
                _endpointTestResults.value = current

                val result = repository.testEndpoint(endpoint.url, endpoint.apiKey)

                val updated = _endpointTestResults.value.toMutableMap()
                updated[endpoint.id] = result
                _endpointTestResults.value = updated
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "testEndpointForId error", e)
            }
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
            try {
                apiManager.addEndpoint(name, url, apiKey)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "addEndpoint error", e)
            }
            loadCurrentEndpoint()
        }
    }

    fun updateEndpoint(id: Long, name: String, url: String, apiKey: String) {
        viewModelScope.launch {
            try {
                apiManager.updateEndpoint(id, name, url, apiKey)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "updateEndpoint error", e)
            }
            loadCurrentEndpoint()
        }
    }

    fun selectEndpoint(id: Long) {
        viewModelScope.launch {
            try {
                apiManager.selectEndpoint(id)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "selectEndpoint error", e)
            }
            loadCurrentEndpoint()
            _models.value = emptyList()
        }
    }

    fun deleteEndpoint(id: Long) {
        viewModelScope.launch {
            try {
                apiManager.deleteEndpoint(id)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "deleteEndpoint error", e)
            }
            loadCurrentEndpoint()
        }
    }

    fun testEndpoint(url: String, apiKey: String) {
        viewModelScope.launch {
            _testResult.value = null
            try {
                val result = repository.testEndpoint(url, apiKey)
                _testResult.value = result
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "testEndpoint error", e)
                _testResult.value = Result.failure(e)
            }
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

    // --- Settings Update Methods ---

    fun updateWorkspacePath(path: String) {
        viewModelScope.launch { settingsDataStore.updateWorkspacePath(path) }
    }

    fun updateAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoSaveEnabled(enabled) }
    }

    fun updateAutoSaveInterval(interval: Int) {
        viewModelScope.launch { settingsDataStore.updateAutoSaveInterval(interval) }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch { settingsDataStore.updateFontSize(size) }
    }

    fun updateEditorTheme(theme: String) {
        viewModelScope.launch { settingsDataStore.updateEditorTheme(theme) }
    }

    fun updateShowLineNumbers(show: Boolean) {
        viewModelScope.launch { settingsDataStore.updateShowLineNumbers(show) }
    }

    fun updateTabSize(size: Int) {
        viewModelScope.launch { settingsDataStore.updateTabSize(size) }
    }

    fun updateDefaultCodingModel(model: String) {
        viewModelScope.launch { settingsDataStore.updateDefaultCodingModel(model) }
    }

    fun updateMaxIterations(iterations: Int) {
        viewModelScope.launch { settingsDataStore.updateMaxIterations(iterations) }
    }

    fun updatePermissionPreset(preset: String) {
        viewModelScope.launch { settingsDataStore.updatePermissionPreset(preset) }
    }

    fun updateAutoExtractMemory(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoExtractMemory(enabled) }
    }

    fun updateGitEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateGitEnabled(enabled) }
    }

    fun updateGitAutoCommit(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateGitAutoCommit(enabled) }
    }

    fun updateGitUserName(name: String) {
        viewModelScope.launch { settingsDataStore.updateGitUserName(name) }
    }

    fun updateGitUserEmail(email: String) {
        viewModelScope.launch { settingsDataStore.updateGitUserEmail(email) }
    }
}

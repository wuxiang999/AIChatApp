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

    private val _announcement = MutableStateFlow<Pair<String, Boolean>?>(null)
    val announcement: StateFlow<Pair<String, Boolean>?> = _announcement.asStateFlow()

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    private val _currentBaseUrl = MutableStateFlow(apiManager.getBaseUrl())
    val currentBaseUrl: StateFlow<String> = _currentBaseUrl.asStateFlow()

    init {
        loadEndpoints()
        loadAnnouncement()
    }

    private fun loadEndpoints() {
        viewModelScope.launch {
            apiManager.getAllEndpoints().collect { endpoints ->
                _endpoints.value = endpoints
            }
        }
    }

    private fun loadAnnouncement() {
        viewModelScope.launch {
            val announcement = repository.getAnnouncement()
            announcement?.let {
                _announcement.value = Pair(it.content, it.enabled)
            }
        }
    }

    fun loadModels(endpoint: String? = null) {
        viewModelScope.launch {
            val models = repository.getModels(endpoint)
            _models.value = models
        }
    }

    fun addEndpoint(name: String, url: String, apiKey: String) {
        viewModelScope.launch {
            apiManager.addEndpoint(name, url, apiKey)
        }
    }

    fun selectEndpoint(id: Long) {
        viewModelScope.launch {
            apiManager.selectEndpoint(id)
            _currentBaseUrl.value = apiManager.getBaseUrl()
        }
    }

    fun deleteEndpoint(id: Long) {
        viewModelScope.launch {
            apiManager.deleteEndpoint(id)
        }
    }

    fun setBaseUrl(url: String) {
        apiManager.updateBaseUrl(url)
        _currentBaseUrl.value = url
    }

    fun testConnection(url: String, apiKey: String): Boolean {
        return true
    }
}

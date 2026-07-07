package com.aichat.app.ui.imagegen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageGenViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _generatedImages = MutableStateFlow<List<String>>(emptyList())
    val generatedImages: StateFlow<List<String>> = _generatedImages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun generateImage(
        prompt: String,
        n: Int = 1,
        size: String = "1024x1024",
        model: String = "gpt-image-2",
        quality: String? = null
    ) {
        if (prompt.isBlank() || _isGenerating.value) return

        _error.value = null
        _isGenerating.value = true
        _generatedImages.value = emptyList()

        viewModelScope.launch {
            val result = repository.generateImage(
                prompt = prompt,
                n = n,
                size = size,
                model = model,
                quality = quality
            )
            result.onSuccess { urls ->
                _generatedImages.value = urls
            }
            result.onFailure { e ->
                _error.value = e.message ?: "生成失败"
            }
            _isGenerating.value = false
        }
    }
}

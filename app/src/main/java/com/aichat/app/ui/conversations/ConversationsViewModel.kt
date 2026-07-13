package com.aichat.app.ui.conversations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Conversation
import com.aichat.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            try {
                repository.getAllConversations().collect { conversations ->
                    _conversations.value = conversations
                }
            } catch (e: Exception) {
                Log.e("ConversationsViewModel", "loadConversations error", e)
            }
        }
    }

    suspend fun createConversation(title: String = "新对话"): Conversation {
        _isCreating.value = true
        return try {
            repository.createConversation(title)
        } finally {
            _isCreating.value = false
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(conversationId)
            } catch (e: Exception) {
                Log.e("ConversationsViewModel", "deleteConversation error", e)
            }
        }
    }

    fun updateConversationTitle(conversationId: String, title: String) {
        viewModelScope.launch {
            try {
                repository.updateConversationTitle(conversationId, title)
            } catch (e: Exception) {
                Log.e("ConversationsViewModel", "updateConversationTitle error", e)
            }
        }
    }
}

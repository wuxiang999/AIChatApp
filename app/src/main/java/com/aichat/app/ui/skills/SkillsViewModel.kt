package com.aichat.app.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Skill
import com.aichat.app.data.repository.SkillsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val repository: SkillsRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllSkills().collect { _skills.value = it }
        }
    }

    fun addSkill(name: String, description: String, promptTemplate: String) {
        viewModelScope.launch { repository.addSkill(name, description, promptTemplate) }
    }

    fun toggleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun deleteSkill(id: Long) {
        viewModelScope.launch { repository.deleteSkill(id) }
    }
}

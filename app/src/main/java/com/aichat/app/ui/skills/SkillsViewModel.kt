package com.aichat.app.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.model.Skill
import com.aichat.app.data.repository.SkillsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val repository: SkillsRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val _selectedCategory = MutableStateFlow("全部")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    /** 根据选中分类过滤后的技能列表 */
    val filteredSkills: StateFlow<List<Skill>> = combine(skills, selectedCategory) { allSkills, cat ->
        if (cat == "全部") allSkills
        else allSkills.filter { it.category == cat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllSkills().collect { all ->
                _skills.value = all
                // 收集所有分类
                val cats = repository.getAllCategories()
                _categories.value = cats
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addSkill(name: String, description: String, promptTemplate: String, category: String = "general", tags: String = "") {
        viewModelScope.launch {
            repository.addSkill(name, description, promptTemplate, category, tags)
            _categories.value = repository.getAllCategories()
        }
    }

    fun updateSkill(id: Long, name: String, description: String, promptTemplate: String, category: String, tags: String) {
        viewModelScope.launch {
            repository.updateSkill(id, name, description, promptTemplate, category, tags)
            _categories.value = repository.getAllCategories()
        }
    }

    fun toggleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun deleteSkill(id: Long) {
        viewModelScope.launch { repository.deleteSkill(id) }
    }
}

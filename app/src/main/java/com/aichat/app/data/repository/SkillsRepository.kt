package com.aichat.app.data.repository

import com.aichat.app.data.local.SkillDao
import com.aichat.app.data.model.Skill
import com.aichat.app.data.terminal.TerminalLogBuffer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillsRepository @Inject constructor(
    private val skillDao: SkillDao,
    private val terminal: TerminalLogBuffer
) {
    companion object {
        private const val TAG = "SkillsRepo"
    }

    fun getAllSkills(): Flow<List<Skill>> = skillDao.getAllSkills()

    suspend fun getEnabledSkills(): List<Skill> = skillDao.getEnabledSkills()

    suspend fun addSkill(
        name: String,
        description: String,
        promptTemplate: String,
        category: String = "general",
        tags: String = ""
    ): Long {
        val id = skillDao.insertSkill(
            Skill(name = name, description = description, promptTemplate = promptTemplate, category = category, tags = tags)
        )
        terminal.info(TAG, "添加技能: $name (category=$category)")
        return id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        skillDao.setEnabled(id, enabled)
        terminal.info(TAG, "${if (enabled) "启用" else "禁用"}技能 id=$id")
    }

    suspend fun deleteSkill(id: Long) {
        skillDao.deleteSkillById(id)
        terminal.info(TAG, "删除技能 id=$id")
    }

    suspend fun getEnabledSkillsByCategory(category: String): List<Skill> =
        skillDao.getEnabledSkillsByCategory(category)

    suspend fun getAllCategories(): List<String> =
        skillDao.getAllCategories()

    suspend fun updateSkill(
        id: Long,
        name: String,
        description: String,
        promptTemplate: String,
        category: String,
        tags: String
    ) {
        skillDao.updateSkill(id, name, description, promptTemplate, category, tags)
        terminal.info(TAG, "更新技能 id=$id: $name")
    }
}

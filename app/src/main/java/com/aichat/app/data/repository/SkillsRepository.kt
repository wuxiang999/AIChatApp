package com.aichat.app.data.repository

import com.aichat.app.data.local.SkillDao
import com.aichat.app.data.model.Skill
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillsRepository @Inject constructor(
    private val skillDao: SkillDao
) {
    fun getAllSkills(): Flow<List<Skill>> = skillDao.getAllSkills()

    suspend fun getEnabledSkills(): List<Skill> = skillDao.getEnabledSkills()

    suspend fun addSkill(name: String, description: String, promptTemplate: String): Long =
        skillDao.insertSkill(Skill(name = name, description = description, promptTemplate = promptTemplate))

    suspend fun setEnabled(id: Long, enabled: Boolean) = skillDao.setEnabled(id, enabled)

    suspend fun deleteSkill(id: Long) = skillDao.deleteSkillById(id)
}

package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.app.data.model.Skill
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY createdAt DESC")
    fun getAllSkills(): Flow<List<Skill>>

    @Query("SELECT * FROM skills WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledSkills(): List<Skill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: Skill): Long

    @Query("UPDATE skills SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: Long)

    @Query("SELECT * FROM skills WHERE enabled = 1 AND category = :category ORDER BY createdAt DESC")
    suspend fun getEnabledSkillsByCategory(category: String): List<Skill>

    @Query("SELECT DISTINCT category FROM skills WHERE category != '' ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("""
        UPDATE skills 
        SET name = :name, description = :description, promptTemplate = :promptTemplate, 
            category = :category, tags = :tags 
        WHERE id = :id
    """)
    suspend fun updateSkill(id: Long, name: String, description: String, promptTemplate: String, category: String, tags: String)
}

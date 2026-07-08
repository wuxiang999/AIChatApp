package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aichat.app.data.model.Agent
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY isPreset DESC, createdAt DESC")
    fun getAllAgents(): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): Agent?

    @Query("SELECT * FROM agents WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedAgent(): Agent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent)

    @Update
    suspend fun updateAgent(agent: Agent)

    @Delete
    suspend fun deleteAgent(agent: Agent)

    @Query("DELETE FROM agents WHERE id = :id AND isPreset = 0")
    suspend fun deleteAgentById(id: String)

    @Query("UPDATE agents SET isSelected = 0")
    suspend fun clearAllSelected()

    @Query("UPDATE agents SET isSelected = 1 WHERE id = :id")
    suspend fun selectAgent(id: String)

    @Query("SELECT COUNT(*) FROM agents")
    suspend fun getAgentCount(): Int
}

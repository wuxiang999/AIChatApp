package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aichat.app.data.model.Agent
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY createdAt DESC")
    fun getAllAgents(): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedAgent(): Agent?

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: Long): Agent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent): Long

    @Update
    suspend fun updateAgent(agent: Agent)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun deleteAgent(id: Long)

    @Query("UPDATE agents SET isSelected = 0")
    suspend fun clearSelected()

    @Query("UPDATE agents SET isSelected = 1 WHERE id = :id")
    suspend fun setSelected(id: Long)
}

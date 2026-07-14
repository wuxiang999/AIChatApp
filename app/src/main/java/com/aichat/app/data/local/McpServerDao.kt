package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.app.data.model.McpServer
import kotlinx.coroutines.flow.Flow

@Dao
interface McpServerDao {
    @Query("SELECT * FROM mcp_servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<McpServer>>

    @Query("SELECT * FROM mcp_servers WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledServers(): List<McpServer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServer): Long

    @Query("UPDATE mcp_servers SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)
}

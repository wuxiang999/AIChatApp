package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aichat.app.data.model.ApiEndpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiEndpointDao {
    @Query("SELECT * FROM api_endpoints ORDER BY id ASC")
    fun getAllEndpoints(): Flow<List<ApiEndpoint>>

    @Query("SELECT * FROM api_endpoints WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedEndpoint(): ApiEndpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: ApiEndpoint)

    @Update
    suspend fun updateEndpoint(endpoint: ApiEndpoint)

    @Query("DELETE FROM api_endpoints WHERE id = :id")
    suspend fun deleteEndpoint(id: Long)

    @Query("UPDATE api_endpoints SET isSelected = 0")
    suspend fun clearSelected()

    @Query("UPDATE api_endpoints SET isSelected = 1 WHERE id = :id")
    suspend fun setSelected(id: Long)
}

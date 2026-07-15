package com.aichat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.app.data.model.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    suspend fun getAllMemoriesOnce(): List<Memory>

    @Query("SELECT * FROM memories WHERE source = :source ORDER BY createdAt DESC")
    suspend fun getMemoriesBySource(source: String): List<Memory>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    suspend fun searchMemories(keyword: String): List<Memory>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :keyword || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchMemories(keyword: String, limit: Int): List<Memory>

    @Query("SELECT COUNT(*) FROM memories WHERE content = :content AND source = :source")
    suspend fun countByContent(content: String, source: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Query("UPDATE memories SET content = :content, category = :category, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMemory(id: Long, content: String, category: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("SELECT * FROM memories ORDER BY importance DESC, accessCount DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 20): List<Memory>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun getMemoriesByCategory(category: String): List<Memory>

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessAt = :now WHERE id = :id")
    suspend fun recordAccess(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET importance = :importance WHERE id = :id")
    suspend fun updateImportance(id: Long, importance: Int)

    @Query("DELETE FROM memories WHERE importance < :minImportance AND createdAt < :before")
    suspend fun pruneMemories(minImportance: Int, before: Long)

    @Query("SELECT DISTINCT category FROM memories WHERE category != ''")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoryCount(): Int

    @Query("SELECT DISTINCT source FROM memories")
    suspend fun getAllSources(): List<String>

    @Query("UPDATE memories SET content = :content, tags = :tags, importance = :importance, updatedAt = :now WHERE id = :id")
    suspend fun updateMemory(id: Long, content: String, tags: String, importance: Int, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM memories ORDER BY lastAccessAt DESC LIMIT :limit")
    suspend fun getRecentlyAccessedMemories(limit: Int = 20): List<Memory>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 20): List<Memory>

    @Query("SELECT * FROM memories WHERE category = :category AND importance >= :minImportance ORDER BY importance DESC")
    suspend fun getMemoriesByCategoryWithImportance(category: String, minImportance: Int = 3): List<Memory>
}

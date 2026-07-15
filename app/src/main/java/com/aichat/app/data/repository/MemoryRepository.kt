package com.aichat.app.data.repository

import com.aichat.app.data.local.MemoryDao
import com.aichat.app.data.model.Memory
import com.aichat.app.data.terminal.TerminalLogBuffer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao,
    private val terminal: TerminalLogBuffer
) {
    companion object {
        private const val TAG = "MemoryRepo"
    }

    fun getAllMemories(): Flow<List<Memory>> = memoryDao.getAllMemories()

    suspend fun getAllMemoriesOnce(): List<Memory> = memoryDao.getAllMemoriesOnce()

    suspend fun getMemoriesBySource(source: String): List<Memory> =
        memoryDao.getMemoriesBySource(source)

    suspend fun searchMemories(keyword: String, limit: Int = 5): List<Memory> =
        memoryDao.searchMemories(keyword, limit)

    suspend fun addMemory(
        content: String,
        category: String = "general",
        source: String = "manual",
        conversationId: String? = null
    ): Long {
        val memory = Memory(
            content = content,
            category = category,
            source = source,
            conversationId = conversationId
        )
        val id = memoryDao.insertMemory(memory)
        terminal.memory(TAG, "添加记忆 [${category}]: ${content.take(80)}")
        return id
    }

    suspend fun insertMemory(memory: Memory): Long {
        val id = memoryDao.insertMemory(memory)
        terminal.memory(TAG, "插入记忆 [${memory.category}]: ${memory.content.take(80)}")
        return id
    }

    suspend fun countByContent(content: String, source: String): Int =
        memoryDao.countByContent(content, source)

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
        terminal.memory(TAG, "删除记忆 id=$id")
    }

    suspend fun getMemoriesByCategory(category: String): List<Memory> =
        memoryDao.getMemoriesByCategory(category)

    suspend fun recordAccess(id: Long, now: Long = System.currentTimeMillis()) {
        memoryDao.recordAccess(id, now)
    }

    suspend fun updateMemory(id: Long, content: String, tags: String, importance: Int) {
        memoryDao.updateMemory(id, content, tags, importance)
    }

    suspend fun updateImportance(id: Long, importance: Int) {
        memoryDao.updateImportance(id, importance)
    }

    suspend fun pruneMemories(minImportance: Int, before: Long) {
        memoryDao.pruneMemories(minImportance, before)
    }

    suspend fun getMemoryCount(): Int = memoryDao.getMemoryCount()

    suspend fun getAllCategories(): List<String> = memoryDao.getAllCategories()

    suspend fun getAllSources(): List<String> = memoryDao.getAllSources()
}

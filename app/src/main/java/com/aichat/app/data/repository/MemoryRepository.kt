package com.aichat.app.data.repository

import com.aichat.app.data.local.MemoryDao
import com.aichat.app.data.model.Memory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    fun getAllMemories(): Flow<List<Memory>> = memoryDao.getAllMemories()

    suspend fun getAllMemoriesOnce(): List<Memory> = memoryDao.getAllMemoriesOnce()

    suspend fun addMemory(content: String, category: String = "general"): Long =
        memoryDao.insertMemory(Memory(content = content, category = category))

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)
}

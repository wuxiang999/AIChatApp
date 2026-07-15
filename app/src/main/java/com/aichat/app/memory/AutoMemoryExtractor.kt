package com.aichat.app.memory

import android.util.Log
import com.aichat.app.data.model.Memory
import com.aichat.app.data.remote.ApiManager
import com.aichat.app.data.remote.ChatMessage
import com.aichat.app.data.remote.ChatRequest
import com.aichat.app.data.repository.MemoryRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AutoMemoryExtractor @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val apiManager: ApiManager
) {
    private val gson = Gson()

    companion object {
        private const val TAG = "AutoMemoryExtractor"
        private const val EXTRACTION_MODEL = "gpt-4o-mini"
    }

    suspend fun extractFromConversation(
        conversationId: String,
        messages: List<com.aichat.app.data.model.Message>,
        model: String = EXTRACTION_MODEL
    ): List<Memory> {
        val recentTurns = messages.takeLast(10)
            .filter { !it.isRevoked }
            .map { "${it.role}: ${it.content}" }
            .joinToString("\n")

        if (recentTurns.isBlank()) return emptyList()

        val prompt = """
From the following conversation, extract key information about the user and task.
Notes:
- Only extract persistent valuable information
- Summarize each item in one sentence
- Categorize as: fact / preference / identity / knowledge / task
- Rate importance 1-10
- Optional tags separated by commas

Conversation:
$recentTurns

Return as JSON (max 10 items):
{"memories": [{"content": "...", "category": "fact", "importance": 7, "tags": "..."}]}
If no new info, return {"memories": []}
        """.trimIndent()

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = "You are an information extractor. Extract important user information from conversations."),
                ChatMessage(role = "user", content = prompt)
            ),
            stream = false
        )

        return try {
            val response = apiManager.getApiService().chatCompletion(
                auth = apiManager.getAuthHeader(),
                request = request
            )
            val content = response.choices?.firstOrNull()?.message?.content
                ?: return emptyList()

            val memories = parseExtractionResult(content)
            val newMemories = deduplicateAndSave(memories, conversationId)
            Log.d(TAG, "Extracted ${newMemories.size} new memories from conversation $conversationId")
            newMemories
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            emptyList()
        }
    }

    private fun parseExtractionResult(jsonContent: String): List<ExtractedMemory> {
        return try {
            val root = JSONObject(jsonContent)
            val memoriesJson = root.optJSONArray("memories") ?: return emptyList()
            val result = mutableListOf<ExtractedMemory>()
            for (i in 0 until memoriesJson.length()) {
                val obj = memoriesJson.getJSONObject(i)
                val content = obj.optString("content", "") ?: ""
                if (content.isEmpty()) continue
                result.add(ExtractedMemory(
                    content = content,
                    category = obj.optString("category", "fact") ?: "fact",
                    importance = obj.optInt("importance", 5),
                    tags = obj.optString("tags", "") ?: ""
                ))
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse LLM extraction result", e)
            emptyList()
        }
    }

    private suspend fun deduplicateAndSave(
        extracted: List<ExtractedMemory>,
        conversationId: String
    ): List<Memory> {
        val newMemories = mutableListOf<Memory>()
        for (item in extracted) {
            val exists = memoryRepository.countByContent(item.content, "auto") > 0
            if (!exists) {
                val memory = Memory(
                    content = item.content,
                    category = item.category,
                    source = "auto",
                    conversationId = conversationId,
                    importance = item.importance.coerceIn(1, 10),
                    tags = item.tags
                )
                memoryRepository.insertMemory(memory)
                newMemories.add(memory)
            }
        }
        return newMemories
    }

    suspend fun consolidate() {
        try {
            val allMemories = memoryRepository.getAllMemoriesOnce()
            if (allMemories.isEmpty()) return

            val now = System.currentTimeMillis()
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val ninetyDays = 90L * 24 * 60 * 60 * 1000

            val grouped = allMemories.groupBy { normalize(it.content) }
            for ((_, group) in grouped) {
                if (group.size > 1) {
                    val best = group.maxBy { it.importance }
                    val mergedContent = group.map { it.content }.distinct().joinToString("; ")
                    val mergedTags = group.flatMap { it.tags.split(",").map { t -> t.trim() } }
                        .filter { it.isNotEmpty() }.distinct().joinToString(",")

                    memoryRepository.updateMemory(
                        id = best.id,
                        content = mergedContent,
                        tags = mergedTags,
                        importance = (best.importance + 1).coerceAtMost(10)
                    )
                    for (m in group) {
                        if (m.id != best.id) {
                            memoryRepository.deleteMemory(m.id)
                        }
                    }
                }
            }

            for (m in allMemories) {
                if (m.lastAccessAt > 0 && (now - m.lastAccessAt) > thirtyDays && m.importance > 1) {
                    memoryRepository.updateImportance(m.id, (m.importance - 1).coerceAtLeast(1))
                }
            }

            memoryRepository.pruneMemories(2, now - ninetyDays)

            Log.d(TAG, "Consolidation completed")
        } catch (e: Exception) {
            Log.e(TAG, "Consolidation failed", e)
        }
    }

    private fun normalize(text: String): String {
        return text.replace(Regex("[\\s,，。！？、；：]"), "")
            .take(20)
            .lowercase()
    }

    private data class ExtractedMemory(
        val content: String,
        val category: String,
        val importance: Int = 5,
        val tags: String = ""
    )
}

package com.aichat.app.data.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalLogBuffer @Inject constructor() {
    companion object {
        private const val MAX_ENTRIES = 1000
    }

    data class LogEntry(
        val id: Long = System.nanoTime(),
        val timestamp: Long = System.currentTimeMillis(),
        val level: LogLevel,
        val tag: String,
        val message: String,
        val source: String = "system",
        val toolName: String? = null,
        val details: String? = null
    ) {
        fun formattedTime(): String =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR, TOOL_CALL, TOOL_RESULT, MEMORY, NETWORK
    }

    private val _logs = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(
        level: LogLevel, tag: String, message: String,
        source: String = "system", toolName: String? = null, details: String? = null
    ) {
        val entry = LogEntry(
            level = level, tag = tag, message = message,
            source = source, toolName = toolName, details = details
        )
        _logs.add(entry)
        if (_logs.size > MAX_ENTRIES) _logs.removeAt(0)
        _logsFlow.value = _logs.toList()
    }

    fun debug(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg)
    fun info(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)
    fun warn(tag: String, msg: String) = log(LogLevel.WARN, tag, msg)
    fun error(tag: String, msg: String) = log(LogLevel.ERROR, tag, msg)
    fun success(tag: String, msg: String) = log(LogLevel.INFO, tag, msg)

    fun toolCall(tag: String, toolName: String, args: String) =
        log(LogLevel.TOOL_CALL, tag, "$toolName($args)", source = "tool", toolName = toolName)
    fun toolResult(tag: String, toolName: String, result: String) =
        log(LogLevel.TOOL_RESULT, tag, "$toolName \u2192 ${result.take(200)}", source = "tool", toolName = toolName)

    fun memory(tag: String, msg: String) = log(LogLevel.MEMORY, tag, msg, source = "memory")
    fun network(tag: String, msg: String) = log(LogLevel.NETWORK, tag, msg, source = "network")

    @Synchronized
    fun clear() {
        _logs.clear()
        _logsFlow.value = emptyList()
    }

    @Synchronized
    fun getLogsByLevel(level: LogLevel): List<LogEntry> = _logs.filter { it.level == level }

    @Synchronized
    fun getLogsBySource(source: String): List<LogEntry> = _logs.filter { it.source == source }

    @Synchronized
    fun search(query: String): List<LogEntry> = _logs.filter {
        it.message.contains(query, ignoreCase = true) ||
        it.tag.contains(query, ignoreCase = true) ||
        it.toolName?.contains(query, ignoreCase = true) == true
    }

    @Synchronized
    fun export(): String = _logs.joinToString("\n") {
        "[${it.timestamp}] [${it.level}] [${it.tag}] ${it.message}"
    }
}

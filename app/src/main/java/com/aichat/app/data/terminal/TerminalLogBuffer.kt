package com.aichat.app.data.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 终端日志条目 */
data class TerminalLog(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    enum class LogLevel { INFO, WARN, ERROR, SUCCESS }

    fun formattedTime(): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

/**
 * 应用内终端日志缓冲：内存中有界环形列表，供「终端」页面实时展示。
 * 被 [com.aichat.app.data.repository.ChatRepository] / MCP / ViewModel 共享写入。
 */
@Singleton
class TerminalLogBuffer @Inject constructor() {
    private val capacity = 500

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    fun log(level: TerminalLog.LogLevel, tag: String, message: String) {
        _logs.update { existing ->
            val next = existing + TerminalLog(level = level, tag = tag, message = message)
            if (next.size > capacity) next.takeLast(capacity) else next
        }
    }

    fun info(tag: String, message: String) = log(TerminalLog.LogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = log(TerminalLog.LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = log(TerminalLog.LogLevel.ERROR, tag, message)
    fun success(tag: String, message: String) = log(TerminalLog.LogLevel.SUCCESS, tag, message)

    fun clear() {
        _logs.value = emptyList()
    }
}

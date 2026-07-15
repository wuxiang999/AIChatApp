package com.aichat.app.agent

import com.aichat.app.agent.tools.CalculateTool
import com.aichat.app.agent.tools.CloseTerminalSessionTool
import com.aichat.app.agent.tools.CopyFileTool
import com.aichat.app.agent.tools.CreateMemoryTool
import com.aichat.app.agent.tools.CreateTerminalSessionTool
import com.aichat.app.agent.tools.DeleteFileTool
import com.aichat.app.agent.tools.DeviceInfoTool
import com.aichat.app.agent.tools.EditFileTool
import com.aichat.app.agent.tools.ExecuteInTerminalSessionTool
import com.aichat.app.agent.tools.ExecuteShellTool
import com.aichat.app.agent.tools.FileExistsTool
import com.aichat.app.agent.tools.FileInfoTool
import com.aichat.app.agent.tools.FindFilesTool
import com.aichat.app.agent.tools.GrepCodeTool
import com.aichat.app.agent.tools.HttpRequestTool
import com.aichat.app.agent.tools.ListFilesTool
import com.aichat.app.agent.tools.MakeDirectoryTool
import com.aichat.app.agent.tools.MoveFileTool
import com.aichat.app.agent.tools.QueryMemoryTool
import com.aichat.app.agent.tools.FileReadTool
import com.aichat.app.agent.tools.FileWriteTool
import com.aichat.app.agent.tools.SleepTool
import com.aichat.app.agent.tools.UnzipFilesTool
import com.aichat.app.agent.tools.VisitWebTool
import com.aichat.app.agent.tools.ZipFilesTool
import com.aichat.app.agent.tools.DeleteMemoryTool

/**
 * Factory that creates all 26 adapted Operit tools as CodeVibe ITool instances.
 *
 * Usage:
 *   val tools = CodeVibeToolFactory.create()
 *   tools.forEach { toolRegistry.register(it) }
 *
 * This factory is designed for non-Hilt environments.
 * For Hilt DI, use CodeVibeToolRegistration directly.
 */
object CodeVibeToolFactory {

    fun create(): List<ITool> = listOf(
        // File System (14 tools)
        ListFilesTool(),
        FileReadTool(),
        FileWriteTool(),
        EditFileTool(),
        DeleteFileTool(),
        CopyFileTool(),
        MakeDirectoryTool(),
        FindFilesTool(),
        FileInfoTool(),
        FileExistsTool(),
        MoveFileTool(),
        GrepCodeTool(),
        ZipFilesTool(),
        UnzipFilesTool(),

        // Terminal (4 tools)
        ExecuteShellTool(),
        CreateTerminalSessionTool(),
        ExecuteInTerminalSessionTool(),
        CloseTerminalSessionTool(),

        // Web (2 tools)
        VisitWebTool(),
        HttpRequestTool(),

        // Memory (3 tools)
        QueryMemoryTool(),
        CreateMemoryTool(),
        DeleteMemoryTool(),

        // Utility (3 tools)
        SleepTool(),
        CalculateTool(),
        DeviceInfoTool()
    )
}

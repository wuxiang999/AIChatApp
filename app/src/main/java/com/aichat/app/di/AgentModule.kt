package com.aichat.app.di

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ToolRegistry
import com.aichat.app.agent.tools.BashTool
import com.aichat.app.agent.tools.CodeSandboxTool
import com.aichat.app.agent.tools.ImageTool
import com.aichat.app.agent.tools.MemorySearchTool
import com.aichat.app.agent.tools.ReadFileTool
import com.aichat.app.agent.tools.TerminalLogTool
import com.aichat.app.agent.tools.WebSearchTool
import com.aichat.app.agent.tools.WriteFileTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideToolRegistry(): ToolRegistry {
        return ToolRegistry()
    }

    @Provides
    @Singleton
    fun provideCoreTools(
        registry: ToolRegistry,
        readFileTool: ReadFileTool,
        writeFileTool: WriteFileTool,
        webSearchTool: WebSearchTool,
        bashTool: BashTool,
        codeSandboxTool: CodeSandboxTool,
        imageTool: ImageTool,
        memorySearchTool: MemorySearchTool,
        terminalLogTool: TerminalLogTool
    ): List<ITool> {
        val tools = listOf(
            readFileTool,
            writeFileTool,
            webSearchTool,
            bashTool,
            codeSandboxTool,
            imageTool,
            memorySearchTool,
            terminalLogTool
        )
        registry.registerAll(tools)
        return tools
    }
}

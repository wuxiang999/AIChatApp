package com.aichat.app.di

import com.aichat.app.permission.PermissionEffect
import com.aichat.app.permission.PermissionEngine
import com.aichat.app.permission.PermissionRule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the Permission system.
 *
 * Configures default permission rules:
 * - Read-only tools: auto-allow
 * - Write/Execute tools: ask for approval
 * - Unknown tools: ask (safe default)
 */
@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionEngine(): PermissionEngine {
        val engine = PermissionEngine()

        engine.configure(
            listOf(
                // Read-only tools — auto allow
                PermissionRule("read_file", "*", PermissionEffect.ALLOW),
                PermissionRule("web_search", "*", PermissionEffect.ALLOW),

                // Write tools — ask for approval
                PermissionRule("write_file", "*", PermissionEffect.ASK),

                // MCP tools — ask by default
                PermissionRule("mcp_tool", "*", PermissionEffect.ASK),

                // Bash — ask for approval
                PermissionRule("bash", "*", PermissionEffect.ASK),

                // Image generation — auto allow
                PermissionRule("image_generate", "*", PermissionEffect.ALLOW),

                // Memory search — auto allow
                PermissionRule("memory_search", "*", PermissionEffect.ALLOW),

                // Code sandbox — ask for approval
                PermissionRule("code_sandbox", "*", PermissionEffect.ASK),

                // Wildcard fallback is NOT needed — PermissionEngine defaults to ASK
            )
        )

        return engine
    }
}

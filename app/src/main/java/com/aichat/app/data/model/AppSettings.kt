package com.aichat.app.data.model

data class AppSettings(
    val workspacePath: String = "/storage/emulated/0/Download",
    val autoSaveEnabled: Boolean = true,
    val autoSaveInterval: Int = 30,
    val fontSize: Int = 14,
    val editorTheme: String = "dark",
    val showLineNumbers: Boolean = true,
    val tabSize: Int = 4,
    val defaultCodingModel: String = "gpt-4o",
    val maxIterations: Int = 10,
    val permissionPreset: String = "balanced",
    val autoExtractMemory: Boolean = true,
    val gitEnabled: Boolean = false,
    val gitAutoCommit: Boolean = false,
    val gitUserName: String = "",
    val gitUserEmail: String = "",
    val lastExportPath: String = ""
)

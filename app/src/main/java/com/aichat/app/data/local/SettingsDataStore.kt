package com.aichat.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aichat.app.data.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            workspacePath = prefs[WORKSPACE_PATH] ?: AppSettings().workspacePath,
            autoSaveEnabled = prefs[AUTO_SAVE_ENABLED] ?: AppSettings().autoSaveEnabled,
            autoSaveInterval = prefs[AUTO_SAVE_INTERVAL] ?: AppSettings().autoSaveInterval,
            fontSize = prefs[FONT_SIZE] ?: AppSettings().fontSize,
            editorTheme = prefs[EDITOR_THEME] ?: AppSettings().editorTheme,
            showLineNumbers = prefs[SHOW_LINE_NUMBERS] ?: AppSettings().showLineNumbers,
            tabSize = prefs[TAB_SIZE] ?: AppSettings().tabSize,
            defaultCodingModel = prefs[DEFAULT_CODING_MODEL] ?: AppSettings().defaultCodingModel,
            maxIterations = prefs[MAX_ITERATIONS] ?: AppSettings().maxIterations,
            permissionPreset = prefs[PERMISSION_PRESET] ?: AppSettings().permissionPreset,
            autoExtractMemory = prefs[AUTO_EXTRACT_MEMORY] ?: AppSettings().autoExtractMemory,
            gitEnabled = prefs[GIT_ENABLED] ?: AppSettings().gitEnabled,
            gitAutoCommit = prefs[GIT_AUTO_COMMIT] ?: AppSettings().gitAutoCommit,
            gitUserName = prefs[GIT_USER_NAME] ?: AppSettings().gitUserName,
            gitUserEmail = prefs[GIT_USER_EMAIL] ?: AppSettings().gitUserEmail,
            lastExportPath = prefs[LAST_EXPORT_PATH] ?: AppSettings().lastExportPath
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[WORKSPACE_PATH] = settings.workspacePath
            prefs[AUTO_SAVE_ENABLED] = settings.autoSaveEnabled
            prefs[AUTO_SAVE_INTERVAL] = settings.autoSaveInterval
            prefs[FONT_SIZE] = settings.fontSize
            prefs[EDITOR_THEME] = settings.editorTheme
            prefs[SHOW_LINE_NUMBERS] = settings.showLineNumbers
            prefs[TAB_SIZE] = settings.tabSize
            prefs[DEFAULT_CODING_MODEL] = settings.defaultCodingModel
            prefs[MAX_ITERATIONS] = settings.maxIterations
            prefs[PERMISSION_PRESET] = settings.permissionPreset
            prefs[AUTO_EXTRACT_MEMORY] = settings.autoExtractMemory
            prefs[GIT_ENABLED] = settings.gitEnabled
            prefs[GIT_AUTO_COMMIT] = settings.gitAutoCommit
            prefs[GIT_USER_NAME] = settings.gitUserName
            prefs[GIT_USER_EMAIL] = settings.gitUserEmail
            prefs[LAST_EXPORT_PATH] = settings.lastExportPath
        }
    }

    suspend fun updateWorkspacePath(path: String) {
        context.dataStore.edit { prefs -> prefs[WORKSPACE_PATH] = path }
    }

    suspend fun updateAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_SAVE_ENABLED] = enabled }
    }

    suspend fun updateAutoSaveInterval(interval: Int) {
        context.dataStore.edit { prefs -> prefs[AUTO_SAVE_INTERVAL] = interval }
    }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[FONT_SIZE] = size }
    }

    suspend fun updateEditorTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[EDITOR_THEME] = theme }
    }

    suspend fun updateShowLineNumbers(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_LINE_NUMBERS] = show }
    }

    suspend fun updateTabSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[TAB_SIZE] = size }
    }

    suspend fun updateDefaultCodingModel(model: String) {
        context.dataStore.edit { prefs -> prefs[DEFAULT_CODING_MODEL] = model }
    }

    suspend fun updateMaxIterations(iterations: Int) {
        context.dataStore.edit { prefs -> prefs[MAX_ITERATIONS] = iterations }
    }

    suspend fun updatePermissionPreset(preset: String) {
        context.dataStore.edit { prefs -> prefs[PERMISSION_PRESET] = preset }
    }

    suspend fun updateAutoExtractMemory(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_EXTRACT_MEMORY] = enabled }
    }

    suspend fun updateGitEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[GIT_ENABLED] = enabled }
    }

    suspend fun updateGitAutoCommit(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[GIT_AUTO_COMMIT] = enabled }
    }

    suspend fun updateGitUserName(name: String) {
        context.dataStore.edit { prefs -> prefs[GIT_USER_NAME] = name }
    }

    suspend fun updateGitUserEmail(email: String) {
        context.dataStore.edit { prefs -> prefs[GIT_USER_EMAIL] = email }
    }

    companion object {
        private val WORKSPACE_PATH = stringPreferencesKey("workspace_path")
        private val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        private val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
        private val FONT_SIZE = intPreferencesKey("font_size")
        private val EDITOR_THEME = stringPreferencesKey("editor_theme")
        private val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        private val TAB_SIZE = intPreferencesKey("tab_size")
        private val DEFAULT_CODING_MODEL = stringPreferencesKey("default_coding_model")
        private val MAX_ITERATIONS = intPreferencesKey("max_iterations")
        private val PERMISSION_PRESET = stringPreferencesKey("permission_preset")
        private val AUTO_EXTRACT_MEMORY = booleanPreferencesKey("auto_extract_memory")
        private val GIT_ENABLED = booleanPreferencesKey("git_enabled")
        private val GIT_AUTO_COMMIT = booleanPreferencesKey("git_auto_commit")
        private val GIT_USER_NAME = stringPreferencesKey("git_user_name")
        private val GIT_USER_EMAIL = stringPreferencesKey("git_user_email")
        private val LAST_EXPORT_PATH = stringPreferencesKey("last_export_path")
    }
}

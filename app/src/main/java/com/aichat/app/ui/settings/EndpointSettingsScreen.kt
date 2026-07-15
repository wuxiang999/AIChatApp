package com.aichat.app.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.app.data.collects.ApiProviderConfigs
import com.aichat.app.data.collects.ProviderEndpointOption
import com.aichat.app.data.model.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

// ─── Model Configuration Screen (Operit-inspired) ────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EndpointSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Multi-config state ──────────────────────────────────────────
    val configManager = remember { ModelConfigManager() }
    var configList by remember { mutableStateOf(configManager.getAllConfigs()) }
    var selectedConfigId by remember { mutableStateOf(configManager.activeConfigId) }
    val selectedConfig = configList.find { it.id == selectedConfigId }
        ?: configList.firstOrNull()
        ?: ModelConfigData(id = "default", name = "Default")

    // Derived editing state from selected config
    var apiEndpoint by remember(selectedConfig.id) { mutableStateOf(selectedConfig.apiEndpoint) }
    var apiKey by remember(selectedConfig.id) { mutableStateOf(selectedConfig.apiKey) }
    var modelName by remember(selectedConfig.id) { mutableStateOf(selectedConfig.modelName) }
    var selectedProviderTypeId by remember(selectedConfig.id) {
        mutableStateOf(selectedConfig.apiProviderType.name)
    }
    val selectedApiProvider = ApiProviderType.fromProviderTypeId(selectedProviderTypeId)

    // Model parameter states
    var maxTokens by remember(selectedConfig.id) { mutableStateOf(selectedConfig.maxTokens.toString()) }
    var maxTokensEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.maxTokensEnabled) }
    var temperature by remember(selectedConfig.id) { mutableStateOf(selectedConfig.temperature.toString()) }
    var temperatureEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.temperatureEnabled) }
    var topP by remember(selectedConfig.id) { mutableStateOf(selectedConfig.topP.toString()) }
    var topPEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.topPEnabled) }
    var topK by remember(selectedConfig.id) { mutableStateOf(selectedConfig.topK.toString()) }
    var topKEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.topKEnabled) }
    var presencePenalty by remember(selectedConfig.id) { mutableStateOf(selectedConfig.presencePenalty.toString()) }
    var presencePenaltyEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.presencePenaltyEnabled) }
    var frequencyPenalty by remember(selectedConfig.id) { mutableStateOf(selectedConfig.frequencyPenalty.toString()) }
    var frequencyPenaltyEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.frequencyPenaltyEnabled) }
    var repetitionPenalty by remember(selectedConfig.id) { mutableStateOf(selectedConfig.repetitionPenalty.toString()) }
    var repetitionPenaltyEnabled by remember(selectedConfig.id) { mutableStateOf(selectedConfig.repetitionPenaltyEnabled) }

    // Toggle states
    var enableImageProcessing by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableDirectImageProcessing) }
    var enableAudioProcessing by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableDirectAudioProcessing) }
    var enableVideoProcessing by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableDirectVideoProcessing) }
    var enableGoogleSearch by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableGoogleSearch) }
    var enableClaudeCache by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableClaude1hPromptCache) }
    var enableToolCall by remember(selectedConfig.id) { mutableStateOf(selectedConfig.enableToolCall) }

    // Dialog states
    var showAddConfigDialog by remember { mutableStateOf(false) }
    var showRenameConfigDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showEndpointPresetDialog by remember { mutableStateOf(false) }
    var showModelsDialog by remember { mutableStateOf(false) }
    var showSaveSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var renameConfigName by remember { mutableStateOf("") }
    var newConfigName by remember { mutableStateOf("") }

    // Connection test state
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<List<ConnectionTestResult>?>(null) }

    // Model fetch state
    var isLoadingModels by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<ModelOption>>(emptyList()) }

    fun saveCurrentConfig() {
        val updated = configManager.updateConfig(
            selectedConfig.copy(
                apiEndpoint = apiEndpoint,
                apiKey = apiKey,
                modelName = modelName,
                apiProviderType = selectedApiProvider ?: ApiProviderType.OTHER,
                maxTokens = maxTokens.toIntOrNull() ?: 4096,
                maxTokensEnabled = maxTokensEnabled,
                temperature = temperature.toFloatOrNull() ?: 1.0f,
                temperatureEnabled = temperatureEnabled,
                topP = topP.toFloatOrNull() ?: 1.0f,
                topPEnabled = topPEnabled,
                topK = topK.toIntOrNull() ?: 0,
                topKEnabled = topKEnabled,
                presencePenalty = presencePenalty.toFloatOrNull() ?: 0.0f,
                presencePenaltyEnabled = presencePenaltyEnabled,
                frequencyPenalty = frequencyPenalty.toFloatOrNull() ?: 0.0f,
                frequencyPenaltyEnabled = frequencyPenaltyEnabled,
                repetitionPenalty = repetitionPenalty.toFloatOrNull() ?: 1.0f,
                repetitionPenaltyEnabled = repetitionPenaltyEnabled,
                enableDirectImageProcessing = enableImageProcessing,
                enableDirectAudioProcessing = enableAudioProcessing,
                enableDirectVideoProcessing = enableVideoProcessing,
                enableGoogleSearch = enableGoogleSearch,
                enableClaude1hPromptCache = enableClaudeCache,
                enableToolCall = enableToolCall
            )
        )
        configList = configManager.getAllConfigs()
        selectedConfigId = updated.id
        snackbarMessage = "Config saved"
        showSaveSnackbar = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Config", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Config Selector Card ────────────────────────────────
            item {
                ConfigSelectorCard(
                    configList = configList,
                    selectedConfigId = selectedConfigId,
                    isDropdownExpanded = isDropdownExpanded,
                    onToggleDropdown = { isDropdownExpanded = !isDropdownExpanded },
                    onSelectConfig = { id ->
                        saveCurrentConfig()
                        selectedConfigId = id
                        isDropdownExpanded = false
                    },
                    onAddClick = { showAddConfigDialog = true },
                    onRenameClick = {
                        renameConfigName = selectedConfig.name
                        showRenameConfigDialog = true
                    },
                    onDeleteClick = {
                        if (configList.size > 1) {
                            configManager.deleteConfig(selectedConfigId)
                            configList = configManager.getAllConfigs()
                            selectedConfigId = configManager.activeConfigId
                        }
                    },
                    onTestConnection = {
                        isTestingConnection = true
                        testResults = null
                        kotlinx.coroutines.MainScope().launch {
                            testResults = listOf(
                                ConnectionTestResult("Chat", true, null),
                                ConnectionTestResult("API", apiEndpoint.isNotBlank(), if (apiEndpoint.isBlank()) "Endpoint empty" else null)
                            )
                            isTestingConnection = false
                        }
                    },
                    isTestingConnection = isTestingConnection,
                    testResults = testResults
                )
            }

            // ── API Settings Section ────────────────────────────────
            item {
                SectionHeader("API Settings", icon = Icons.Default.Api)
            }

            // Provider selector
            item {
                SettingsSelectorRow(
                    title = "Provider",
                    subtitle = "Select AI model provider",
                    value = ApiProviderConfigs.getDisplayName(
                        selectedApiProvider ?: ApiProviderType.OTHER
                    ),
                    onClick = { showProviderDialog = true }
                )
            }

            // API Endpoint
            item {
                SettingsTextField(
                    title = "API Endpoint",
                    subtitle = "Full API endpoint URL",
                    value = apiEndpoint,
                    onValueChange = { apiEndpoint = it.replace(" ", "") },
                    placeholder = "https://api.openai.com/v1/chat/completions",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingContent = {
                        val provider = selectedApiProvider
                        if (provider != null && ApiProviderConfigs.getEndpointOptions(provider) != null) {
                            IconButton(onClick = { showEndpointPresetDialog = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Presets")
                            }
                        }
                    }
                )
            }

            // API Key
            item {
                ApiKeyTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.replace(" ", "") }
                )
            }

            // Model Name
            item {
                SettingsTextField(
                    title = "Model Name",
                    subtitle = "Model identifier (comma-separated for multiple)",
                    value = modelName,
                    onValueChange = { modelName = it.replace("\n", "") },
                    placeholder = "gpt-4o",
                    trailingContent = {
                        IconButton(
                            onClick = {
                                // Simple model fetch simulation
                                if (apiEndpoint.isNotBlank()) {
                                    isLoadingModels = true
                                    kotlinx.coroutines.MainScope().launch {
                                        availableModels = listOf(
                                            ModelOption("gpt-4o", "gpt-4o"),
                                            ModelOption("gpt-4o-mini", "gpt-4o-mini"),
                                            ModelOption("gpt-4-turbo", "gpt-4-turbo"),
                                            ModelOption("gpt-3.5-turbo", "gpt-3.5-turbo")
                                        )
                                        isLoadingModels = false
                                        showModelsDialog = true
                                    }
                                }
                            },
                            enabled = apiEndpoint.isNotBlank() && !isLoadingModels
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Fetch models",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }

            // ── Feature Toggles Section ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Features", icon = Icons.Default.ToggleOn)
            }

            item {
                SettingsSwitchRow(
                    title = "Image Processing",
                    subtitle = "Enable direct image input processing",
                    checked = enableImageProcessing,
                    onCheckedChange = { enableImageProcessing = it }
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Audio Processing",
                    subtitle = "Enable direct audio input processing",
                    checked = enableAudioProcessing,
                    onCheckedChange = { enableAudioProcessing = it }
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Video Processing",
                    subtitle = "Enable direct video input processing",
                    checked = enableVideoProcessing,
                    onCheckedChange = { enableVideoProcessing = it }
                )
            }

            // Provider-specific features
            if (selectedApiProvider == ApiProviderType.GOOGLE || selectedApiProvider == ApiProviderType.GEMINI_GENERIC) {
                item {
                    SettingsSwitchRow(
                        title = "Google Search Grounding",
                        subtitle = "Enable Google Search integration (Gemini only)",
                        checked = enableGoogleSearch,
                        onCheckedChange = { enableGoogleSearch = it }
                    )
                }
            }

            if (selectedApiProvider == ApiProviderType.ANTHROPIC || selectedApiProvider == ApiProviderType.ANTHROPIC_GENERIC) {
                item {
                    SettingsSwitchRow(
                        title = "Claude 1h Prompt Cache",
                        subtitle = "Enable 1-hour prompt cache TTL (Claude only)",
                        checked = enableClaudeCache,
                        onCheckedChange = { enableClaudeCache = it }
                    )
                }
            }

            item {
                SettingsSwitchRow(
                    title = "Tool Call",
                    subtitle = "Enable native tool/function calling",
                    checked = enableToolCall,
                    onCheckedChange = { enableToolCall = it }
                )
            }

            // ── Model Parameters Section ────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Model Parameters", icon = Icons.Default.Tune)
            }

            item {
                ModelParametersPanel(
                    maxTokens = maxTokens,
                    maxTokensEnabled = maxTokensEnabled,
                    onMaxTokensChange = { maxTokens = it },
                    onMaxTokensToggle = { maxTokensEnabled = it },
                    temperature = temperature,
                    temperatureEnabled = temperatureEnabled,
                    onTemperatureChange = { temperature = it },
                    onTemperatureToggle = { temperatureEnabled = it },
                    topP = topP,
                    topPEnabled = topPEnabled,
                    onTopPChange = { topP = it },
                    onTopPToggle = { topPEnabled = it },
                    topK = topK,
                    topKEnabled = topKEnabled,
                    onTopKChange = { topK = it },
                    onTopKToggle = { topKEnabled = it },
                    presencePenalty = presencePenalty,
                    presencePenaltyEnabled = presencePenaltyEnabled,
                    onPresencePenaltyChange = { presencePenalty = it },
                    onPresencePenaltyToggle = { presencePenaltyEnabled = it },
                    frequencyPenalty = frequencyPenalty,
                    frequencyPenaltyEnabled = frequencyPenaltyEnabled,
                    onFrequencyPenaltyChange = { frequencyPenalty = it },
                    onFrequencyPenaltyToggle = { frequencyPenaltyEnabled = it },
                    repetitionPenalty = repetitionPenalty,
                    repetitionPenaltyEnabled = repetitionPenaltyEnabled,
                    onRepetitionPenaltyChange = { repetitionPenalty = it },
                    onRepetitionPenaltyToggle = { repetitionPenaltyEnabled = it }
                )
            }

            // ── Save Button ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { saveCurrentConfig() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Configuration", fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────

    if (showAddConfigDialog) {
        AlertDialog(
            onDismissRequest = { showAddConfigDialog = false; newConfigName = "" },
            title = { Text("New Config") },
            text = {
                Column {
                    Text("Create a new model configuration", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newConfigName,
                        onValueChange = { newConfigName = it },
                        label = { Text("Config name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newConfigName.isNotBlank()) {
                            val newId = configManager.createConfig(newConfigName)
                            configList = configManager.getAllConfigs()
                            selectedConfigId = newId
                            showAddConfigDialog = false
                            newConfigName = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddConfigDialog = false; newConfigName = "" }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showRenameConfigDialog) {
        AlertDialog(
            onDismissRequest = { showRenameConfigDialog = false },
            title = { Text("Rename Config") },
            text = {
                OutlinedTextField(
                    value = renameConfigName,
                    onValueChange = { renameConfigName = it },
                    label = { Text("Config name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameConfigName.isNotBlank()) {
                            configManager.renameConfig(selectedConfigId, renameConfigName)
                            configList = configManager.getAllConfigs()
                            showRenameConfigDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameConfigDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Provider selection dialog
    if (showProviderDialog) {
        ProviderSelectionDialog(
            currentProviderId = selectedProviderTypeId,
            onDismiss = { showProviderDialog = false },
            onSelect = { providerId ->
                val prevProvider = ApiProviderType.fromProviderTypeId(selectedProviderTypeId)
                val newProvider = ApiProviderType.fromProviderTypeId(providerId)
                selectedProviderTypeId = providerId

                // Auto-fill endpoint and model
                if (newProvider != null) {
                    val prevEndpoint = if (prevProvider != null) ApiProviderConfigs.getDefaultApiEndpoint(prevProvider) else ""
                    val currentIsDefault = apiEndpoint.isEmpty() || apiEndpoint == prevEndpoint
                    if (currentIsDefault) {
                        apiEndpoint = ApiProviderConfigs.getDefaultApiEndpoint(newProvider)
                    }
                    val defaultModel = ApiProviderConfigs.getDefaultModelName(newProvider)
                    if (defaultModel.isNotEmpty() && (modelName.isEmpty() || ApiProviderConfigs.isDefaultModelName(modelName))) {
                        modelName = defaultModel
                    }
                }
                showProviderDialog = false
            }
        )
    }

    // Endpoint preset dialog
    if (showEndpointPresetDialog) {
        val provider = selectedApiProvider
        if (provider != null) {
            val options = ApiProviderConfigs.getEndpointOptions(provider)
            if (options != null) {
                EndpointPresetDialog(
                    options = options,
                    currentEndpoint = apiEndpoint,
                    onDismiss = { showEndpointPresetDialog = false },
                    onSelect = { endpoint ->
                        apiEndpoint = endpoint
                        showEndpointPresetDialog = false
                    }
                )
            }
        }
    }

    // Model selection dialog
    if (showModelsDialog) {
        ModelsSelectionDialog(
            models = availableModels,
            selectedModels = modelName.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            onDismiss = { showModelsDialog = false },
            onConfirm = { selected ->
                modelName = selected.joinToString(",")
                showModelsDialog = false
            }
        )
    }

    // Snackbar
    if (showSaveSnackbar) {
        LaunchedEffect(snackbarMessage) {
            kotlinx.coroutines.delay(2000)
            showSaveSnackbar = false
        }
    }
}

// ─── In-memory Config Manager ────────────────────────────────────────

private class ModelConfigManager {
    private val configs = mutableMapOf<String, ModelConfigData>()
    private val uuidOrder = mutableListOf<String>()
    var activeConfigId: String = "default"

    init {
        val default = ModelConfigData(
            id = "default",
            name = "Default",
            apiProviderType = ApiProviderType.DEEPSEEK
        )
        configs["default"] = default
        uuidOrder.add("default")
    }

    fun getAllConfigs(): List<ModelConfigData> = uuidOrder.mapNotNull { configs[it] }

    fun getConfig(id: String): ModelConfigData? = configs[id]

    fun updateConfig(config: ModelConfigData): ModelConfigData {
        configs[config.id] = config
        activeConfigId = config.id
        return config
    }

    fun createConfig(name: String): String {
        val id = UUID.randomUUID().toString()
        configs[id] = ModelConfigData(id = id, name = name)
        uuidOrder.add(id)
        activeConfigId = id
        return id
    }

    fun renameConfig(id: String, newName: String) {
        configs[id]?.let { configs[id] = it.copy(name = newName) }
    }

    fun deleteConfig(id: String) {
        if (id == "default" || uuidOrder.size <= 1) return
        configs.remove(id)
        uuidOrder.remove(id)
        if (activeConfigId == id) {
            activeConfigId = uuidOrder.firstOrNull() ?: "default"
        }
    }
}

private data class ConnectionTestResult(val label: String, val success: Boolean, val error: String?)

// ─── Reusable UI Components ─────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigSelectorCard(
    configList: List<ModelConfigData>,
    selectedConfigId: String,
    isDropdownExpanded: Boolean,
    onToggleDropdown: () -> Unit,
    onSelectConfig: (String) -> Unit,
    onAddClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTestConnection: () -> Unit,
    isTestingConnection: Boolean,
    testResults: List<ConnectionTestResult>?
) {
    val selectedName = configList.find { it.id == selectedConfigId }?.name ?: "Default"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Model Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("New", fontSize = 12.sp)
                }
            }

            // Config dropdown selector
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onToggleDropdown() },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(selectedName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    AnimatedContent(targetState = isDropdownExpanded) { expanded ->
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select config",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedConfigId != "default") {
                    TextButton(onClick = onRenameClick, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rename", fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", fontSize = 14.sp)
                    }
                }
                TextButton(
                    onClick = onTestConnection,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isTestingConnection) "Testing..." else "Test Connection", fontSize = 14.sp)
                }
            }

            // Test results
            AnimatedVisibility(visible = testResults != null) {
                testResults?.let { results ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            results.forEachIndexed { i, r ->
                                val icon = if (r.success) Icons.Default.CheckCircle else Icons.Default.Warning
                                val color = if (r.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(r.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (r.success) Text("OK", style = MaterialTheme.typography.bodySmall, color = color)
                                }
                                if (!r.success && r.error != null) {
                                    Text(r.error, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(start = 26.dp))
                                }
                                if (i < results.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { onToggleDropdown() },
            modifier = Modifier.width(280.dp)
        ) {
            configList.forEach { config ->
                val isSelected = config.id == selectedConfigId
                DropdownMenuItem(
                    text = {
                        Text(
                            config.name,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = if (isSelected) {{ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null,
                    onClick = { onSelectConfig(config.id) }
                )
            }
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
internal fun SettingsTextField(
    title: String,
    subtitle: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val inputEnabled = enabled && !readOnly
    val focusManager = LocalFocusManager.current
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { if (inputEnabled) onValueChange(it) },
                        singleLine = singleLine,
                        enabled = inputEnabled,
                        keyboardOptions = keyboardOptions,
                        visualTransformation = visualTransformation,
                        interactionSource = resolvedInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (value.isEmpty()) {
                                        Text(placeholder, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
                trailingContent?.invoke()
            }
        }
    }
}

@Composable
private fun SettingsSelectorRow(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp).weight(0.5f, fill = false),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var showKey by rememberSaveable { mutableStateOf(false) }

    SettingsTextField(
        title = "API Key",
        subtitle = if (value.isEmpty()) "Enter your API key" else "API key configured",
        value = if (showKey || isFocused || value.isEmpty()) value else "•".repeat(value.length.coerceAtMost(32)),
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        visualTransformation = VisualTransformation.None,
        interactionSource = interactionSource,
        trailingContent = {
            IconButton(onClick = { showKey = !showKey }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle visibility",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

// ─── Model Parameters Panel ─────────────────────────────────────────

@Composable
private fun ModelParametersPanel(
    maxTokens: String, maxTokensEnabled: Boolean, onMaxTokensChange: (String) -> Unit, onMaxTokensToggle: (Boolean) -> Unit,
    temperature: String, temperatureEnabled: Boolean, onTemperatureChange: (String) -> Unit, onTemperatureToggle: (Boolean) -> Unit,
    topP: String, topPEnabled: Boolean, onTopPChange: (String) -> Unit, onTopPToggle: (Boolean) -> Unit,
    topK: String, topKEnabled: Boolean, onTopKChange: (String) -> Unit, onTopKToggle: (Boolean) -> Unit,
    presencePenalty: String, presencePenaltyEnabled: Boolean, onPresencePenaltyChange: (String) -> Unit, onPresencePenaltyToggle: (Boolean) -> Unit,
    frequencyPenalty: String, frequencyPenaltyEnabled: Boolean, onFrequencyPenaltyChange: (String) -> Unit, onFrequencyPenaltyToggle: (Boolean) -> Unit,
    repetitionPenalty: String, repetitionPenaltyEnabled: Boolean, onRepetitionPenaltyChange: (String) -> Unit, onRepetitionPenaltyToggle: (Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var tabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("Generation", "Creativity", "Repetition")

                    ScrollableTabRow(
                        selectedTabIndex = tabIndex,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { i, label ->
                            Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label, fontSize = 13.sp) })
                        }
                    }

                    when (tabIndex) {
                        0 -> Column {
                            ParameterSlider("Max Tokens", maxTokens, onMaxTokensChange, maxTokensEnabled, onMaxTokensToggle, "4096", KeyboardType.Number)
                        }
                        1 -> Column {
                            ParameterSlider("Temperature", temperature, onTemperatureChange, temperatureEnabled, onTemperatureToggle, "1.0", KeyboardType.Decimal)
                            ParameterSlider("Top-p", topP, onTopPChange, topPEnabled, onTopPToggle, "1.0", KeyboardType.Decimal)
                            ParameterSlider("Top-k", topK, onTopKChange, topKEnabled, onTopKToggle, "0", KeyboardType.Number)
                        }
                        2 -> Column {
                            ParameterSlider("Presence Penalty", presencePenalty, onPresencePenaltyChange, presencePenaltyEnabled, onPresencePenaltyToggle, "0.0", KeyboardType.Decimal)
                            ParameterSlider("Frequency Penalty", frequencyPenalty, onFrequencyPenaltyChange, frequencyPenaltyEnabled, onFrequencyPenaltyToggle, "0.0", KeyboardType.Decimal)
                            ParameterSlider("Repetition Penalty", repetitionPenalty, onRepetitionPenaltyChange, repetitionPenaltyEnabled, onRepetitionPenaltyToggle, "1.0", KeyboardType.Decimal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(6.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    enabled = enabled
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

// ─── Dialogs ────────────────────────────────────────────────────────

@Composable
private fun ProviderSelectionDialog(
    currentProviderId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val providers = remember { ApiProviderConfigs.getAllProviderOptions() }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isEmpty()) providers
        else providers.filter { (id, name) ->
            id.contains(searchQuery, ignoreCase = true) || name.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search providers...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filtered) { _, (id, displayName) ->
                        val isSelected = id == currentProviderId
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .clickable { onSelect(id) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(
                                        getProviderColor(id), CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        displayName.first().toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(displayName, style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                if (isSelected) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun EndpointPresetDialog(
    options: List<ProviderEndpointOption>,
    currentEndpoint: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                options.forEach { option ->
                    val isSelected = option.endpoint == currentEndpoint
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option.endpoint) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent)
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Text(option.endpoint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (option.label.isNotBlank()) {
                            Text(option.label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ModelsSelectionDialog(
    models: List<ModelOption>,
    selectedModels: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateOf(selectedModels) }

    val filtered = remember(searchQuery, models) {
        if (searchQuery.isEmpty()) models
        else models.filter { it.id.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Available Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search models...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filtered) { _, model ->
                        val isChecked = selected.value.contains(model.id)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val newSet = selected.value.toMutableSet()
                                if (isChecked) newSet.remove(model.id) else newSet.add(model.id)
                                selected.value = newSet
                            }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { c ->
                                val newSet = selected.value.toMutableSet()
                                if (c) newSet.add(model.id) else newSet.remove(model.id)
                                selected.value = newSet
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(model.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.height(36.dp)) {
                        Text("Close", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { onConfirm(selected.value.toList()) },
                        modifier = Modifier.height(36.dp),
                        enabled = selected.value.isNotEmpty()
                    ) { Text("Confirm (${selected.value.size})", fontSize = 14.sp) }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────

@Composable
private fun getProviderColor(providerId: String): Color {
    val baseColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    return baseColors[abs(providerId.hashCode()) % baseColors.size]
}

private data class ModelOption(val id: String, val name: String)

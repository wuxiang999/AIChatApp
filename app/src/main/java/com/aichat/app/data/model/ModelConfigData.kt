package com.aichat.app.data.model

/** Default values for model config */
object ModelConfigDefaults {
    const val DEFAULT_CONTEXT_LENGTH = 64.0f
    const val DEFAULT_MAX_CONTEXT_LENGTH = 200.0f
    const val DEFAULT_ENABLE_MAX_CONTEXT_MODE = false
}

/** Complete model configuration including API settings and parameters */
data class ModelConfigData(
    val id: String,
    val name: String,

    // API settings
    val apiKey: String = "",
    val apiEndpoint: String = "",
    val modelName: String = "",
    val apiProviderType: ApiProviderType = ApiProviderType.DEEPSEEK,

    // Parameter enabled states
    val maxTokensEnabled: Boolean = false,
    val temperatureEnabled: Boolean = false,
    val topPEnabled: Boolean = false,
    val topKEnabled: Boolean = false,
    val presencePenaltyEnabled: Boolean = false,
    val frequencyPenaltyEnabled: Boolean = false,
    val repetitionPenaltyEnabled: Boolean = false,

    // Parameter values
    val maxTokens: Int = 4096,
    val temperature: Float = 1.0f,
    val topP: Float = 1.0f,
    val topK: Int = 0,
    val presencePenalty: Float = 0.0f,
    val frequencyPenalty: Float = 0.0f,
    val repetitionPenalty: Float = 1.0f,

    // Custom parameters JSON
    val customParameters: String = "[]",
    val hasCustomParameters: Boolean = false,

    // Custom headers JSON
    val customHeaders: String = "{}",

    // Context / summary config
    val contextLength: Float = ModelConfigDefaults.DEFAULT_CONTEXT_LENGTH,
    val maxContextLength: Float = ModelConfigDefaults.DEFAULT_MAX_CONTEXT_LENGTH,
    val enableMaxContextMode: Boolean = ModelConfigDefaults.DEFAULT_ENABLE_MAX_CONTEXT_MODE,

    // Image/audio/video processing
    val enableDirectImageProcessing: Boolean = false,
    val enableDirectAudioProcessing: Boolean = false,
    val enableDirectVideoProcessing: Boolean = false,

    // Provider-specific features
    val enableGoogleSearch: Boolean = false,
    val enableClaude1hPromptCache: Boolean = false,
    val enableToolCall: Boolean = false,

    // Rate limiting
    val requestLimitPerMinute: Int = 0,
    val maxConcurrentRequests: Int = 0
)

/** Simplified model config summary for list display */
data class ModelConfigSummary(
    val id: String,
    val name: String,
    val modelName: String = "",
    val apiEndpoint: String = "",
    val apiProviderType: ApiProviderType = ApiProviderType.DEEPSEEK,
    val modelIndex: Int = 0
)

/** Get model name by index from comma-separated model list */
fun getModelByIndex(modelName: String, index: Int): String {
    if (modelName.isEmpty()) return ""
    val models = modelName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (index >= 0 && index < models.size) models[index] else models.getOrNull(0) ?: ""
}

/** Get model list from comma-separated string */
fun getModelList(modelName: String): List<String> {
    if (modelName.isEmpty()) return emptyList()
    return modelName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

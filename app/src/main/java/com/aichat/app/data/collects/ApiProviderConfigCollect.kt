package com.aichat.app.data.collects

import com.aichat.app.data.model.ApiProviderType

data class ProviderEndpointOption(
    val endpoint: String,
    val label: String
)

data class ProviderApiConfig(
    val providerType: ApiProviderType,
    val defaultModelName: String = "",
    val defaultApiEndpoint: String = "",
    val displayName: String = "",
    val endpointOptions: List<ProviderEndpointOption> = emptyList(),
    val requiresApiKey: Boolean = true
)

object ApiProviderConfigs {
    private val configs: Map<ApiProviderType, ProviderApiConfig> = listOf(
        ProviderApiConfig(
            providerType = ApiProviderType.OPENAI,
            displayName = "OpenAI",
            defaultModelName = "gpt-4o",
            defaultApiEndpoint = "https://api.openai.com/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OPENAI_RESPONSES,
            displayName = "OpenAI Responses",
            defaultModelName = "gpt-4o",
            defaultApiEndpoint = "https://api.openai.com/v1/responses"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OPENAI_RESPONSES_GENERIC,
            displayName = "OpenAI Resp. (Generic)",
            defaultModelName = "",
            defaultApiEndpoint = ""
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OPENAI_GENERIC,
            displayName = "OpenAI Compatible",
            defaultModelName = "",
            defaultApiEndpoint = ""
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.ANTHROPIC,
            displayName = "Anthropic",
            defaultModelName = "claude-3-opus-20240229",
            defaultApiEndpoint = "https://api.anthropic.com/v1/messages"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.ANTHROPIC_GENERIC,
            displayName = "Anthropic Compatible",
            defaultModelName = "",
            defaultApiEndpoint = ""
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.GOOGLE,
            displayName = "Google Gemini",
            defaultModelName = "gemini-2.0-flash",
            defaultApiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.GEMINI_GENERIC,
            displayName = "Gemini Compatible",
            defaultModelName = "gemini-2.0-flash",
            defaultApiEndpoint = ""
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.DEEPSEEK,
            displayName = "DeepSeek",
            defaultModelName = "deepseek-chat",
            defaultApiEndpoint = "https://api.deepseek.com/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.BAIDU,
            displayName = "Baidu (Wenxin)",
            defaultModelName = "ernie-bot-4",
            defaultApiEndpoint = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.ALIYUN,
            displayName = "Alibaba (Tongyi)",
            defaultModelName = "qwen-max",
            defaultApiEndpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.ZHIPU,
            displayName = "Zhipu AI (GLM)",
            defaultModelName = "glm-4",
            defaultApiEndpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            endpointOptions = listOf(
                ProviderEndpointOption("https://open.bigmodel.cn/api/paas/v4/chat/completions", "CN standard"),
                ProviderEndpointOption("https://open.bigmodel.cn/api/coding/paas/v4/chat/completions", "CN coding"),
                ProviderEndpointOption("https://api.z.ai/api/paas/v4/chat/completions", "Intl standard"),
                ProviderEndpointOption("https://api.z.ai/api/coding/paas/v4/chat/completions", "Intl coding")
            )
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.MOONSHOT,
            displayName = "Moonshot (Kimi)",
            defaultModelName = "moonshot-v1-128k",
            defaultApiEndpoint = "https://api.moonshot.cn/v1/chat/completions",
            endpointOptions = listOf(
                ProviderEndpointOption("https://api.moonshot.cn/v1/chat/completions", "CN (moonshot.cn)"),
                ProviderEndpointOption("https://api.moonshot.ai/v1/chat/completions", "Intl (moonshot.ai)"),
                ProviderEndpointOption("https://api.kimi.com/coding/v1/chat/completions", "Kimi Code")
            )
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.SILICONFLOW,
            displayName = "SiliconFlow",
            defaultModelName = "Yi-1.5-34B",
            defaultApiEndpoint = "https://api.siliconflow.cn/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OPENROUTER,
            displayName = "OpenRouter",
            defaultModelName = "google/gemini-pro",
            defaultApiEndpoint = "https://openrouter.ai/api/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.MISTRAL,
            displayName = "Mistral AI",
            defaultModelName = "codestral-latest",
            defaultApiEndpoint = "https://codestral.mistral.ai/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.DOUBAO,
            displayName = "Doubao (Volcengine)",
            defaultModelName = "Doubao-pro-4k",
            defaultApiEndpoint = "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
            endpointOptions = listOf(
                ProviderEndpointOption("https://ark.cn-beijing.volces.com/api/v3/chat/completions", "CN standard"),
                ProviderEndpointOption("https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions", "CN coding")
            )
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.NVIDIA,
            displayName = "NVIDIA NIM",
            defaultModelName = "nvidia/nemotron-3-nano",
            defaultApiEndpoint = "https://integrate.api.nvidia.com/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.BAICHUAN,
            displayName = "Baichuan",
            defaultModelName = "baichuan4",
            defaultApiEndpoint = "https://api.baichuan-ai.com/v1/chat/completions"
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.LMSTUDIO,
            displayName = "LM Studio",
            defaultModelName = "local-model",
            defaultApiEndpoint = "http://localhost:1234/v1/chat/completions",
            requiresApiKey = false
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OLLAMA,
            displayName = "Ollama",
            defaultModelName = "llama3",
            defaultApiEndpoint = "http://localhost:11434/v1/chat/completions",
            requiresApiKey = false
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OPENAI_LOCAL,
            displayName = "Local OpenAI Compatible",
            defaultModelName = "",
            defaultApiEndpoint = "http://localhost:8000/v1/chat/completions",
            requiresApiKey = false
        ),
        ProviderApiConfig(
            providerType = ApiProviderType.OTHER,
            displayName = "Custom Provider",
            defaultModelName = "",
            defaultApiEndpoint = ""
        )
    ).associateBy(ProviderApiConfig::providerType)

    fun get(providerType: ApiProviderType): ProviderApiConfig =
        configs[providerType] ?: ProviderApiConfig(providerType = providerType)

    fun getDefaultModelName(providerType: ApiProviderType): String = get(providerType).defaultModelName

    fun getDefaultApiEndpoint(providerType: ApiProviderType): String = get(providerType).defaultApiEndpoint

    fun getEndpointOptions(providerType: ApiProviderType): List<ProviderEndpointOption>? =
        get(providerType).endpointOptions.takeIf { it.isNotEmpty() }

    fun requiresApiKey(providerType: ApiProviderType): Boolean = get(providerType).requiresApiKey

    fun getDisplayName(providerType: ApiProviderType): String = get(providerType).displayName

    fun isDefaultModelName(modelName: String): Boolean =
        configs.values.any { it.defaultModelName == modelName }

    fun isDefaultApiEndpoint(endpoint: String): Boolean =
        configs.values.any { it.defaultApiEndpoint == endpoint }

    /** Get all provider display options for the provider selection dialog */
    fun getAllProviderOptions(): List<Pair<String, String>> =
        ApiProviderType.values().map { it.name to get(it).displayName }
}

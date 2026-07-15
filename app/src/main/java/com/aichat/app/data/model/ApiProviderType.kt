package com.aichat.app.data.model

/** API provider type enum matching Operit's provider system */
enum class ApiProviderType {
    OPENAI,
    OPENAI_RESPONSES,
    OPENAI_RESPONSES_GENERIC,
    OPENAI_GENERIC,
    ANTHROPIC,
    ANTHROPIC_GENERIC,
    GOOGLE,
    GEMINI_GENERIC,
    BAIDU,
    ALIYUN,
    XUNFEI,
    ZHIPU,
    BAICHUAN,
    MOONSHOT,
    MIMO,
    DEEPSEEK,
    MISTRAL,
    SILICONFLOW,
    IFLOW,
    OPENROUTER,
    FOUR_ROUTER,
    NOUS_PORTAL,
    INFINIAI,
    ALIPAY_BAILING,
    DOUBAO,
    NVIDIA,
    LMSTUDIO,
    OLLAMA,
    OPENAI_LOCAL,
    MNN,
    LLAMA_CPP,
    PPINFRA,
    NOVITA,
    OTHER;

    companion object {
        fun fromProviderTypeId(providerTypeId: String): ApiProviderType? {
            val normalized = providerTypeId.trim()
            if (normalized.isEmpty()) return null
            return values().firstOrNull {
                it.name.equals(normalized, ignoreCase = true)
            }
        }
    }
}

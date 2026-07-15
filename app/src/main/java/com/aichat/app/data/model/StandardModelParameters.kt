package com.aichat.app.data.model

import com.aichat.app.data.model.ParameterCategory.*
import com.aichat.app.data.model.ParameterValueType.*

/** Static definition of a model parameter */
data class ParameterDefinition<T : Any>(
    val id: String,
    val name: String,
    val apiName: String,
    val description: String,
    val defaultValue: T,
    val valueType: ParameterValueType,
    val category: ParameterCategory,
    val minValue: T? = null,
    val maxValue: T? = null
)

/** Central repository of standard model parameter definitions */
object StandardModelParameters {
    const val DEFAULT_MAX_TOKENS = 4096
    const val DEFAULT_TEMPERATURE = 1.0f
    const val DEFAULT_TOP_P = 1.0f
    const val DEFAULT_TOP_K = 0
    const val DEFAULT_PRESENCE_PENALTY = 0.0f
    const val DEFAULT_FREQUENCY_PENALTY = 0.0f
    const val DEFAULT_REPETITION_PENALTY = 1.0f

    val DEFINITIONS: List<ParameterDefinition<*>> = listOf(
        ParameterDefinition(
            id = "max_tokens",
            name = "Max tokens",
            apiName = "max_tokens",
            description = "Maximum number of tokens to generate in one response",
            defaultValue = DEFAULT_MAX_TOKENS,
            valueType = INT,
            category = GENERATION,
            minValue = 1
        ),
        ParameterDefinition(
            id = "temperature",
            name = "Temperature",
            apiName = "temperature",
            description = "Controls randomness: lower is deterministic, higher is random",
            defaultValue = DEFAULT_TEMPERATURE,
            valueType = FLOAT,
            category = CREATIVITY,
            minValue = 0.0f,
            maxValue = 2.0f
        ),
        ParameterDefinition(
            id = "top_p",
            name = "Top-p sampling",
            apiName = "top_p",
            description = "Consider only tokens within cumulative probability top-p",
            defaultValue = DEFAULT_TOP_P,
            valueType = FLOAT,
            category = CREATIVITY,
            minValue = 0.0f,
            maxValue = 1.0f
        ),
        ParameterDefinition(
            id = "top_k",
            name = "Top-k sampling",
            apiName = "top_k",
            description = "Consider only the top-k tokens. 0 disables",
            defaultValue = DEFAULT_TOP_K,
            valueType = INT,
            category = CREATIVITY,
            minValue = 0,
            maxValue = 100
        ),
        ParameterDefinition(
            id = "presence_penalty",
            name = "Presence penalty",
            apiName = "presence_penalty",
            description = "Encourages new topics: higher values reduce repetition",
            defaultValue = DEFAULT_PRESENCE_PENALTY,
            valueType = FLOAT,
            category = REPETITION,
            minValue = -2.0f,
            maxValue = 2.0f
        ),
        ParameterDefinition(
            id = "frequency_penalty",
            name = "Frequency penalty",
            apiName = "frequency_penalty",
            description = "Reduces repetition: higher values penalize frequent tokens",
            defaultValue = DEFAULT_FREQUENCY_PENALTY,
            valueType = FLOAT,
            category = REPETITION,
            minValue = -2.0f,
            maxValue = 2.0f
        ),
        ParameterDefinition(
            id = "repetition_penalty",
            name = "Repetition penalty",
            apiName = "repetition_penalty",
            description = "1.0 means no penalty; >1 discourages repetition",
            defaultValue = DEFAULT_REPETITION_PENALTY,
            valueType = FLOAT,
            category = REPETITION,
            minValue = 0.0f,
            maxValue = 2.0f
        )
    )
}

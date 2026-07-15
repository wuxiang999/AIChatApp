package com.aichat.app.agent

/**
 * Schema for a single parameter in a tool definition.
 * Maps to JSON Schema for LLM function calling.
 */
data class ParameterSchema(
    val name: String,
    val type: ParameterType,
    val description: String = "",
    val required: Boolean = true,
    val enumValues: List<String>? = null,
    val items: ParameterSchema? = null  // for array types
)

enum class ParameterType(val jsonType: String) {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    OBJECT("object")
}

/**
 * Type-safe definition of a tool that the AI can call.
 * Inspired by OpenCode's Tool.make() + Hermes tools/registry.py
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ParameterSchema> = emptyList(),
    val action: String = name,           // used for permission matching
    val toolset: String = "default",     // grouping for filtering
    val emoji: String = "",
    val isAvailable: Boolean = true       // check_fn result cached
) {
    /**
     * Convert to JSON Schema format for LLM function calling API.
     */
    fun toJsonSchema(): Map<String, Any?> {
        val props = mutableMapOf<String, Any?>()
        val required = mutableListOf<String>()

        for (param in parameters) {
            props[param.name] = param.toJsonProperty()
            if (param.required) required.add(param.name)
        }

        return mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to name,
                "description" to description,
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to props,
                    "required" to required
                )
            )
        )
    }
}

private fun ParameterSchema.toJsonProperty(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>("type" to type.jsonType)
    if (description.isNotEmpty()) result["description"] = description
    if (enumValues != null) result["enum"] = enumValues
    if (type == ParameterType.ARRAY && items != null) {
        result["items"] = items.toJsonProperty()
    }
    return result
}

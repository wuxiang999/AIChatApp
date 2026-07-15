package com.aichat.app.data.model

/** Model parameter with generic value type support */
data class ModelParameter<T>(
    val id: String,
    val name: String,
    val apiName: String,
    val description: String = "",
    val defaultValue: T,
    val currentValue: T,
    val isEnabled: Boolean,
    val valueType: ParameterValueType,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val category: ParameterCategory = ParameterCategory.OTHER,
    val isCustom: Boolean = false
)

/** Parameter value type enum */
enum class ParameterValueType {
    INT, FLOAT, STRING, BOOLEAN, OBJECT
}

/** Parameter category enum */
enum class ParameterCategory {
    GENERATION, CREATIVITY, REPETITION, OTHER
}

/** Custom parameter data for JSON serialization */
data class CustomParameterData(
    val id: String,
    val name: String,
    val apiName: String,
    val description: String = "",
    val defaultValue: String,
    val currentValue: String,
    val isEnabled: Boolean,
    val valueType: String,
    val minValue: String? = null,
    val maxValue: String? = null,
    val category: String = "OTHER"
)

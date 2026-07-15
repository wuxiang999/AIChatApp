package com.aichat.app.agent

data class ToolParameterSchema(
    val name: String,
    val type: String = "string",
    val description: String,
    val required: Boolean = true,
    val default: String? = null
)

data class ToolPrompt(
    val name: String,
    val description: String,
    val parameters: String = "",
    val parametersStructured: List<ToolParameterSchema>? = null,
    val details: String = "",
    val notes: String = ""
) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("- $name: $description")

        val paramsString = if (parametersStructured != null && parametersStructured.isNotEmpty()) {
            parametersStructured.joinToString(", ") { param ->
                val parts = mutableListOf<String>()
                parts.add(param.description)
                if (param.default != null && !param.description.contains("default")) {
                    parts.add("default ${param.default}")
                }
                val fullDesc = parts.joinToString(", ")
                "${param.name} ($fullDesc)"
            }
        } else {
            parameters
        }

        if (paramsString.isNotEmpty()) {
            builder.append(" Parameters: $paramsString")
        }

        if (details.isNotEmpty()) {
            builder.append("\n$details")
        }

        if (notes.isNotEmpty()) {
            builder.append("\n$notes")
        }

        return builder.toString()
    }
}

data class SystemToolPromptCategory(
    val categoryName: String,
    val categoryHeader: String = "",
    val tools: List<ToolPrompt>,
    val categoryFooter: String = ""
) {
    override fun toString(): String {
        val builder = StringBuilder()

        if (categoryName.isNotEmpty()) {
            builder.append(categoryName)
            builder.append(":")
        }

        if (categoryHeader.isNotEmpty()) {
            builder.append("\n")
            builder.append(categoryHeader)
        }

        if (tools.isNotEmpty()) {
            builder.append("\n")
            tools.forEachIndexed { index, tool ->
                builder.append(tool.toString())
                if (index < tools.size - 1) {
                    builder.append("\n")
                }
            }
        }

        if (categoryFooter.isNotEmpty()) {
            builder.append("\n")
            builder.append(categoryFooter)
        }

        return builder.toString()
    }
}

data class PackageToolPromptCategory(
    val packageName: String,
    val packageDescription: String,
    val tools: List<ToolPrompt>
) {
    override fun toString(): String {
        val builder = StringBuilder()

        builder.append("Package: $packageName")
        builder.append("\n")
        builder.append("Description: $packageDescription")

        if (tools.isNotEmpty()) {
            builder.append("\n")
            builder.append("Tools:")
            builder.append("\n")
            tools.forEachIndexed { index, tool ->
                builder.append(tool.toString())
                if (index < tools.size - 1) {
                    builder.append("\n")
                }
            }
        }

        return builder.toString()
    }
}

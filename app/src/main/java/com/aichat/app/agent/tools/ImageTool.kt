package com.aichat.app.agent.tools

import com.aichat.app.agent.ITool
import com.aichat.app.agent.ParameterSchema
import com.aichat.app.agent.ParameterType
import com.aichat.app.agent.ToolContext
import com.aichat.app.agent.ToolDefinition
import com.aichat.app.agent.ToolResult
import com.aichat.app.data.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageTool @Inject constructor(
    private val chatRepository: ChatRepository
) : ITool {
    override val definition = ToolDefinition(
        name = "image_generate",
        description = "根据文字描述生成图片，支持文生图和图生图",
        parameters = listOf(
            ParameterSchema("prompt", ParameterType.STRING, "图片描述，详细的prompt效果更好", required = true),
            ParameterSchema("size", ParameterType.STRING, "图片尺寸: 1024x1024 / 1792x1024 / 1024x1792", required = false),
            ParameterSchema("model", ParameterType.STRING, "模型: dall-e-3 / dall-e-2", required = false)
        ),
        action = "image_generate",
        toolset = "core",
        emoji = ""
    )

    override suspend fun execute(context: ToolContext, args: Map<String, Any?>): ToolResult {
        val prompt = args["prompt"]?.toString() ?: return ToolResult.Error("缺少 prompt 参数")
        val size = args["size"]?.toString() ?: "1024x1024"
        val model = args["model"]?.toString() ?: "dall-e-3"
        // 调用 chatRepository.generateImage
        val result = chatRepository.generateImage(prompt = prompt, size = size, model = model)
        return result.fold(
            onSuccess = { urls -> ToolResult.Success("生成成功！\n${urls.joinToString("\n")}") },
            onFailure = { ToolResult.Error("图片生成失败: ${it.message}") }
        )
    }
}

package com.aichat.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

interface OpenAIApiService {

    @GET("models")
    suspend fun getModels(
        @retrofit2.http.Header("Authorization") auth: String
    ): ModelsResponse

    @POST("chat/completions")
    suspend fun chatCompletion(
        @retrofit2.http.Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatCompletionResponse

    @Streaming
    @POST("chat/completions")
    fun chatCompletionStream(
        @retrofit2.http.Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): Call<ResponseBody>

    @Multipart
    @POST("images/generations")
    suspend fun generateImage(
        @retrofit2.http.Header("Authorization") auth: String,
        @Part("prompt") prompt: RequestBody,
        @Part("n") n: RequestBody,
        @Part("size") size: RequestBody,
        @Part("model") model: RequestBody,
        @Part("quality") quality: RequestBody? = null
    ): ImageGenerationResponse

    @Multipart
    @POST("images/edits")
    suspend fun editImage(
        @retrofit2.http.Header("Authorization") auth: String,
        @Part image: MultipartBody.Part,
        @Part("prompt") prompt: RequestBody,
        @Part("n") n: RequestBody,
        @Part("size") size: RequestBody,
        @Part("model") model: RequestBody
    ): ImageGenerationResponse
}

data class ModelsResponse(
    val data: List<ModelData>
)

data class ModelData(
    val id: String,
    val object_: String? = null,
    val owned_by: String? = null
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 4096,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,
    val content: Any
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrlData? = null
)

data class ImageUrlData(
    val url: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val error: ApiError? = null
)

data class Choice(
    val index: Int,
    val message: ResponseMessage? = null,
    val delta: DeltaMessage? = null,
    val finish_reason: String? = null
)

data class ResponseMessage(
    val role: String,
    val content: String,
    val reasoning_content: String? = null
)

data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null
)

data class ApiError(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

data class ImageGenerationResponse(
    val created: Long? = null,
    val data: List<ImageData>? = null,
    val error: ApiError? = null
)

data class ImageData(
    val url: String? = null,
    val b64_json: String? = null
)

data class StreamResponse(
    val id: String? = null,
    val choices: List<StreamChoice>?,
    val error: ApiError? = null
)

data class StreamChoice(
    val delta: StreamDelta?,
    val finish_reason: String? = null,
    val index: Int = 0
)

data class StreamDelta(
    val content: String? = null,
    val role: String? = null,
    val reasoning_content: String? = null
)

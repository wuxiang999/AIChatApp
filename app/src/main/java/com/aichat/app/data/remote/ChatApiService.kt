package com.aichat.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ChatApiService {

    @GET("index.php")
    suspend fun getEndpoints(@Query("action") action: String = "get_endpoints"): EndpointsResponse

    @GET("index.php")
    suspend fun getAnnouncement(@Query("action") action: String = "get_announcement"): AnnouncementResponse

    @GET("index.php")
    suspend fun getModels(
        @Query("action") action: String = "get_models",
        @Query("endpoint") endpoint: String? = null
    ): ModelsResponse

    @FormUrlEncoded
    @POST("index.php?action=get_history")
    suspend fun getHistory(
        @Field("session_id") sessionId: String
    ): HistoryResponse

    @FormUrlEncoded
    @POST("index.php?action=clear_conversation")
    suspend fun clearConversation(
        @Field("session_id") sessionId: String
    ): BaseResponse

    @FormUrlEncoded
    @POST("index.php?action=save_message")
    suspend fun saveMessage(
        @Field("session_id") sessionId: String,
        @Field("reply") reply: String,
        @Field("type") type: String = "assistant"
    ): BaseResponse

    @FormUrlEncoded
    @POST("index.php?action=chat")
    suspend fun chat(
        @Field("session_id") sessionId: String,
        @Field("message") message: String,
        @Field("model") model: String = "gpt-3.5-turbo",
        @Field("file_data") fileData: String? = null,
        @Field("api_endpoint") apiEndpoint: String? = null
    ): ChatResponse

    @Streaming
    @FormUrlEncoded
    @POST("index.php?action=chat_stream")
    fun chatStream(
        @Field("session_id") sessionId: String,
        @Field("message") message: String,
        @Field("model") model: String = "gpt-3.5-turbo",
        @Field("file_data") fileData: String? = null,
        @Field("api_endpoint") apiEndpoint: String? = null
    ): Call<ResponseBody>

    @Multipart
    @POST("index.php?action=upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): UploadResponse

    @Multipart
    @POST("index.php?action=image_generate")
    suspend fun generateImage(
        @Part("prompt") prompt: RequestBody,
        @Part("n") n: RequestBody,
        @Part("size") size: RequestBody,
        @Part("model") model: RequestBody,
        @Part("api_endpoint") apiEndpoint: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): ImageGenerationResponse
}

data class EndpointsResponse(
    val endpoints: Map<String, String>
)

data class AnnouncementResponse(
    val content: String,
    val enabled: Boolean,
    val updated_at: String
)

data class ModelsResponse(
    val models: List<String>
)

data class HistoryResponse(
    val history: List<HistoryMessage>
)

data class HistoryMessage(
    val role: String,
    val content: Any
)

data class BaseResponse(
    val success: Boolean,
    val error: String? = null,
    val message: String? = null
)

data class ChatResponse(
    val success: Boolean,
    val reply: String? = null,
    val error: String? = null
)

data class UploadResponse(
    val success: Boolean,
    val original_name: String? = null,
    val content: String? = null,
    val type: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class ImageGenerationResponse(
    val success: Boolean,
    val image_urls: List<String>? = null,
    val reply: String? = null,
    val error: String? = null
)

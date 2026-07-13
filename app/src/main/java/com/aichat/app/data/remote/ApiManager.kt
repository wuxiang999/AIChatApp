package com.aichat.app.data.remote

import android.util.Log
import com.aichat.app.data.local.ApiEndpointDao
import com.aichat.app.data.model.ApiEndpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiManager @Inject constructor(
    private val apiEndpointDao: ApiEndpointDao
) {
    @Volatile
    private var currentEndpoint: ApiEndpoint? = null
    @Volatile
    private var retrofit: Retrofit? = null
    @Volatile
    private var apiService: OpenAIApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC 级别避免 BODY 对流式响应（SSE）的缓冲干扰
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun initialize() {
        val endpoints = apiEndpointDao.getAllEndpoints().first()
        if (endpoints.isEmpty()) {
            val defaultEndpoint = ApiEndpoint(
                name = "wsocket",
                url = "https://ai.wsocket.xyz/v1",
                apiKey = "sk-7Nb8FwAmO5zvmEzwkHlBXWX5RaDycdkPAmeKZqT2Ql5cDEQQ",
                isSelected = true
            )
            val generatedId = apiEndpointDao.insertEndpoint(defaultEndpoint)
            // 使用数据库返回的真实 id，避免内存对象 id=0 与数据库 id 不一致
            val inserted = defaultEndpoint.copy(id = generatedId)
            currentEndpoint = inserted
            updateRetrofit(inserted.url)
        } else {
            val selected = endpoints.find { it.isSelected } ?: endpoints.first()
            currentEndpoint = selected
            updateRetrofit(selected.url)
        }
    }

    /**
     * 更新 Retrofit 实例。对非法 URL 做保护，避免 Retrofit.Builder().baseUrl() 抛出
     * IllegalArgumentException 导致应用闪退。URL 非法时保持旧 apiService 不变并记录错误。
     * 返回 true 表示更新成功，false 表示 URL 非法未更新。
     */
    private fun updateRetrofit(baseUrl: String): Boolean {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        // 先校验 URL 合法性，避免 Retrofit.Builder().baseUrl() 抛 IllegalArgumentException
        val parsed = url.toHttpUrlOrNull()
            ?: run {
                Log.e("ApiManager", "Invalid base URL, keep previous service: $url")
                return false
            }
        if (parsed.scheme != "http" && parsed.scheme != "https") {
            Log.e("ApiManager", "Base URL scheme must be http/https: $url")
            return false
        }
        return try {
            val newRetrofit = Retrofit.Builder()
                .baseUrl(parsed)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit = newRetrofit
            apiService = newRetrofit.create(OpenAIApiService::class.java)
            Log.d("ApiManager", "Base URL updated to: $url")
            true
        } catch (e: Exception) {
            Log.e("ApiManager", "updateRetrofit failed for url: $url", e)
            false
        }
    }

    fun getApiService(): OpenAIApiService {
        if (apiService == null) {
            // 尝试使用 currentEndpoint 的 url 创建临时 service，而非直接抛异常崩溃
            currentEndpoint?.let { ep ->
                updateRetrofit(ep.url)
            }
        }
        return apiService
            ?: throw IllegalStateException("ApiManager not initialized: no endpoint configured")
    }

    fun getAuthHeader(): String {
        return "Bearer ${currentEndpoint?.apiKey ?: ""}"
    }

    fun getCurrentEndpoint(): ApiEndpoint? = currentEndpoint

    fun getCurrentBaseUrl(): String = currentEndpoint?.url ?: ""

    fun getAllEndpoints(): Flow<List<ApiEndpoint>> = apiEndpointDao.getAllEndpoints()

    suspend fun addEndpoint(name: String, url: String, apiKey: String): Long {
        val endpoints = apiEndpointDao.getAllEndpoints().first()
        val isFirst = endpoints.isEmpty()
        val endpoint = ApiEndpoint(
            name = name,
            url = url,
            apiKey = apiKey,
            isSelected = isFirst
        )
        val generatedId = apiEndpointDao.insertEndpoint(endpoint)
        if (isFirst) {
            // 使用数据库返回的真实 id
            currentEndpoint = endpoint.copy(id = generatedId)
            updateRetrofit(url)
        }
        return generatedId
    }

    suspend fun selectEndpoint(id: Long) {
        apiEndpointDao.clearSelected()
        apiEndpointDao.setSelected(id)
        val endpoints = apiEndpointDao.getAllEndpoints().first()
        val selected = endpoints.find { it.id == id } ?: endpoints.firstOrNull()
        // 即使目标 id 未找到，也回退到第一个端点，避免 apiService 保持 null 导致崩溃
        selected?.let {
            currentEndpoint = it
            updateRetrofit(it.url)
        }
    }

    suspend fun deleteEndpoint(id: Long) {
        val wasSelected = currentEndpoint?.id == id
        apiEndpointDao.deleteEndpoint(id)
        if (wasSelected) {
            val endpoints = apiEndpointDao.getAllEndpoints().first()
            if (endpoints.isNotEmpty()) {
                try {
                    selectEndpoint(endpoints.first().id)
                } catch (e: Exception) {
                    Log.e("ApiManager", "deleteEndpoint fallback select failed", e)
                }
            } else {
                currentEndpoint = null
                apiService = null
                retrofit = null
            }
        }
    }

    suspend fun updateEndpoint(id: Long, name: String, url: String, apiKey: String) {
        val endpoints = apiEndpointDao.getAllEndpoints().first()
        val endpoint = endpoints.find { it.id == id }
        endpoint?.let {
            val updated = it.copy(name = name, url = url, apiKey = apiKey)
            apiEndpointDao.updateEndpoint(updated)
            if (it.isSelected) {
                currentEndpoint = updated
                updateRetrofit(url)
            }
        }
    }

    fun getApiServiceForEndpoint(url: String, apiKey: String? = null): OpenAIApiService {
        val baseUrl = if (url.endsWith("/")) url else "$url/"
        val parsed = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid endpoint URL: $url")
        val testRetrofit = Retrofit.Builder()
            .baseUrl(parsed)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return testRetrofit.create(OpenAIApiService::class.java)
    }

    suspend fun testEndpoint(url: String, apiKey: String): Result<Int> {
        return try {
            val baseUrl = if (url.endsWith("/")) url else "$url/"
            val parsed = baseUrl.toHttpUrlOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid URL: $url"))
            val testRetrofit = Retrofit.Builder()
                .baseUrl(parsed)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val testService = testRetrofit.create(OpenAIApiService::class.java)
            val response = testService.getModels("Bearer $apiKey")
            Result.success(response.data.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

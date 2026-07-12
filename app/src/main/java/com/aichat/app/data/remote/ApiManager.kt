package com.aichat.app.data.remote

import android.util.Log
import com.aichat.app.data.local.ApiEndpointDao
import com.aichat.app.data.model.ApiEndpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private var currentEndpoint: ApiEndpoint? = null
    private var retrofit: Retrofit? = null
    private var apiService: OpenAIApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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

    private fun updateRetrofit(baseUrl: String) {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit?.create(OpenAIApiService::class.java)
        Log.d("ApiManager", "Base URL updated to: $url")
    }

    suspend fun setCurrentEndpoint(endpoint: ApiEndpoint) {
        currentEndpoint = endpoint
        updateRetrofit(endpoint.url)
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
                selectEndpoint(endpoints.first().id)
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
        val testRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return testRetrofit.create(OpenAIApiService::class.java)
    }

    suspend fun testEndpoint(url: String, apiKey: String): Result<Int> {
        return try {
            val testRetrofit = Retrofit.Builder()
                .baseUrl(if (url.endsWith("/")) url else "$url/")
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

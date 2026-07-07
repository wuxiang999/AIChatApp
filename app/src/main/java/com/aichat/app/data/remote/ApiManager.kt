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
    private var currentBaseUrl: String = "http://10.0.2.2/"
    private var retrofit: Retrofit? = null
    private var apiService: ChatApiService? = null

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
        val selected = apiEndpointDao.getSelectedEndpoint()
        if (selected != null) {
            updateBaseUrl(selected.url)
        }
    }

    fun updateBaseUrl(baseUrl: String) {
        currentBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit?.create(ChatApiService::class.java)
        Log.d("ApiManager", "Base URL updated to: $currentBaseUrl")
    }

    fun getApiService(): ChatApiService {
        if (apiService == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit?.create(ChatApiService::class.java)
        }
        return apiService!!
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun getAllEndpoints(): Flow<List<ApiEndpoint>> = apiEndpointDao.getAllEndpoints()

    suspend fun addEndpoint(name: String, url: String, apiKey: String) {
        val endpoint = ApiEndpoint(name = name, url = url, apiKey = apiKey)
        apiEndpointDao.insertEndpoint(endpoint)
    }

    suspend fun selectEndpoint(id: Long) {
        apiEndpointDao.clearSelected()
        apiEndpointDao.setSelected(id)
        val endpoints = apiEndpointDao.getAllEndpoints().first()
        val selected = endpoints.find { it.id == id }
        selected?.let { updateBaseUrl(it.url) }
    }

    suspend fun deleteEndpoint(id: Long) {
        apiEndpointDao.deleteEndpoint(id)
    }
}

package com.aichat.app

import android.app.Application
import android.util.Log
import com.aichat.app.data.remote.ApiManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AIChatApplication : Application() {

    @Inject
    lateinit var apiManager: ApiManager

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AIChatApplication", "apiManager.initialize failed", throwable)
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            try {
                apiManager.initialize()
            } catch (e: Exception) {
                Log.e("AIChatApplication", "apiManager.initialize error", e)
            }
        }
    }
}

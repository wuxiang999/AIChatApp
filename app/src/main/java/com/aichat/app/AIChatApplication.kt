package com.aichat.app

import android.app.Application
import android.util.Log
import com.aichat.app.data.remote.ApiManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AIChatApplication : Application() {

    @Inject
    lateinit var apiManager: ApiManager

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiManager.initialize()
            } catch (e: Exception) {
                Log.e("AIChatApplication", "apiManager.initialize error", e)
            }
        }
    }
}

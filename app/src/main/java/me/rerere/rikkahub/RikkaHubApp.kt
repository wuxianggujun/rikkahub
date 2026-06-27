package me.rerere.rikkahub

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val TAG = "RikkaHubApp"

const val CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID = "chat_completed"
const val CHAT_LIVE_UPDATE_NOTIFICATION_CHANNEL_ID = "chat_live_update"

class RikkaHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RikkaHubInitializer.initializeStandalone(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        RikkaHubInitializer.terminate(this)
    }
}

class AppScope : CoroutineScope by CoroutineScope(
    SupervisorJob()
        + Dispatchers.Main
        + CoroutineName("AppScope")
        + CoroutineExceptionHandler { _, error ->
            Log.e(TAG, "AppScope exception", error)
        }
)

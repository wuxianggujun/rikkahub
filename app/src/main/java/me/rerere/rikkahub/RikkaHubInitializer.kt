package me.rerere.rikkahub

import android.app.Application
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.whl.quickjs.android.QuickJSLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.rerere.common.android.appTempFolder
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.files.FileFolders
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.repository.WorkspaceRepository
import me.rerere.rikkahub.di.appModule
import me.rerere.rikkahub.di.dataSourceModule
import me.rerere.rikkahub.di.repositoryModule
import me.rerere.rikkahub.di.viewModelModule
import me.rerere.rikkahub.utils.CrashHandler
import me.rerere.rikkahub.utils.DatabaseUtil
import me.rerere.workspace.WorkspaceManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin

object RikkaHubInitializer {
    private const val TAG = "RikkaHubInitializer"

    @Volatile
    private var initialized = false

    fun initializeStandalone(application: Application) {
        synchronized(this) {
            if (initialized) return
            startKoin {
                androidLogger()
                androidContext(application)
                workManagerFactory()
                modules(appModule, viewModelModule, dataSourceModule, repositoryModule)
            }
            initializeAfterKoin(application, installCrashHandler = true)
            initialized = true
        }
    }

    fun initializeEmbedded(application: Application) {
        synchronized(this) {
            if (initialized) return
            loadKoinModules(listOf(appModule, viewModelModule, dataSourceModule, repositoryModule))
            initializeAfterKoin(application, installCrashHandler = false)
            initialized = true
        }
    }

    fun terminate(application: Application) {
        runCatching {
            get<AppScope>().cancel()
        }.onFailure { error ->
            Log.w(TAG, "terminate failed", error)
        }
    }

    private fun initializeAfterKoin(application: Application, installCrashHandler: Boolean) {
        application.createNotificationChannel()
        DatabaseUtil.setCursorWindowSize(32 * 1024 * 1024)

        if (installCrashHandler) {
            CrashHandler.install(application)
        }

        runCatching {
            QuickJSLoader.init()
        }.onFailure { error ->
            Log.w(TAG, "QuickJSLoader.init failed", error)
        }

        application.deleteTempFiles()
        application.cleanupToolOutputs()
        application.cleanupWorkspaceTempDirs()
        application.checkWorkspaceIntegrity()
        application.syncManagedFiles()
        incrementLaunchCount()
    }

    private fun incrementLaunchCount() {
        get<AppScope>().launch {
            runCatching {
                val store = get<SettingsStore>()
                val current = store.settingsFlowRaw.first()
                store.update(current.copy(launchCount = current.launchCount + 1))
                Log.i(TAG, "incrementLaunchCount: ${store.settingsFlowRaw.first().launchCount}")
            }.onFailure { error ->
                Log.e(TAG, "incrementLaunchCount failed", error)
            }
        }
    }

    private fun Application.cleanupWorkspaceTempDirs() {
        get<AppScope>().launch(Dispatchers.IO) {
            runCatching {
                get<WorkspaceManager>().cleanupAllTempDirs()
            }.onFailure { error ->
                Log.e(TAG, "cleanupWorkspaceTempDirs failed", error)
            }
        }
    }

    private fun Application.checkWorkspaceIntegrity() {
        get<AppScope>().launch(Dispatchers.IO) {
            runCatching {
                get<WorkspaceRepository>().checkIntegrity()
            }.onFailure { error ->
                Log.e(TAG, "checkWorkspaceIntegrity failed", error)
            }
        }
    }

    private fun Application.deleteTempFiles() {
        get<AppScope>().launch(Dispatchers.IO) {
            val dir = appTempFolder
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    private fun Application.cleanupToolOutputs() {
        get<AppScope>().launch(Dispatchers.IO) {
            runCatching {
                val dir = File(filesDir, FileFolders.TOOL_OUTPUTS)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
        }
    }

    private fun Application.syncManagedFiles() {
        get<AppScope>().launch(Dispatchers.IO) {
            runCatching {
                get<FilesManager>().syncFolder()
            }.onFailure { error ->
                Log.e(TAG, "syncManagedFiles failed", error)
            }
        }
    }

    private fun Application.createNotificationChannel() {
        val notificationManager = NotificationManagerCompat.from(this)
        val chatCompletedChannel = NotificationChannelCompat
            .Builder(
                CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
            .setName(getString(R.string.notification_channel_chat_completed))
            .setVibrationEnabled(true)
            .build()
        notificationManager.createNotificationChannel(chatCompletedChannel)

        val chatLiveUpdateChannel = NotificationChannelCompat
            .Builder(
                CHAT_LIVE_UPDATE_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
            .setName(getString(R.string.notification_channel_chat_live_update))
            .setVibrationEnabled(false)
            .build()
        notificationManager.createNotificationChannel(chatLiveUpdateChannel)

    }

    private inline fun <reified T : Any> get(): T = GlobalContext.get().get()
}

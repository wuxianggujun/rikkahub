package me.rerere.rikkahub

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.rerere.highlight.Highlighter
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.event.AppEventBus
import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext

object RikkaHubEmbeddedWarmup {
    private val warmupMutex = Mutex()

    @Volatile
    private var warmedUp = false

    val isWarmedUp: Boolean
        get() = warmedUp

    fun ensureInitialized(application: Application) {
        RikkaHubInitializer.initializeEmbedded(application)
    }

    suspend fun warmup(application: Application) {
        if (warmedUp) return
        warmupMutex.withLock {
            if (warmedUp) return
            withContext(Dispatchers.Default) {
                ensureInitialized(application)
                val settingsStore = get<SettingsStore>()
                settingsStore.settingsFlow.first { settings -> !settings.init }
                get<OkHttpClient>()
                get<Highlighter>()
                get<AppEventBus>()
            }
            warmedUp = true
        }
    }

    private inline fun <reified T : Any> get(): T = GlobalContext.get().get()
}

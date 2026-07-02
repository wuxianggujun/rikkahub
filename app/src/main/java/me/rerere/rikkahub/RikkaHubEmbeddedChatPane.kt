package me.rerere.rikkahub

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import me.rerere.highlight.Highlighter
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.db.DatabaseMigrationTracker
import me.rerere.rikkahub.data.db.MigrationState
import me.rerere.rikkahub.data.event.AppEvent
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.rikkahub.ui.components.ui.TTSController
import me.rerere.rikkahub.ui.context.LocalASRState
import me.rerere.rikkahub.ui.context.LocalEmbeddedHost
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalSharedTransitionScope
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.rikkahub.ui.hooks.readBooleanPreference
import me.rerere.rikkahub.ui.hooks.readStringPreference
import me.rerere.rikkahub.ui.hooks.rememberCustomAsrState
import me.rerere.rikkahub.ui.hooks.rememberCustomTtsState
import me.rerere.rikkahub.ui.pages.assistant.AssistantPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantBasicPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantExtensionsPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantLocalToolPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantMcpPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantMemoryPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantPromptPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantRequestPage
import me.rerere.rikkahub.ui.pages.backup.BackupPage
import me.rerere.rikkahub.ui.pages.chat.ChatPage
import me.rerere.rikkahub.ui.pages.debug.DebugPage
import me.rerere.rikkahub.ui.pages.extensions.ExtensionsPage
import me.rerere.rikkahub.ui.pages.extensions.PromptPage
import me.rerere.rikkahub.ui.pages.extensions.QuickMessagesPage
import me.rerere.rikkahub.ui.pages.extensions.skills.SkillDetailPage
import me.rerere.rikkahub.ui.pages.extensions.skills.SkillsPage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceDetailPage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspacePage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceTerminalPage
import me.rerere.rikkahub.ui.pages.favorite.FavoritePage
import me.rerere.rikkahub.ui.pages.history.HistoryPage
import me.rerere.rikkahub.ui.pages.imggen.ImageGenPage
import me.rerere.rikkahub.ui.pages.log.LogPage
import me.rerere.rikkahub.ui.pages.search.SearchPage
import me.rerere.rikkahub.ui.pages.setting.SettingFilesPage
import me.rerere.rikkahub.ui.pages.setting.SettingMcpPage
import me.rerere.rikkahub.ui.pages.setting.SettingModelPage
import me.rerere.rikkahub.ui.pages.setting.SettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingPreferencesGeneralPage
import me.rerere.rikkahub.ui.pages.setting.SettingPreferencesNotificationPage
import me.rerere.rikkahub.ui.pages.setting.SettingPreferencesPage
import me.rerere.rikkahub.ui.pages.setting.SettingPreferencesUIPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderDetailPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchDetailPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchPage
import me.rerere.rikkahub.ui.pages.setting.SettingSpeechPage
import me.rerere.rikkahub.ui.pages.share.handler.ShareHandlerPage
import me.rerere.rikkahub.ui.pages.stats.StatsPage
import me.rerere.rikkahub.ui.pages.translator.TranslatorPage
import me.rerere.rikkahub.ui.pages.webview.WebViewPage
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import okhttp3.OkHttpClient
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private const val EMBEDDED_PANE_TAG = "RikkaHubEmbeddedPane"

private enum class RikkaHubEmbeddedWarmupState {
    Loading,
    Ready,
    Error,
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun RikkaHubEmbeddedChatPane(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    var retrySignal by remember(application) { mutableStateOf(0) }
    when (rememberRikkaHubEmbeddedWarmupState(application, retrySignal)) {
        RikkaHubEmbeddedWarmupState.Loading -> {
            RikkaHubEmbeddedLoadingPane(modifier)
            return
        }

        RikkaHubEmbeddedWarmupState.Error -> {
            RikkaHubEmbeddedErrorPane(
                modifier = modifier,
                onRetry = { retrySignal++ }
            )
            return
        }

        RikkaHubEmbeddedWarmupState.Ready -> Unit
    }

    val hostColorScheme = MaterialTheme.colorScheme
    val hostTypography = MaterialTheme.typography
    val hostDarkMode = hostColorScheme.background.luminance() < 0.5f

    RikkahubTheme(
        colorScheme = hostColorScheme,
        darkMode = hostDarkMode,
        typography = hostTypography,
        syncSystemBars = false,
    ) {
        val okHttpClient = koinInject<OkHttpClient>()
        RikkaHubImageLoader(okHttpClient)
        RikkaHubChatPane(
            startScreen = rememberStartChatScreen(),
            highlighter = koinInject(),
            settingsStore = koinInject(),
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun RikkaHubEmbeddedSettingsPane(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    var retrySignal by remember(application) { mutableStateOf(0) }
    when (rememberRikkaHubEmbeddedWarmupState(application, retrySignal)) {
        RikkaHubEmbeddedWarmupState.Loading -> {
            RikkaHubEmbeddedLoadingPane(modifier)
            return
        }

        RikkaHubEmbeddedWarmupState.Error -> {
            RikkaHubEmbeddedErrorPane(
                modifier = modifier,
                onRetry = { retrySignal++ }
            )
            return
        }

        RikkaHubEmbeddedWarmupState.Ready -> Unit
    }

    val hostColorScheme = MaterialTheme.colorScheme
    val hostTypography = MaterialTheme.typography
    val hostDarkMode = hostColorScheme.background.luminance() < 0.5f

    RikkahubTheme(
        colorScheme = hostColorScheme,
        darkMode = hostDarkMode,
        typography = hostTypography,
        syncSystemBars = false,
    ) {
        val okHttpClient = koinInject<OkHttpClient>()
        RikkaHubImageLoader(okHttpClient)
        RikkaHubChatPane(
            startScreen = Screen.Setting,
            highlighter = koinInject(),
            settingsStore = koinInject(),
            modifier = modifier,
            handleSystemBack = true,
            onRootBack = onNavigateBack,
        )
    }
}

@Composable
private fun rememberRikkaHubEmbeddedWarmupState(
    application: Application,
    retrySignal: Int,
): RikkaHubEmbeddedWarmupState {
    var state by remember(application) {
        mutableStateOf(
            if (RikkaHubEmbeddedWarmup.isWarmedUp) {
                RikkaHubEmbeddedWarmupState.Ready
            } else {
                RikkaHubEmbeddedWarmupState.Loading
            }
        )
    }

    LaunchedEffect(application, retrySignal) {
        if (RikkaHubEmbeddedWarmup.isWarmedUp) {
            state = RikkaHubEmbeddedWarmupState.Ready
            return@LaunchedEffect
        }
        state = RikkaHubEmbeddedWarmupState.Loading
        val result = runCatching {
            RikkaHubEmbeddedWarmup.warmup(application)
        }
        result.onFailure { error ->
            Log.w(EMBEDDED_PANE_TAG, "RikkaHub embedded warmup failed", error)
        }
        state = if (result.isSuccess && RikkaHubEmbeddedWarmup.isWarmedUp) {
            RikkaHubEmbeddedWarmupState.Ready
        } else {
            RikkaHubEmbeddedWarmupState.Error
        }
    }
    return state
}

@Composable
private fun RikkaHubEmbeddedLoadingPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RikkaHubEmbeddedErrorPane(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.rikkahub_embedded_init_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.rikkahub_embedded_retry))
            }
        }
    }
}

@Composable
private fun rememberStartChatScreen(): Screen.Chat {
    val context = LocalContext.current
    return remember {
        Screen.Chat(
            id = if (context.readBooleanPreference("create_new_conversation_on_start", true)) {
                Uuid.random().toString()
            } else {
                context.readStringPreference(
                    "lastConversationId",
                    Uuid.random().toString()
                ) ?: Uuid.random().toString()
            }
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
internal fun RikkaHubImageLoader(okHttpClient: OkHttpClient) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient },
                        cacheStrategy = { CacheControlCacheStrategy() },
                    )
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory(scaleToDensity = true))
            }
            .build()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RikkaHubChatPane(
    startScreen: Screen,
    highlighter: Highlighter,
    settingsStore: SettingsStore,
    modifier: Modifier = Modifier,
    handleSystemBack: Boolean = false,
    onRootBack: () -> Unit = {},
    onBackStackChanged: ((MutableList<NavKey>) -> Unit)? = null,
    backStackContent: @Composable (MutableList<NavKey>) -> Unit = {},
) {
    val toastState = rememberToasterState()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    if (settings.init) {
        RikkaHubEmbeddedLoadingPane(modifier)
        return
    }
    val tts = rememberCustomTtsState()
    val asr = rememberCustomAsrState()
    val eventBus = koinInject<AppEventBus>()
    LaunchedEffect(tts) {
        eventBus.events.collect { event ->
            when (event) {
                is AppEvent.Speak -> tts.speak(event.text)
            }
        }
    }
    val migrationState by DatabaseMigrationTracker.state.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(startScreen)
    if (handleSystemBack) {
        BackHandler {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                onRootBack()
            }
        }
    }
    if (onBackStackChanged != null) {
        SideEffect { onBackStackChanged(backStack) }
    }
    backStackContent(backStack)

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalEmbeddedHost provides true,
            LocalNavController provides Navigator(backStack, onRootBack),
            LocalSharedTransitionScope provides this,
            LocalSettings provides settings,
            LocalHighlighter provides highlighter,
            LocalToaster provides toastState,
            LocalTTSState provides tts,
            LocalASRState provides asr,
        ) {
            Toaster(
                state = toastState,
                darkTheme = LocalDarkMode.current,
                richColors = true,
                alignment = Alignment.TopCenter,
                showCloseButton = true,
            )
            TTSController()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    modifier = Modifier.fillMaxSize(),
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        } else {
                            onRootBack()
                        }
                    },
                    transitionSpec = {
                        if (backStack.size == 1) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            slideInHorizontally { it } togetherWith
                                slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                        }
                    },
                    popTransitionSpec = {
                        slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                            slideOutHorizontally { it }
                    },
                    predictivePopTransitionSpec = {
                        slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                            slideOutHorizontally { it }
                    },
                    entryProvider = entryProvider {
                        entry<Screen.Chat>(
                            metadata = NavDisplay.transitionSpec { fadeIn() togetherWith fadeOut() }
                                + NavDisplay.popTransitionSpec { fadeIn() togetherWith fadeOut() }
                        ) { key ->
                            ChatPage(
                                id = Uuid.parse(key.id),
                                text = key.text,
                                files = key.files.map { it.toUri() },
                                nodeId = key.nodeId?.let { Uuid.parse(it) }
                            )
                        }

                        entry<Screen.ShareHandler> { key ->
                            ShareHandlerPage(
                                text = key.text,
                                image = key.streamUri
                            )
                        }

                        entry<Screen.History> {
                            HistoryPage()
                        }

                        entry<Screen.Favorite> {
                            FavoritePage()
                        }

                        entry<Screen.Assistant> {
                            AssistantPage()
                        }

                        entry<Screen.AssistantDetail> { key ->
                            AssistantDetailPage(key.id)
                        }

                        entry<Screen.AssistantBasic> { key ->
                            AssistantBasicPage(key.id)
                        }

                        entry<Screen.AssistantPrompt> { key ->
                            AssistantPromptPage(key.id)
                        }

                        entry<Screen.AssistantMemory> { key ->
                            AssistantMemoryPage(key.id)
                        }

                        entry<Screen.AssistantRequest> { key ->
                            AssistantRequestPage(key.id)
                        }

                        entry<Screen.AssistantMcp> { key ->
                            AssistantMcpPage(key.id)
                        }

                        entry<Screen.AssistantLocalTool> { key ->
                            AssistantLocalToolPage(key.id)
                        }

                        entry<Screen.AssistantInjections> { key ->
                            AssistantExtensionsPage(key.id)
                        }

                        entry<Screen.Translator> {
                            TranslatorPage()
                        }

                        entry<Screen.Setting> {
                            SettingPage()
                        }

                        entry<Screen.Backup> {
                            BackupPage()
                        }

                        entry<Screen.ImageGen> {
                            ImageGenPage()
                        }

                        entry<Screen.WebView> { key ->
                            WebViewPage(key.url, key.content)
                        }

                        entry<Screen.SettingPreferences> {
                            SettingPreferencesPage()
                        }

                        entry<Screen.SettingPreferencesNotification> {
                            SettingPreferencesNotificationPage()
                        }

                        entry<Screen.SettingPreferencesGeneral> {
                            SettingPreferencesGeneralPage()
                        }

                        entry<Screen.SettingPreferencesUI> {
                            SettingPreferencesUIPage()
                        }

                        entry<Screen.SettingProvider> {
                            SettingProviderPage()
                        }

                        entry<Screen.SettingProviderDetail> { key ->
                            val id = Uuid.parse(key.providerId)
                            SettingProviderDetailPage(id = id)
                        }

                        entry<Screen.SettingModels> {
                            SettingModelPage()
                        }

                        entry<Screen.SettingSearch> {
                            SettingSearchPage()
                        }

                        entry<Screen.SettingSearchDetail> { key ->
                            val id = Uuid.parse(key.serviceId)
                            SettingSearchDetailPage(id)
                        }

                        entry<Screen.SettingSpeech> {
                            SettingSpeechPage()
                        }

                        entry<Screen.SettingMcp> {
                            SettingMcpPage()
                        }

                        entry<Screen.SettingFiles> {
                            SettingFilesPage()
                        }

                        entry<Screen.Debug> {
                            DebugPage()
                        }

                        entry<Screen.Log> {
                            LogPage()
                        }

                        entry<Screen.Extensions> {
                            ExtensionsPage()
                        }

                        entry<Screen.QuickMessages> {
                            QuickMessagesPage()
                        }

                        entry<Screen.Prompts> {
                            PromptPage()
                        }

                        entry<Screen.Skills> {
                            SkillsPage()
                        }

                        entry<Screen.Workspaces> {
                            WorkspacePage()
                        }

                        entry<Screen.WorkspaceDetail> { key ->
                            WorkspaceDetailPage(key.id)
                        }

                        entry<Screen.WorkspaceTerminal> { key ->
                            WorkspaceTerminalPage(key.id)
                        }

                        entry<Screen.SkillDetail> { key ->
                            SkillDetailPage(skillName = key.skillName)
                        }

                        entry<Screen.MessageSearch> {
                            SearchPage()
                        }

                        entry<Screen.Stats> {
                            StatsPage()
                        }
                    }
                )
                AnimatedVisibility(
                    visible = migrationState is MigrationState.Migrating,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val state = migrationState as? MigrationState.Migrating
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.db_migrating),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (state != null) {
                                Text(
                                    text = "v${state.from} -> v${state.to}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

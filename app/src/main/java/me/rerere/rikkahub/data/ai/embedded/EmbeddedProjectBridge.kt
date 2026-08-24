package me.rerere.rikkahub.data.ai.embedded

import java.util.concurrent.atomic.AtomicReference

/**
 * Host-side project access exposed to an embedded RikkaHub instance.
 * Implementations must treat every path as workspace-relative and enforce
 * their own project-root and symlink boundary before touching the filesystem.
 */
interface EmbeddedProjectBridge {
    suspend fun snapshot(): EmbeddedProjectContext

    suspend fun findFiles(pattern: String?, maxResults: Int): List<String>

    suspend fun readFile(path: String): EmbeddedProjectFile

    suspend fun writeFile(path: String, content: String): EmbeddedProjectMutation

    suspend fun editFile(
        path: String,
        oldText: String,
        newText: String,
        replaceAll: Boolean,
    ): EmbeddedProjectMutation
}

data class EmbeddedProjectContext(
    val projectName: String,
    val activeFile: String? = null,
    val selection: String? = null,
    val activeFileDirty: Boolean = false,
)

data class EmbeddedProjectFile(
    val path: String,
    val content: String,
    val fromEditorBuffer: Boolean,
    val dirty: Boolean,
)

data class EmbeddedProjectMutation(
    val path: String,
    val replacements: Int = 0,
    val content: String,
    val persisted: Boolean,
    val dirty: Boolean,
)

object EmbeddedProjectBridgeRegistry {
    private val current = AtomicReference<EmbeddedProjectBridge?>(null)

    fun register(bridge: EmbeddedProjectBridge) {
        current.set(bridge)
    }

    fun unregister(bridge: EmbeddedProjectBridge) {
        current.compareAndSet(bridge, null)
    }

    fun current(): EmbeddedProjectBridge? = current.get()
}

package me.rerere.rikkahub.data.ai.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.embedded.EmbeddedProjectBridge

private const val DEFAULT_FIND_LIMIT = 200
private const val MAX_FIND_LIMIT = 1_000

suspend fun createEmbeddedProjectTools(bridge: EmbeddedProjectBridge): List<Tool> {
    val projectContext = bridge.snapshot()
    return listOf(
        Tool(
            name = "host_project_find_files",
            description = "Find files in the current TinaIDE project using a relative glob pattern. Paths in results are project-relative.",
            systemPrompt = { _, _ -> projectContext.toSystemPrompt() },
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("pattern", buildJsonObject {
                            put("type", "string")
                            put("description", "Relative glob such as **/*.cpp or CMakeLists.txt. Defaults to all files.")
                        })
                        put("max_results", buildJsonObject {
                            put("type", "integer")
                            put("description", "Maximum number of results, from 1 to 1000.")
                        })
                    },
                    required = emptyList(),
                )
            },
            execute = {
                val params = it.jsonObject
                val pattern = params.string("pattern")
                val maxResults = params.string("max_results")?.toIntOrNull()
                    ?.coerceIn(1, MAX_FIND_LIMIT)
                    ?: DEFAULT_FIND_LIMIT
                val files = bridge.findFiles(pattern, maxResults)
                listOf(
                    UIMessagePart.Text(
                        buildJsonObject {
                            put("files", files.joinToString("\n"))
                            put("count", files.size)
                        }.toString()
                    )
                )
            },
        ),
        Tool(
            name = "host_project_read_file",
            description = "Read a UTF-8 text file from the current TinaIDE project. The path must be project-relative. Open editor buffers include unsaved changes.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Project-relative file path.")
                        })
                    },
                    required = listOf("path"),
                )
            },
            execute = {
                val path = it.jsonObject.requiredNonEmptyString("path")
                val file = bridge.readFile(path)
                listOf(
                    UIMessagePart.Text(
                        buildJsonObject {
                            put("path", file.path)
                            put("content", file.content)
                            put("fromEditorBuffer", file.fromEditorBuffer)
                            put("dirty", file.dirty)
                        }.toString()
                    )
                )
            },
        ),
        Tool(
            name = "host_project_write_file",
            description = "Write a UTF-8 text file in the current TinaIDE project. The path must be project-relative. Changes to an open file remain in the editor as unsaved content. User approval is required.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Project-relative file path.")
                        })
                        put("content", buildJsonObject {
                            put("type", "string")
                            put("description", "Complete UTF-8 file content. May be empty.")
                        })
                    },
                    required = listOf("path", "content"),
                )
            },
            needsApproval = { true },
            execute = {
                val params = it.jsonObject
                val mutation = bridge.writeFile(
                    path = params.requiredNonEmptyString("path"),
                    content = params.requiredString("content"),
                )
                listOf(UIMessagePart.Text(mutation.toJson().toString()))
            },
        ),
        Tool(
            name = "host_project_edit_file",
            description = "Make an exact text replacement in a UTF-8 file in the current TinaIDE project. The path must be project-relative. User approval is required.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Project-relative file path.")
                        })
                        put("old_text", buildJsonObject {
                            put("type", "string")
                            put("description", "Text to replace; it must match exactly once unless replace_all is true.")
                        })
                        put("new_text", buildJsonObject {
                            put("type", "string")
                            put("description", "Replacement text. May be empty to delete the match.")
                        })
                        put("replace_all", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Replace every occurrence. Defaults to false.")
                        })
                    },
                    required = listOf("path", "old_text", "new_text"),
                )
            },
            needsApproval = { true },
            execute = {
                val params = it.jsonObject
                val replaceAll = params.string("replace_all")?.toBooleanStrictOrNull() ?: false
                val mutation = bridge.editFile(
                    path = params.requiredNonEmptyString("path"),
                    oldText = params.requiredNonEmptyString("old_text"),
                    newText = params.requiredString("new_text"),
                    replaceAll = replaceAll,
                )
                listOf(UIMessagePart.Text(mutation.toJson().toString()))
            },
        ),
    )
}

private fun kotlinx.serialization.json.JsonObject.string(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull

private fun kotlinx.serialization.json.JsonObject.requiredString(name: String): String =
    string(name) ?: error("$name is required")

private fun kotlinx.serialization.json.JsonObject.requiredNonEmptyString(name: String): String =
    requiredString(name).takeIf { it.isNotEmpty() } ?: error("$name must not be empty")

private fun me.rerere.rikkahub.data.ai.embedded.EmbeddedProjectMutation.toJson() = buildJsonObject {
    put("path", path)
    put("replacements", replacements)
    put("content", content)
    put("persisted", persisted)
    put("dirty", dirty)
}

private fun me.rerere.rikkahub.data.ai.embedded.EmbeddedProjectContext.toSystemPrompt(): String = buildString {
    appendLine("<tinaide_project>")
    appendLine("You are embedded in TinaIDE and can inspect or modify the user's current project with host_project_* tools.")
    appendLine("- Current project: ${projectName.ifBlank { "unknown" }}")
    appendLine("- Paths passed to host_project_* tools must be relative to the project root. Never use absolute paths or .. segments.")
    appendLine("- Read files before editing them. Prefer host_project_edit_file for focused changes and host_project_write_file for new or complete files.")
    appendLine("- Writes to files currently open in TinaIDE update the unsaved editor buffer so the user can review and undo them.")
    activeFile?.let { path ->
        appendLine("- Active file: $path${if (activeFileDirty) " (unsaved changes)" else ""}")
    }
    selection?.let { text ->
        appendLine("- Current selection:")
        appendLine("<selection>")
        appendLine(text)
        appendLine("</selection>")
    }
    append("</tinaide_project>")
}

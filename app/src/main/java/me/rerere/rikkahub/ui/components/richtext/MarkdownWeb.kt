package me.rerere.rikkahub.ui.components.richtext

import android.content.Context
import androidx.compose.material3.ColorScheme
import me.rerere.rikkahub.R
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.escapeHtml
import me.rerere.rikkahub.utils.toCssHex

internal const val MERMAID_VERSION = "12.2.0"

/**
 * Build HTML page for markdown preview with support for:
 * - Sanitized Markdown rendering via markdown-it
 * - LaTeX math via KaTeX
 * - Mermaid diagrams
 * - Syntax highlighting via highlight.js
 */
fun buildMarkdownPreviewHtml(context: Context, markdown: String, colorScheme: ColorScheme): String {
    val htmlTemplate = context.assets.open("html/mark.html").bufferedReader(Charsets.UTF_8).use { it.readText() }

    return htmlTemplate
        .replace("{{MARKDOWN_BASE64}}", markdown.base64Encode())
        .replace("{{MERMAID_VERSION}}", MERMAID_VERSION)
        .replace("{{PREVIEW_TITLE}}", context.getString(R.string.code_block_preview).escapeHtml())
        .replace("{{RENDER_ERROR_BASE64}}", context.getString(R.string.markdown_preview_render_error).base64Encode())
        .replace(
            "{{WEBVIEW_UNSUPPORTED_BASE64}}",
            context.getString(R.string.markdown_preview_unsupported_webview).base64Encode()
        )
        .replace("{{BACKGROUND_COLOR}}", colorScheme.background.toCssHex())
        .replace("{{ON_BACKGROUND_COLOR}}", colorScheme.onBackground.toCssHex())
        .replace("{{SURFACE_COLOR}}", colorScheme.surface.toCssHex())
        .replace("{{ON_SURFACE_COLOR}}", colorScheme.onSurface.toCssHex())
        .replace("{{SURFACE_VARIANT_COLOR}}", colorScheme.surfaceVariant.toCssHex())
        .replace("{{ON_SURFACE_VARIANT_COLOR}}", colorScheme.onSurfaceVariant.toCssHex())
        .replace("{{PRIMARY_COLOR}}", colorScheme.primary.toCssHex())
        .replace("{{OUTLINE_COLOR}}", colorScheme.outline.toCssHex())
        .replace("{{OUTLINE_VARIANT_COLOR}}", colorScheme.outlineVariant.toCssHex())
}

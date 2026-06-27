package me.rerere.rikkahub.data.ai.transformers

import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.BuildConfig
import java.io.File

private const val MIME_PDF = "application/pdf"
private const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
private const val MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
private const val MIME_EPUB = "application/epub+zip"

object DocumentAsPromptTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return withContext(Dispatchers.IO) {
            messages.map { message ->
                message.copy(
                    parts = message.parts.toMutableList().apply {
                        val documents = filterIsInstance<UIMessagePart.Document>()
                        if (documents.isNotEmpty()) {
                            documents.forEach { document ->
                                val content = readDocumentContent(document)
                                val path = resolveWorkspacePath(document)
                                val pathAttr = path?.let { " path=\"$it\"" } ?: ""
                                val prompt = """
                                  <UploadFile name="${document.fileName}"$pathAttr>
                                  ```
                                  $content
                                  ```
                                  </UploadFile>
                                  """.trimMargin()
                                add(0, UIMessagePart.Text(prompt))
                            }
                        }
                    }
                )
            }
        }
    }

    private fun parsePdfAsText(file: File): String {
        return invokeDocumentParser("me.rerere.document.PdfParser", "parserPdf", file)
    }

    private fun parseDocxAsText(file: File): String {
        return invokeDocumentParser("me.rerere.document.DocxParser", "parse", file)
    }

    private fun parsePptxAsText(file: File): String {
        return invokeDocumentParser("me.rerere.document.PptxParser", "parse", file)
    }

    private fun parseEpubAsText(file: File): String {
        return invokeDocumentParser("me.rerere.document.EpubParser", "parse", file)
    }

    private fun invokeDocumentParser(className: String, methodName: String, file: File): String {
        val parserClass = Class.forName(className)
        val parser = parserClass.getField("INSTANCE").get(null)
        val method = parserClass.getMethod(methodName, File::class.java)
        return method.invoke(parser, file) as? String ?: ""
    }

    // 上传文件保存在 filesDir/upload 下, 该目录通过 proot 挂载到 workspace 的 /upload
    // 返回文件在 workspace 内的绝对路径, 便于 AI 用 workspace 工具直接读取原始文件
    private fun resolveWorkspacePath(document: UIMessagePart.Document): String? {
        val file = runCatching { document.url.toUri().toFile() }.getOrNull() ?: return null
        if (file.parentFile?.name != "upload") return null
        return "/upload/${file.name}"
    }

    private fun readDocumentContent(document: UIMessagePart.Document): String {
        val file = runCatching { document.url.toUri().toFile() }.getOrNull()
            ?: return "[ERROR, invalid file uri: ${document.fileName}]"
        if (!file.exists() || !file.isFile) {
            return "[ERROR, file not found: ${document.fileName}]"
        }
        return runCatching {
            when (document.mime) {
                MIME_PDF -> readStructuredDocument(document, file, ::parsePdfAsText)
                MIME_DOCX -> readStructuredDocument(document, file, ::parseDocxAsText)
                MIME_PPTX -> readStructuredDocument(document, file, ::parsePptxAsText)
                MIME_EPUB -> readStructuredDocument(document, file, ::parseEpubAsText)
                else -> file.readText()
            }
        }.getOrElse {
            "[ERROR, failed to read file: ${document.fileName}]"
        }
    }

    private fun readStructuredDocument(
        document: UIMessagePart.Document,
        file: File,
        parser: (File) -> String,
    ): String {
        if (!BuildConfig.ENABLE_DOCUMENT_PROMPT_PARSERS) {
            return "[ERROR, document parsing disabled in this build: ${document.fileName}]"
        }
        return parser(file)
    }
}

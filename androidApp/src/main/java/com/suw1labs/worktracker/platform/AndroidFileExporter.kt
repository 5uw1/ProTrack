package com.suw1labs.worktracker.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android implementation: writes the export under `cache/exports/` and launches the
 * system share sheet with a FileProvider URI.
 */
class AndroidFileExporter(private val context: Context) : FileExporter {

    /**
     * Set by the foreground activity: opens the system "create document" picker so the user can
     * choose where to store the file (Downloads, Drive, …). Falls back to the share sheet when
     * no activity is available.
     */
    var saveRequestHandler: ((filename: String, mimeType: String, content: String) -> Unit)? = null

    /**
     * Set by the foreground activity: opens the system "open document" picker and hands back the
     * chosen file's text (null when cancelled or unreadable).
     */
    var openRequestHandler: ((mimeTypes: Array<String>, onResult: (String?) -> Unit) -> Unit)? = null

    override suspend fun shareText(content: String, filename: String, mimeType: String, title: String) {
        val file = withContext(Dispatchers.IO) { saveExportFile(content, filename) }
        shareFile(file, mimeType, title)
    }

    override suspend fun saveText(content: String, filename: String, mimeType: String) {
        val handler = saveRequestHandler
        if (handler != null) {
            withContext(Dispatchers.Main) { handler(filename, mimeType, content) }
        } else {
            shareText(content, filename, mimeType, filename)
        }
    }

    override suspend fun openText(mimeTypes: List<String>, extensions: List<String>): String? {
        val handler = openRequestHandler ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                handler(mimeTypes.toTypedArray()) { text -> if (continuation.isActive) continuation.resume(text) }
            }
        }
    }

    private fun saveExportFile(content: String, filename: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    private fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
}

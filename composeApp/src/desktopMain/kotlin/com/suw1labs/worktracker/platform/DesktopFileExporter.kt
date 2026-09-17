package com.suw1labs.worktracker.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Desktop "share": asks where to save the export with the native save dialog, writes the
 * file and opens it with the default application (e.g. Excel) when possible.
 */
class DesktopFileExporter : FileExporter {

    override suspend fun saveText(content: String, filename: String, mimeType: String) {
        val target = withContext(Dispatchers.Main) { chooseSaveLocation(filename, filename) } ?: return
        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
        }
    }

    override suspend fun shareText(content: String, filename: String, mimeType: String, title: String) {
        val target = withContext(Dispatchers.Main) { chooseSaveLocation(filename, title) } ?: return

        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
        }

        withContext(Dispatchers.IO) {
            runCatching {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(target)
                }
            }
        }
    }

    /** Native open dialog, filtered to [extensions] where the platform supports filters (macOS, Linux). */
    override suspend fun openText(mimeTypes: List<String>, extensions: List<String>): String? {
        val source = withContext(Dispatchers.Main) { chooseOpenLocation(extensions) } ?: return null
        return withContext(Dispatchers.IO) { runCatching { source.readText(Charsets.UTF_8) }.getOrNull() }
    }

    private fun chooseSaveLocation(filename: String, title: String): File? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply {
            file = filename
            isVisible = true
        }
        val dir = dialog.directory ?: return null
        val name = dialog.file ?: return null
        return File(dir, name)
    }

    private fun chooseOpenLocation(extensions: List<String>): File? {
        val dialog = FileDialog(null as Frame?, "Open", FileDialog.LOAD).apply {
            if (extensions.isNotEmpty()) {
                setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
            }
            isVisible = true
        }
        val dir = dialog.directory ?: return null
        val name = dialog.file ?: return null
        return File(dir, name)
    }
}

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

    private fun chooseSaveLocation(filename: String, title: String): File? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply {
            file = filename
            isVisible = true
        }
        val dir = dialog.directory ?: return null
        val name = dialog.file ?: return null
        return File(dir, name)
    }
}

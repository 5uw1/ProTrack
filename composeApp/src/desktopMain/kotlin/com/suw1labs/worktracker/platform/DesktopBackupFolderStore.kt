package com.suw1labs.worktracker.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Desktop: the folder is a plain path (a Google Drive / iCloud Drive / OneDrive folder works, as
 * the file is written atomically). Remembered in the user's Java preferences, not in the database.
 */
class DesktopBackupFolderStore(
    private val prefs: Preferences = Preferences.userRoot().node("com/suw1labs/worktracker")
) : BackupFolderStore {
    private val _folder = MutableStateFlow(prefs.get(KEY_PATH, null)?.let { BackupFolder(it, File(it).name.ifEmpty { it }) })
    override val folder: StateFlow<BackupFolder?> = _folder

    override suspend fun pickFolder(): BackupFolder? {
        val dir = withContext(Dispatchers.Main) { chooseDirectory() } ?: return null
        val chosen = BackupFolder(dir.absolutePath, dir.name.ifEmpty { dir.absolutePath })
        prefs.put(KEY_PATH, chosen.reference)
        _folder.value = chosen
        return chosen
    }

    override fun clearFolder() {
        prefs.remove(KEY_PATH)
        _folder.value = null
    }

    /** Written to a temp file next to the target and moved into place, so a sync client never sees a half-written file. */
    override suspend fun write(filename: String, content: String) {
        withContext(Dispatchers.IO) {
            val dir = currentDirectory()
        if (!dir.isDirectory) throw java.io.IOException("backup folder missing: $dir")
        val target = File(dir, filename)
        val temp = File(dir, ".$filename.tmp")
        temp.writeText(content, StandardCharsets.UTF_8)
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        }
    }

    override suspend fun read(filename: String): String? = withContext(Dispatchers.IO) {
        val file = File(currentDirectory(), filename)
        if (file.isFile) file.readText(StandardCharsets.UTF_8) else null
    }

    private fun currentDirectory(): File = File(_folder.value?.reference ?: throw java.io.IOException("no backup folder chosen"))

    /** macOS: the native dialog can pick folders; elsewhere Swing's chooser in directories-only mode. */
    private fun chooseDirectory(): File? {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            try {
                val dialog = FileDialog(null as Frame?, "Choose backup folder", FileDialog.LOAD).apply { isVisible = true }
                val dir = dialog.directory ?: return null
                val name = dialog.file ?: return null
                return File(dir, name)
            } finally {
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
            }
        }
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose backup folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    private companion object {
        const val KEY_PATH = "autoBackupFolder"
    }
}

package com.suw1labs.worktracker.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A folder the user chose for automatic backups. [reference] is whatever the platform needs to
 * find it again (a SAF tree URI, a security-scoped bookmark, a path); [displayName] is for the UI.
 */
data class BackupFolder(val reference: String, val displayName: String)

/**
 * Platform access to a user-chosen folder (Google Drive, iCloud Drive, a local directory, …) in
 * which the app keeps its automatic backup. The choice is stored outside the database, because
 * it is specific to this device and must not travel inside a backup.
 */
interface BackupFolderStore {
    /** The chosen folder, or null while automatic backups are off. */
    val folder: StateFlow<BackupFolder?>

    /** Opens the platform folder picker; the choice is persisted and published on [folder]. Returns null when cancelled. */
    suspend fun pickFolder(): BackupFolder?

    /** Forgets the folder (the files already written stay where they are). */
    fun clearFolder()

    /** Writes [content] as [filename] in the folder, replacing an existing file. Throws when the folder is gone or not writable. */
    suspend fun write(filename: String, content: String)

    /** Reads [filename] from the folder, or null when there is no such file. Throws when the folder is not readable. */
    suspend fun read(filename: String): String?
}

/** Platforms without a folder picker: automatic backups stay off. */
object NoOpBackupFolderStore : BackupFolderStore {
    override val folder: StateFlow<BackupFolder?> = MutableStateFlow(null)
    override suspend fun pickFolder(): BackupFolder? = null
    override fun clearFolder() = Unit
    override suspend fun write(filename: String, content: String) = throw UnsupportedOperationException("no backup folder")
    override suspend fun read(filename: String): String? = null
}

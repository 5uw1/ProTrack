package com.suw1labs.worktracker.data.backup

import com.suw1labs.worktracker.platform.BackupFolder
import com.suw1labs.worktracker.platform.BackupFolderStore
import com.suw1labs.worktracker.util.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the UI shows next to the folder: when the file was last written, or why it could not be. */
data class AutoBackupState(
    val lastWrittenAt: Long? = null,
    val lastError: Boolean = false,
    val running: Boolean = false
)

/**
 * Mirrors the whole database as one JSON file into the folder from [store] shortly after every
 * change. The database itself stays in the app's private storage: SQLite must not live on a
 * cloud-synced folder, but a self-contained JSON copy can, and it restores on any platform.
 *
 * [createBackup] is injected (instead of the database) so the debounce and skip logic is unit-tested
 * without SQLite.
 */
class AutoBackup(
    private val store: BackupFolderStore,
    private val createBackup: suspend () -> BackupFile,
    private val now: () -> Long = { currentTimeMillis() },
    /** How long to wait after the last change before writing, so a burst of edits is one write. */
    private val settleMillis: Long = DEFAULT_SETTLE_MILLIS
) {
    private val _state = MutableStateFlow(AutoBackupState())
    val state: StateFlow<AutoBackupState> = _state.asStateFlow()
    val folder: StateFlow<BackupFolder?> get() = store.folder

    suspend fun pickFolder(): BackupFolder? = store.pickFolder()
    fun clearFolder() = store.clearFolder()

    private val writeLock = Mutex()
    /** Content of the last successful write (without the timestamp), so unchanged data is not rewritten. */
    private var lastFingerprint: String? = null

    /** Writes after each burst of [changes] and whenever a folder is (re)chosen; runs for the life of [scope]. */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope, changes: Flow<Unit>) {
        scope.launch {
            merge(changes, store.folder.map { lastFingerprint = null })
                .debounce(settleMillis)
                .collect { backupNow() }
        }
    }

    /**
     * Writes the backup now unless no folder is chosen or nothing changed since the last write.
     * Returns true when a file was written.
     */
    suspend fun backupNow(): Boolean = writeLock.withLock {
        if (store.folder.value == null) return false
        _state.value = _state.value.copy(running = true)
        try {
            val backup = createBackup()
            val fingerprint = BackupCodec.encode(backup.copy(exportedAt = 0L))
            if (fingerprint == lastFingerprint) return false
            store.write(FILE_NAME, BackupCodec.encode(backup))
            lastFingerprint = fingerprint
            _state.value = AutoBackupState(lastWrittenAt = now(), lastError = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(lastError = true)
            false
        } finally {
            _state.value = _state.value.copy(running = false)
        }
    }

    /** The backup currently in the folder, or null when there is none. Throws [BackupException] for unreadable content. */
    suspend fun readLatest(): BackupFile? {
        val text = try {
            store.read(FILE_NAME)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupError.IO, e)
        }
        return text?.let { BackupCodec.decode(it) }
    }

    companion object {
        const val FILE_NAME = "worktracker-backup.json"
        const val DEFAULT_SETTLE_MILLIS = 5_000L
    }
}

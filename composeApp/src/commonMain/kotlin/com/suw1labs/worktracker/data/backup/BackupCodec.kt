package com.suw1labs.worktracker.data.backup

import com.suw1labs.worktracker.util.DateFormats
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Why a file could not be imported. Mapped to a translated message by the UI. */
enum class BackupError {
    /** The text is not JSON at all or lacks the `worktracker-backup` marker. */
    NOT_A_BACKUP,
    /** Written by a newer app version with an incompatible layout – update the app first. */
    NEWER_FORMAT,
    /** Recognised as a backup, but the content is damaged or hand-edited beyond repair. */
    CORRUPT,
    /** The platform could not read or write the file. */
    IO
}

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(error.name, cause)

/**
 * Serialises a [BackupFile] to JSON and back. Pure and platform independent, so the format is
 * tested once in `commonTest` and behaves identically on every device.
 */
object BackupCodec {
    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "json"

    /** Types accepted by the "open file" picker; the content is validated afterwards anyway. */
    val OPEN_MIME_TYPES: List<String> = listOf("application/json", "text/plain", "application/octet-stream")

    private val json = Json {
        prettyPrint = true
        // Write every field, even those equal to their default, so a file is fully self-describing.
        encodeDefaults = true
        // Keys added by newer app versions are skipped instead of failing the import.
        ignoreUnknownKeys = true
    }

    fun fileName(exportedAt: Long): String = "worktracker_backup_${DateFormats.fileStamp(exportedAt)}.$FILE_EXTENSION"

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /**
     * Parses [text] into a [BackupFile], upgrading older layouts on the way.
     * @throws BackupException with the reason when the text is not a usable backup.
     */
    fun decode(text: String): BackupFile {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            throw BackupException(BackupError.NOT_A_BACKUP, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.NOT_A_BACKUP, e)
        }
        val format = root["format"]?.jsonPrimitive?.takeIf { it.isString }?.content
        if (format != BackupFile.FORMAT) throw BackupException(BackupError.NOT_A_BACKUP)
        val formatVersion = root["formatVersion"]?.jsonPrimitive?.intOrNull ?: throw BackupException(BackupError.CORRUPT)
        if (formatVersion > BackupFile.FORMAT_VERSION) throw BackupException(BackupError.NEWER_FORMAT)

        val upgraded = upgrade(root, formatVersion)
        val decoded = try {
            json.decodeFromJsonElement(BackupFile.serializer(), upgraded)
        } catch (e: SerializationException) {
            throw BackupException(BackupError.CORRUPT, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.CORRUPT, e)
        }
        return decoded.sanitized()
    }

    /**
     * Brings the raw JSON of an older [BackupFile.FORMAT_VERSION] up to the current layout.
     * Add one `if (fromVersion < N) { … }` block per incompatible change, in ascending order, e.g.
     * renaming a key: `result = JsonObject(result.mapKeys { if (it.key == "old") "new" else it.key })`.
     */
    private fun upgrade(root: JsonObject, fromVersion: Int): JsonObject {
        if (fromVersion < 1) throw BackupException(BackupError.CORRUPT)
        return root
    }
}

/**
 * Repairs relations that cannot be represented in the database: rows with duplicate ids are
 * reduced to the first one, tasks of unknown projects are dropped and time entries pointing at an
 * unknown project or task are kept as unassigned – the same thing the app's foreign keys do when
 * a project is deleted.
 */
internal fun BackupFile.sanitized(): BackupFile {
    val projects = projects.distinctBy { it.id }
    val projectIds = projects.map { it.id }.toSet()
    val tasks = tasks.distinctBy { it.id }.filter { it.projectId in projectIds }
    val taskIds = tasks.map { it.id }.toSet()
    val entries = timeEntries.distinctBy { it.id }.map { e ->
        val projectId = e.projectId?.takeIf { it in projectIds }
        val taskId = e.taskId?.takeIf { it in taskIds }
        if (projectId == e.projectId && taskId == e.taskId) e else e.copy(projectId = projectId, taskId = taskId)
    }
    return copy(
        settings = settings?.copy(id = 1),
        projects = projects,
        tasks = tasks,
        timeEntries = entries,
        attendanceSessions = attendanceSessions.distinctBy { it.id },
        dayRecords = dayRecords.distinctBy { it.id }.distinctBy { it.dayStart }
    )
}

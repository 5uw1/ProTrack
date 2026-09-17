package com.suw1labs.worktracker.data.backup

import com.suw1labs.worktracker.data.DATABASE_VERSION
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import kotlinx.serialization.Serializable

/**
 * Everything the app stores, as one self-describing JSON document that can be moved to another
 * device (Android, iOS or desktop) and restored there.
 *
 * Compatibility rules:
 * - [format] identifies the file; anything else is rejected before parsing.
 * - [formatVersion] is bumped only when the JSON layout changes incompatibly (a renamed or retyped
 *   field). Adding a field with a default keeps the version: older files simply lack it and newer
 *   files carry an extra key that older apps ignore. Incompatible changes get an upgrade step in
 *   [BackupCodec.upgrade].
 * - [schemaVersion] records the database version the export came from – for diagnostics and for
 *   upgrade steps that depend on it.
 *
 * Row ids are exported as they are so relations (task → project, entry → task) stay intact when
 * restoring onto another device.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val formatVersion: Int = FORMAT_VERSION,
    val schemaVersion: Int = DATABASE_VERSION,
    /** Epoch millis of the export. */
    val exportedAt: Long,
    /** Free text such as "android", "ios" or "desktop" – informational only. */
    val platform: String = "",
    val settings: AppSettings? = null,
    val projects: List<Project> = emptyList(),
    val tasks: List<WorkTask> = emptyList(),
    val timeEntries: List<TimeEntry> = emptyList(),
    val attendanceSessions: List<AttendanceSession> = emptyList(),
    val dayRecords: List<DayRecord> = emptyList()
) {
    val summary: BackupSummary
        get() = BackupSummary(
            exportedAt = exportedAt,
            platform = platform,
            projects = projects.size,
            tasks = tasks.size,
            timeEntries = timeEntries.size,
            attendanceSessions = attendanceSessions.size,
            dayRecords = dayRecords.size
        )

    companion object {
        const val FORMAT = "worktracker-backup"
        const val FORMAT_VERSION = 1
    }
}

/** What a backup contains, for the confirmation dialog before it replaces the local data. */
data class BackupSummary(
    val exportedAt: Long,
    val platform: String,
    val projects: Int,
    val tasks: Int,
    val timeEntries: Int,
    val attendanceSessions: Int,
    val dayRecords: Int
)

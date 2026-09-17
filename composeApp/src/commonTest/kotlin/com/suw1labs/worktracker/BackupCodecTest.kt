package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.DATABASE_VERSION
import com.suw1labs.worktracker.data.backup.BackupCodec
import com.suw1labs.worktracker.data.backup.BackupError
import com.suw1labs.worktracker.data.backup.BackupException
import com.suw1labs.worktracker.data.backup.BackupFile
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The backup file format is platform independent, so it is tested once here for every target. */
class BackupCodecTest {

    private fun sampleBackup() = BackupFile(
        exportedAt = 1_800_000_000_000L,
        platform = "test",
        settings = AppSettings(weekdayHours = "8.5,8.5,8.5,8.5,0,0,0", language = "de", paidBreakMinutes = 15),
        projects = listOf(
            Project(id = 1, code = "UNPRODUCTIVE", name = "Unproductive", colorHex = "#F59E0B", isProductive = false, createdAt = 1),
            Project(id = 7, code = "P-2026-0142", name = "Spindle retrofit", client = "Customer AG", budgetHours = 120.0, createdAt = 2)
        ),
        tasks = listOf(
            WorkTask(id = 3, projectId = 1, title = "Meeting", priority = "LOW", reminderEnabled = false, createdAt = 3),
            WorkTask(id = 9, projectId = 7, title = "PLC", deadlineTimestamp = 1_800_100_000_000L, createdAt = 4)
        ),
        timeEntries = listOf(
            TimeEntry(id = 20, projectId = 7, taskId = 9, description = "wiring \"cabinet\"", startTime = 100, endTime = 200, createdAt = 5),
            TimeEntry(id = 21, projectId = null, taskId = null, startTime = 300, endTime = null, createdAt = 6)
        ),
        attendanceSessions = listOf(AttendanceSession(id = 40, clockIn = 100, clockOut = 400, clockOutReason = "LUNCH", createdAt = 7)),
        dayRecords = listOf(DayRecord(id = 50, dayStart = 0, type = "SICK", hours = 8.5, createdAt = 8))
    )

    @Test
    fun roundTrip_preservesEveryRowAndId() {
        val original = sampleBackup()
        val text = BackupCodec.encode(original)
        val decoded = BackupCodec.decode(text)

        assertEquals(original, decoded)
        assertEquals(BackupFile.FORMAT, decoded.format)
        assertEquals(BackupFile.FORMAT_VERSION, decoded.formatVersion)
        assertEquals(DATABASE_VERSION, decoded.schemaVersion)
        assertTrue(text.contains("\"paidBreakMinutes\": 15"), "every field is written, even defaults")
    }

    @Test
    fun decode_olderFileWithoutNewerFields_fallsBackToDefaults() {
        // A file written before `paidBreakMinutes` (schema 6) and `platform` existed.
        val text = """
            {
              "format": "worktracker-backup",
              "formatVersion": 1,
              "schemaVersion": 6,
              "exportedAt": 1700000000000,
              "settings": {"id": 1, "fullTimeWeeklyHours": 42.0, "weekdayHours": "8,8,8,8,8,0,0", "maxWeeklyHours": 45.0, "language": "fr"},
              "projects": [{"id": 2, "code": "P-1", "name": "One", "client": "", "colorHex": "#3B82F6", "budgetHours": 0.0, "status": "ACTIVE", "isProductive": true, "createdAt": 1}]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(text)

        assertEquals(6, decoded.schemaVersion)
        assertEquals("", decoded.platform)
        assertEquals(0, decoded.settings?.paidBreakMinutes)
        assertEquals("fr", decoded.settings?.language)
        assertEquals(listOf(2L), decoded.projects.map { it.id })
        assertTrue(decoded.tasks.isEmpty() && decoded.timeEntries.isEmpty())
    }

    @Test
    fun decode_ignoresKeysAddedByNewerAppsOfTheSameFormatVersion() {
        val text = BackupCodec.encode(sampleBackup())
            .replace("\"platform\": \"test\"", "\"platform\": \"test\", \"futureField\": {\"x\": 1}")
            .replace("\"client\": \"Customer AG\"", "\"client\": \"Customer AG\", \"archivedAt\": 5")

        val decoded = BackupCodec.decode(text)
        assertEquals(sampleBackup(), decoded)
    }

    @Test
    fun decode_rejectsForeignAndBrokenFiles() {
        assertEquals(BackupError.NOT_A_BACKUP, assertFailsWith<BackupException> { BackupCodec.decode("not json at all") }.error)
        assertEquals(BackupError.NOT_A_BACKUP, assertFailsWith<BackupException> { BackupCodec.decode("""{"projects": []}""") }.error)
        assertEquals(BackupError.NOT_A_BACKUP, assertFailsWith<BackupException> { BackupCodec.decode("""[1, 2, 3]""") }.error)
        assertEquals(
            BackupError.NEWER_FORMAT,
            assertFailsWith<BackupException> { BackupCodec.decode("""{"format": "worktracker-backup", "formatVersion": 99, "exportedAt": 1}""") }.error
        )
        assertEquals(
            BackupError.CORRUPT,
            assertFailsWith<BackupException> { BackupCodec.decode("""{"format": "worktracker-backup", "formatVersion": 1, "projects": "nope"}""") }.error
        )
    }

    @Test
    fun decode_repairsDanglingReferencesAndDuplicates() {
        val backup = sampleBackup().copy(
            projects = sampleBackup().projects + Project(id = 7, code = "DUP", name = "duplicate id"),
            tasks = sampleBackup().tasks + WorkTask(id = 99, projectId = 12345, title = "orphan"),
            timeEntries = sampleBackup().timeEntries + TimeEntry(id = 30, projectId = 12345, taskId = 99, startTime = 1, endTime = 2),
            settings = AppSettings(id = 42, language = "en")
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(backup))

        assertEquals(listOf(1L, 7L), decoded.projects.map { it.id })
        assertEquals("Spindle retrofit", decoded.projects.first { it.id == 7L }.name)
        assertEquals(listOf(3L, 9L), decoded.tasks.map { it.id })
        val repaired = decoded.timeEntries.first { it.id == 30L }
        assertNull(repaired.projectId)
        assertNull(repaired.taskId)
        assertEquals(1L, decoded.settings?.id)
    }

    @Test
    fun fileName_isDatedJson() {
        val name = BackupCodec.fileName(1_800_000_000_000L)
        assertTrue(name.startsWith("worktracker_backup_") && name.endsWith(".json"), name)
    }
}

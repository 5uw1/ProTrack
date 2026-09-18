package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.import.WorkAppImporter
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.util.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkAppImporterTest {
    private val sample = """
WORK Export

Date Range;01.08.2026 to 31.08.2026
Worked;169:30
Paused;15:59
Exported at;18 Sep 2026 at 09:25:53


ABSENCE;Type;From;To;Days;;;;Notes
;Sick;5 Aug 2026;6 Aug 2026;2;;;;Flu


PROJECTS;Project;Task;Logged Time
;Office;Email;169:30
;;> Total;169:30


DAYS;Day;Weekday;Logged Time
;3 Aug 2026;Monday;08:59


DAYS IN DETAIL;Day;Project;Task;From;To;Worked;Absence;Notes
;3 Aug 2026;Office;Email;10:03;12:42;02:39;00:00;
;;;Pause;12:42;13:13;00:31;00:00;
;;Office;Email;13:13;19:33;06:20;00:00;
;4 Aug 2026;Office;Email;10:07;12:45;02:38;00:00;
;;;Pause;12:45;13:18;00:33;00:00;
;;Office;Email;13:18;15:27;02:09;00:00;
;;;Pause;15:27;15:51;00:24;00:00;
;;Office;Email;15:51;19:38;03:47;00:00;Cabinet 3
""".trimIndent()

    private fun hm(millis: Long): String = millis.toLocalDateTime().let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }

    @Test
    fun parse_readsDetailRowsAndAbsences() {
        assertTrue(WorkAppImporter.looksLikeWorkExport(sample))
        val export = WorkAppImporter.parse(sample)
        assertEquals(8, export.rows.size)
        assertEquals(2, export.days)
        assertEquals(3, export.rows.count { it.isPause })
        assertEquals(LocalDate(2026, 8, 3), export.rows.first().date)
        assertEquals("Cabinet 3", export.rows.last().notes)
        assertEquals(listOf("Sick"), export.absences.map { it.type })
    }

    @Test
    fun parseDate_acceptsTheCommonFormats() {
        for (text in listOf("3 Aug 2026", "3. Aug. 2026", "03.08.2026", "2026-08-03", "3 août 2026", "3 August 2026")) {
            assertEquals(LocalDate(2026, 8, 3), WorkAppImporter.parseDate(text), text)
        }
    }

    @Test
    fun plan_buildsSessionsAroundPausesAndActivitiesPerRow() {
        val plan = WorkAppImporter.plan(WorkAppImporter.parse(sample), emptyList(), emptyList(), emptyList(), emptyList(), AppSettings())

        assertEquals(listOf("Office"), plan.newProjects.map { it.name })
        assertEquals(listOf("Office" to "Email"), plan.newTasks)
        assertEquals(5, plan.entries.size)
        assertEquals("10:03", hm(plan.entries.first().start))
        assertEquals("12:42", hm(plan.entries.first().end))

        // 3 Aug: 10:03–12:42 (lunch), 13:13–19:33 (end of day); 4 Aug: three periods, the 15:27 pause is a break.
        assertEquals(5, plan.sessions.size)
        assertEquals(listOf("LUNCH", "END_OF_DAY", "LUNCH", "BREAK", "END_OF_DAY"), plan.sessions.map { it.clockOutReason })
        assertEquals("13:13", hm(plan.sessions[1].clockIn))
        assertEquals("19:33", hm(plan.sessions[1].clockOut!!))

        // Sick 5–6 Aug (Wednesday, Thursday) → two credited day records at the daily target.
        assertEquals(2, plan.dayRecords.size)
        assertEquals("SICK", plan.dayRecords.first().type)
        assertEquals(8.0, plan.dayRecords.first().hours)
    }

    @Test
    fun plan_skipsWhatAlreadyExists() {
        val first = WorkAppImporter.plan(WorkAppImporter.parse(sample), emptyList(), emptyList(), emptyList(), emptyList(), AppSettings())
        val existingSessions = first.sessions.mapIndexed { i, s -> s.copy(id = i + 1L) }
        val office = Project(id = 7, code = "Office", name = "Office")
        val second = WorkAppImporter.plan(WorkAppImporter.parse(sample), listOf(office), emptyList(), emptyList(), existingSessions, AppSettings())
        assertTrue(second.newProjects.isEmpty(), "project exists by name")
        assertTrue(second.sessions.isEmpty())
        assertEquals(5, second.skippedSessions)
        assertEquals(5, second.entries.size, "entries are not in the local data yet")
    }
}

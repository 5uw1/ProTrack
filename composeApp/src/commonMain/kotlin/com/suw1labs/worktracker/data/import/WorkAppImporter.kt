package com.suw1labs.worktracker.data.import

import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.util.startOfDayMillis
import com.suw1labs.worktracker.util.toEpochMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/**
 * Reads the CSV export of the iOS app "WORK" (semicolon separated; sections ABSENCE, PROJECTS, DAYS,
 * DAYS IN DETAIL). Only "DAYS IN DETAIL" and "ABSENCE" carry data we keep:
 *
 * ```
 * DAYS IN DETAIL;Day;Project;Task;From;To;Worked;Absence;Notes
 * ;3 Aug 2026;Office;Email;10:03;12:42;02:39;00:00;
 * ;;;Pause;12:42;13:13;00:31;00:00;
 * ```
 *
 * Work rows become activities on the named project / task (created when missing); a run of work
 * rows becomes one clock-in period, every "Pause" row (or an uncovered gap) a clock-out and the next
 * clock-in. Absences become day records. Rows already present locally are skipped, so importing the
 * same export twice changes nothing.
 */
object WorkAppImporter {
    /** One line of "DAYS IN DETAIL". [project] is null for a pause. */
    data class Row(val date: LocalDate, val project: String?, val task: String, val from: LocalTime, val to: LocalTime, val notes: String) {
        val isPause: Boolean get() = project == null
    }

    data class Absence(val type: String, val from: LocalDate, val to: LocalDate, val notes: String)

    data class Export(val rows: List<Row>, val absences: List<Absence>) {
        val days: Int get() = rows.map { it.date }.distinct().size
        val isEmpty: Boolean get() = rows.isEmpty() && absences.isEmpty()
    }

    /** Everything the import will write, computed against the local data so it can be shown before confirming. */
    data class Plan(
        val newProjects: List<Project>,
        val newTasks: List<Pair<String, String>>,
        val sessions: List<AttendanceSession>,
        /** Entries with the project / task still as names (ids exist only after the projects were created). */
        val entries: List<PlannedEntry>,
        val dayRecords: List<DayRecord>,
        val skippedEntries: Int,
        val skippedSessions: Int
    ) {
        val isEmpty: Boolean get() = sessions.isEmpty() && entries.isEmpty() && dayRecords.isEmpty() && newProjects.isEmpty()
    }

    data class PlannedEntry(val project: String, val task: String, val start: Long, val end: Long, val notes: String)

    private val monthNames: Map<String, Int> = buildMap {
        listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec").forEachIndexed { i, m -> put(m, i + 1) }
        listOf("januar", "februar", "märz", "april", "mai", "juni", "juli", "august", "september", "oktober", "november", "dezember").forEachIndexed { i, m -> put(m, i + 1) }
        listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre").forEachIndexed { i, m -> put(m, i + 1) }
        put("mär", 3); put("mrz", 3); put("okt", 10); put("dez", 12); put("sept", 9); put("fév", 2); put("avr", 4); put("juil", 7); put("déc", 12)
    }

    fun looksLikeWorkExport(text: String): Boolean = text.trimStart().startsWith("WORK Export") || text.contains("DAYS IN DETAIL;")

    fun parse(text: String): Export {
        val rows = mutableListOf<Row>()
        val absences = mutableListOf<Absence>()
        var section = ""
        var currentDate: LocalDate? = null
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            if (line.isBlank()) continue
            val cells = line.split(';').map { it.trim() }
            val head = cells[0]
            if (head.isNotEmpty()) {
                section = head.uppercase()
                continue
            }
            when (section) {
                "DAYS IN DETAIL" -> {
                    // ;Day;Project;Task;From;To;Worked;Absence;Notes – Day only on the first row of a day.
                    if (cells.size < 6) continue
                    cells[1].takeIf { it.isNotEmpty() }?.let { parseDate(it)?.let { d -> currentDate = d } }
                    val date = currentDate ?: continue
                    val from = parseTime(cells[4]) ?: continue
                    val to = parseTime(cells[5]) ?: continue
                    val project = cells[2].ifEmpty { null }
                    val task = cells[3]
                    if (project == null && !task.equals("Pause", ignoreCase = true) && task.isNotEmpty()) continue
                    rows += Row(date, project, task, from, to, cells.getOrElse(8) { "" })
                }
                "ABSENCE" -> {
                    // ;Type;From;To;Days;;;;Notes
                    if (cells.size < 4 || cells[1].startsWith("No ", ignoreCase = true)) continue
                    val from = parseDate(cells[2]) ?: continue
                    val to = parseDate(cells[3]) ?: from
                    absences += Absence(cells[1], from, to, cells.getOrElse(8) { "" })
                }
            }
        }
        return Export(rows, absences)
    }

    /** "3 Aug 2026", "3. Aug. 2026", "03.08.2026", "2026-08-03", "3 août 2026". */
    fun parseDate(text: String): LocalDate? {
        val t = text.trim()
        Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").find(t)?.let { m ->
            val (y, mo, d) = m.destructured
            return runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
        }
        Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{4})$""").find(t)?.let { m ->
            val (d, mo, y) = m.destructured
            return runCatching { LocalDate(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
        }
        Regex("""^(\d{1,2})\.?\s+([^\s\d.]+)\.?\s+(\d{4})$""").find(t)?.let { m ->
            val (d, name, y) = m.destructured
            val month = monthNames[name.lowercase()] ?: monthNames[name.lowercase().take(3)] ?: return null
            return runCatching { LocalDate(y.toInt(), month, d.toInt()) }.getOrNull()
        }
        return null
    }

    fun parseTime(text: String): LocalTime? {
        val m = Regex("""^(\d{1,2}):(\d{2})$""").find(text.trim()) ?: return null
        val (h, min) = m.destructured
        return runCatching { LocalTime(h.toInt(), min.toInt()) }.getOrNull()
    }

    /**
     * Turns the export into sessions, activities and absences, skipping what already exists locally
     * (same start and end). Pauses between 11:00 and 14:30 of at least 20 minutes are tagged as lunch.
     */
    fun plan(
        export: Export,
        projects: List<Project>,
        tasks: List<WorkTaskWithProject>,
        entries: List<TimeEntryWithDetails>,
        sessions: List<AttendanceSession>,
        settings: AppSettings,
        colorOffset: Int = projects.size
    ): Plan {
        val existingEntries = entries.filter { it.endTime != null }.map { it.startTime to it.endTime!! }.toHashSet()
        val existingSessions = sessions.filter { it.clockOut != null }.map { it.clockIn to it.clockOut!! }.toHashSet()
        val knownProjects = projects.associateBy { it.name.trim().lowercase() }
        val knownTasks = tasks.groupBy { it.projectName.trim().lowercase() }.mapValues { (_, list) -> list.map { it.title.trim().lowercase() }.toSet() }

        val newProjects = linkedMapOf<String, Project>()
        val newTasks = linkedSetOf<Pair<String, String>>()
        val plannedEntries = mutableListOf<PlannedEntry>()
        val plannedSessions = mutableListOf<AttendanceSession>()
        var skippedEntries = 0
        var skippedSessions = 0

        export.rows.groupBy { it.date }.toSortedMap().forEach { (date, dayRows) ->
            val ordered = dayRows.sortedBy { it.from }
            // Work rows → activities (+ projects / tasks to create).
            ordered.filter { !it.isPause }.forEach { row ->
                val project = row.project!!
                val key = project.trim().lowercase()
                if (key !in knownProjects && key !in newProjects) {
                    newProjects[key] = ProjectImporter.toProjects(listOf(ProjectImporter.ParsedProject(code = project.trim(), name = project.trim(), client = "")), colorOffset + newProjects.size).first()
                }
                if (row.task.isNotBlank() && knownTasks[key]?.contains(row.task.trim().lowercase()) != true) newTasks += project.trim() to row.task.trim()
                val start = at(date, row.from)
                val end = at(date, row.to).let { if (it <= start) it + 24 * 3600_000L else it }
                if (start to end in existingEntries) skippedEntries++ else plannedEntries += PlannedEntry(project.trim(), row.task.trim(), start, end, row.notes)
            }
            // Runs of work rows → clock-in periods; a pause row or an uncovered gap ends one.
            var sessionStart: Long? = null
            var sessionEnd: Long? = null
            fun close(reason: ClockOutReason?) {
                val s = sessionStart ?: return
                val e = sessionEnd ?: return
                if (e > s) {
                    if (s to e in existingSessions) skippedSessions++ else plannedSessions += AttendanceSession(clockIn = s, clockOut = e, clockOutReason = reason?.name, createdAt = s)
                }
                sessionStart = null; sessionEnd = null
            }
            ordered.forEach { row ->
                val start = at(date, row.from)
                val end = at(date, row.to).let { if (it <= start) it + 24 * 3600_000L else it }
                if (row.isPause) {
                    close(pauseReason(row.from, end - start))
                    return@forEach
                }
                if (sessionStart != null && start > sessionEnd!!) close(pauseReason(sessionEnd!!, start - sessionEnd!!, date))
                if (sessionStart == null) sessionStart = start
                sessionEnd = maxOf(sessionEnd ?: end, end)
            }
            close(ClockOutReason.END_OF_DAY)
        }

        val dayRecords = mutableListOf<DayRecord>()
        export.absences.forEach { absence ->
            var day = absence.from
            while (day <= absence.to) {
                val iso = day.dayOfWeek.isoDayNumber
                if (settings.isWorkDay(iso)) {
                    val type = absenceType(absence.type)
                    dayRecords += DayRecord(
                        dayStart = day.startOfDayMillis(),
                        type = type.name,
                        hours = settings.targetHoursFor(iso),
                        label = if (type == AbsenceType.OTHER) absence.type else "",
                        note = absence.notes
                    )
                }
                day = day.plus(1, DateTimeUnit.DAY)
            }
        }

        return Plan(newProjects.values.toList(), newTasks.toList(), plannedSessions, plannedEntries, dayRecords, skippedEntries, skippedSessions)
    }

    private fun at(date: LocalDate, time: LocalTime): Long = LocalDateTime(date, time).toEpochMillis()

    private fun pauseReason(from: LocalTime, millis: Long): ClockOutReason =
        if (from.hour in 11..14 && millis >= 20 * 60_000L) ClockOutReason.LUNCH else ClockOutReason.BREAK

    private fun pauseReason(fromMillis: Long, millis: Long, date: LocalDate): ClockOutReason {
        val hour = ((fromMillis - date.startOfDayMillis()) / 3600_000L).toInt()
        return if (hour in 11..14 && millis >= 20 * 60_000L) ClockOutReason.LUNCH else ClockOutReason.BREAK
    }

    private fun absenceType(text: String): AbsenceType {
        val t = text.lowercase()
        return when {
            "sick" in t || "krank" in t || "malad" in t -> AbsenceType.SICK
            "public" in t || "feiertag" in t || "férié" in t -> AbsenceType.PUBLIC_HOLIDAY
            "vacation" in t || "holiday" in t || "urlaub" in t || "ferien" in t || "vacance" in t -> AbsenceType.HOLIDAY
            "comp" in t || "überzeit" in t -> AbsenceType.COMPENSATION
            "train" in t || "education" in t || "schul" in t || "weiterbildung" in t || "formation" in t -> AbsenceType.EDUCATION
            else -> AbsenceType.OTHER
        }
    }
}

package com.suw1labs.worktracker.ui.timesheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.formatFixed

private val TimeColumn = 64.dp
private val HoursColumn = 56.dp

/**
 * The week as a table: every activity a row with its start, end, project, task and note, grouped
 * by day with the day's clock-in periods on its heading line. Made for typing: Tab goes through the
 * fields, ↑/↓ to the row above or below, Ctrl/⌘+N adds a row to the day being edited.
 */
@Composable
fun WeekTable(
    days: List<TimesheetDay>,
    now: Long,
    activeProjects: List<Project>,
    allProjects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    actions: TimesheetActions,
    focusEntryId: Long?,
    onFocusEntryHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = strings
    var editingDay by remember { mutableStateOf<Int?>(null) }
    val allEntries = remember(days) { days.flatMap { it.entries } }

    fun addRow(dayIndex: Int) {
        val day = days[dayIndex]
        val last = day.entries.lastOrNull()
        val start = last?.let { it.endTime ?: now } ?: day.sessions.firstOrNull()?.clockIn ?: ClockTime.at(day.range.start, 8 * 60)
        val end = minOf(start + 3600_000L, day.range.endExclusive)
        if (end > start) actions.create(last?.projectId, last?.taskId, start, end)
    }

    LazyColumn(
        modifier = modifier
            .testTag("timesheet_table")
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.N && (event.isCtrlPressed || event.isMetaPressed)) {
                    addRow(editingDay ?: days.indexOfFirst { it.range.contains(now) }.coerceAtLeast(0))
                    true
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item { HeaderRow() }
        days.forEachIndexed { dayIndex, day ->
            item(key = "day_${day.range.start}") {
                DayHeading(day, now, actions, onAddRow = { addRow(dayIndex) })
            }
            items(day.entries, key = { "entry_${it.id}" }) { entry ->
                EntryRow(
                    entry = entry,
                    dayStart = day.range.start,
                    now = now,
                    projects = projectChoices(activeProjects, allProjects, entry.projectId),
                    tasks = taskChoices(tasks, entry.projectId, entry.taskId),
                    all = allEntries,
                    actions = actions,
                    requestFocus = focusEntryId == entry.id,
                    onFocusHandled = onFocusEntryHandled,
                    onFocused = { editingDay = dayIndex },
                )
            }
        }
        item { Spacer(Modifier.size(48.dp)) }
    }
}

@Composable
private fun HeaderRow() {
    val t = strings
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(t.startLabel, style = style, color = color, modifier = Modifier.width(TimeColumn))
        Text(t.endLabel, style = style, color = color, modifier = Modifier.width(TimeColumn))
        Text(t.hoursLabel, style = style, color = color, modifier = Modifier.width(HoursColumn))
        Text(t.sapProject, style = style, color = color, modifier = Modifier.weight(1.4f))
        Text(t.category, style = style, color = color, modifier = Modifier.weight(1f))
        Text(t.noteLabel, style = style, color = color, modifier = Modifier.weight(1.6f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun DayHeading(day: TimesheetDay, now: Long, actions: TimesheetActions, onAddRow: () -> Unit) {
    val t = strings
    val colors = MaterialTheme.colorScheme
    val isToday = day.range.contains(now)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isToday) colors.primary.copy(alpha = 0.08f) else colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            DateFormats.fullDate(day.range.start),
            fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = if (isToday) colors.primary else colors.onSurface,
            modifier = Modifier.width(170.dp)
        )
        Text(day.workedSeconds(now).let { if (it > 0) TimeFormat.hm(it) else "–" }, fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(56.dp))
        Text(t.attendancePeriod, fontSize = 12.sp, color = colors.onSurfaceVariant)
        day.sessions.forEach { session -> SessionCells(session, day.range.start, actions) }
        IconButton(
            onClick = {
                // Default: from the day's first to its last activity, or a normal office day.
                val start = day.entries.minOfOrNull { it.startTime } ?: ClockTime.at(day.range.start, 8 * 60)
                val end = day.entries.mapNotNull { it.endTime }.maxOrNull()?.takeIf { it > start } ?: ClockTime.at(day.range.start, 17 * 60)
                actions.addSession(start, end)
            },
            modifier = Modifier.size(32.dp).testTag("timesheet_add_session_${day.range.start}")
        ) { Icon(Icons.Default.Add, contentDescription = t.addClockPeriod, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAddRow, modifier = Modifier.testTag("timesheet_add_row_${day.range.start}")) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(t.newActivity)
        }
    }
}

@Composable
private fun SessionCells(session: AttendanceSession, dayStart: Long, actions: TimesheetActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeField(
            minutes = ClockTime.minutesOf(session.clockIn, dayStart),
            onCommit = { m ->
                val start = ClockTime.at(dayStart, m)
                val ok = session.clockOut == null || start < session.clockOut
                if (ok) actions.saveSession(session.copy(clockIn = start))
                ok
            },
            testTag = "timesheet_session_in_${session.id}"
        )
        Text("–", modifier = Modifier.padding(horizontal = 4.dp))
        TimeField(
            minutes = session.clockOut?.let { ClockTime.minutesOf(it, dayStart) },
            enabled = session.clockOut != null,
            placeholder = "…",
            onCommit = { m ->
                val end = ClockTime.at(dayStart, m)
                val ok = end > session.clockIn
                if (ok) actions.saveSession(session.copy(clockOut = end))
                ok
            },
            testTag = "timesheet_session_out_${session.id}"
        )
        if (session.clockOut != null) {
            IconButton(onClick = { actions.deleteSession(session.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = strings.delete, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: TimeEntryWithDetails,
    dayStart: Long,
    now: Long,
    projects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    all: List<TimeEntryWithDetails>,
    actions: TimesheetActions,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onFocused: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val startFocus = remember { FocusRequester() }
    var rowFocused by remember { mutableStateOf(false) }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            runCatching { startFocus.requestFocus() }
            onFocusHandled()
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (rowFocused) colors.primary.copy(alpha = 0.06f) else colors.surface.copy(alpha = 0f))
                .onFocusChanged { rowFocused = it.hasFocus; if (it.hasFocus) onFocused() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag("timesheet_row_${entry.id}")
        ) {
            TimeField(
                minutes = ClockTime.minutesOf(entry.startTime, dayStart),
                onCommit = { m ->
                    val saved = TimesheetEdits.retimed(entry, ClockTime.at(dayStart, m), entry.endTime, all)
                    saved?.let(actions.save)
                    saved != null
                },
                focusRequester = startFocus,
                testTag = "timesheet_start_${entry.id}"
            )
            TimeField(
                minutes = entry.endTime?.let { ClockTime.minutesOf(it, dayStart) },
                enabled = !entry.isRunning,
                placeholder = "…",
                onCommit = { m ->
                    val saved = TimesheetEdits.retimed(entry, entry.startTime, ClockTime.at(dayStart, m), all)
                    saved?.let(actions.save)
                    saved != null
                },
                testTag = "timesheet_end_${entry.id}"
            )
            Text(
                // Always two decimals, so the column lines up (1.30 next to 1.47).
                (entry.durationSeconds(now) / 3600.0).formatFixed(2),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(HoursColumn)
            )
            ProjectPicker(
                projects = projects,
                selectedId = entry.projectId,
                onSelect = { id -> if (id != entry.projectId) actions.save(listOf(entry.toEntity().copy(projectId = id, taskId = null))) },
                modifier = Modifier.weight(1.4f),
                testTag = "timesheet_project_${entry.id}"
            )
            TaskPicker(
                tasks = tasks,
                selectedId = entry.taskId,
                enabled = entry.projectId != null,
                onSelect = { id -> if (id != entry.taskId) actions.save(listOf(entry.toEntity().copy(taskId = id))) },
                modifier = Modifier.weight(1f),
                testTag = "timesheet_task_${entry.id}"
            )
            NoteField(
                text = entry.description,
                onCommit = { note -> actions.save(listOf(entry.toEntity().copy(description = note))) },
                modifier = Modifier.weight(1.6f),
                testTag = "timesheet_note_${entry.id}"
            )
            IconButton(onClick = { actions.delete(entry) }, modifier = Modifier.size(32.dp).testTag("timesheet_delete_${entry.id}")) {
                Icon(Icons.Default.Close, contentDescription = strings.delete, modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))
    }
}

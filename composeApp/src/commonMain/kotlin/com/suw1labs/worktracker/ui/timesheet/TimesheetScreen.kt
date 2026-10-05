package com.suw1labs.worktracker.ui.timesheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
import com.suw1labs.worktracker.ui.components.ProjectDropdown
import com.suw1labs.worktracker.ui.components.TaskDropdown
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.DateRange
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.ReportPeriodType
import com.suw1labs.worktracker.util.TimeFormat
import kotlinx.coroutines.launch

/** One day of the week on screen: its activities (by start time) and clock-in periods. */
data class TimesheetDay(val range: DateRange, val entries: List<TimeEntryWithDetails>, val sessions: List<AttendanceSession>) {
    /** Logged time inside this day (a running activity counts until [now]). */
    fun workedSeconds(now: Long): Long = entries.sumOf { e ->
        val start = maxOf(e.startTime, range.start)
        val end = minOf(e.endTime ?: now, range.endExclusive)
        ((end - start) / 1000L).coerceAtLeast(0L)
    }

    fun lanes(now: Long): Map<Long, Lane> = TimesheetLayout.lanes(entries.associate { it.id to (it.startTime to (it.endTime ?: now)) })
}

sealed interface TimesheetSelection {
    data class Entry(val id: Long) : TimesheetSelection
    data class Session(val id: Long) : TimesheetSelection
}

/** Everything the calendar and the table may change. */
class TimesheetActions(
    val save: (List<TimeEntry>) -> Unit,
    val create: (projectId: Long?, taskId: Long?, start: Long, end: Long) -> Unit,
    val delete: (TimeEntryWithDetails) -> Unit,
    val saveSession: (AttendanceSession) -> Unit,
    val addSession: (start: Long, end: Long) -> Unit,
    val deleteSession: (Long) -> Unit,
)

enum class TimesheetMode { CALENDAR, TABLE }

/** True once the database holds what [edit] saved. */
private fun TimeEntryWithDetails.matches(edit: TimeEntry): Boolean =
    startTime == edit.startTime && endTime == edit.endTime && projectId == edit.projectId && taskId == edit.taskId && description == edit.description

/** This entry as [edit] changed it, with the project and task details looked up for a new project or task. */
private fun TimeEntryWithDetails.withEdit(edit: TimeEntry, projects: List<Project>, tasks: List<WorkTaskWithProject>): TimeEntryWithDetails {
    val project = if (edit.projectId == projectId) null else projects.firstOrNull { it.id == edit.projectId }
    val sameProject = edit.projectId == projectId
    return copy(
        startTime = edit.startTime,
        endTime = edit.endTime,
        description = edit.description,
        projectId = edit.projectId,
        projectCode = if (sameProject) projectCode else project?.code,
        projectName = if (sameProject) projectName else project?.name,
        projectColor = if (sameProject) projectColor else project?.colorHex,
        projectProductive = if (sameProject) projectProductive else project?.isProductive,
        taskId = edit.taskId,
        taskTitle = if (edit.taskId == taskId) taskTitle else tasks.firstOrNull { it.id == edit.taskId }?.title,
    )
}

/**
 * The Timesheet tab (wide windows only): a whole week on one screen, edited with mouse and
 * keyboard, as a calendar or as a table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesheetScreen(viewModel: TrackerViewModel, modifier: Modifier = Modifier) {
    val t = strings
    val now by viewModel.now.collectAsState()
    val entriesState = viewModel.allEntries.collectAsState()
    val sessionsState = viewModel.allSessions.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val allProjectsState = viewModel.allProjects.collectAsState()
    val allProjects by allProjectsState
    val tasksState = viewModel.allTasks.collectAsState()
    val tasks by tasksState
    val snackbarHost = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var weekStart by remember { mutableStateOf(DateRanges.weekRange(now).start) }
    var mode by remember { mutableStateOf(TimesheetMode.CALENDAR) }
    var selection by remember { mutableStateOf<TimesheetSelection?>(null) }
    var focusEntryId by remember { mutableStateOf<Long?>(null) }
    val calendarFocus = remember { FocusRequester() }
    val inspectorStartFocus = remember { FocusRequester() }

    // Edits show at once, before the database has them back: two quick keystrokes (or key repeat)
    // must build on each other, not both on the state before the first one was saved.
    val pending = remember { mutableStateMapOf<Long, TimeEntry>() }
    val entries = entriesState.value
    LaunchedEffect(entries) {
        pending.keys.toList().forEach { id ->
            val saved = entries.firstOrNull { it.id == id }
            if (saved == null || saved.matches(pending[id]!!)) pending.remove(id)
        }
    }
    val week = DateRanges.weekRange(weekStart)
    val daysState = remember {
        derivedStateOf {
            val range = DateRanges.weekRange(weekStart)
            val shown = entriesState.value.map { e -> pending[e.id]?.let { e.withEdit(it, allProjectsState.value, tasksState.value) } ?: e }
            DateRanges.daysIn(range).map { day ->
                TimesheetDay(
                    range = day,
                    entries = shown.filter { day.contains(it.startTime) }.sortedBy { it.startTime },
                    sessions = sessionsState.value.filter { day.contains(it.clockIn) }.sortedBy { it.clockIn }
                )
            }
        }
    }
    val days = daysState.value
    fun shiftWeek(delta: Int) {
        weekStart = DateRanges.shiftAnchor(ReportPeriodType.WEEK, weekStart, delta)
        selection = null
    }

    val actions = remember(viewModel, snackbarHost) {
        TimesheetActions(
            save = { edited ->
                edited.forEach { pending[it.id] = it }
                viewModel.updateEntries(edited)
            },
            create = { projectId, taskId, start, end ->
                viewModel.addManualEntry(projectId, taskId, "", start, end) { id ->
                    selection = TimesheetSelection.Entry(id)
                    // The table puts the cursor into the new row's start field.
                    if (mode == TimesheetMode.TABLE) focusEntryId = id
                }
            },
            delete = { entry ->
                viewModel.deleteTimeEntry(entry.id)
                if (selection == TimesheetSelection.Entry(entry.id)) selection = null
                scope.launch {
                    val result = snackbarHost.showSnackbar(message = t.activityDeleted, actionLabel = t.undo, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
                }
            },
            saveSession = { viewModel.updateSession(it) },
            addSession = { start, end -> viewModel.addManualSession(start, end, null) },
            deleteSession = { id ->
                viewModel.deleteSession(id)
                if (selection == TimesheetSelection.Session(id)) selection = null
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.PageUp -> { shiftWeek(-1); true }
                    Key.PageDown -> { shiftWeek(1); true }
                    else -> false
                }
            }
            .testTag("timesheet_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            IconButton(onClick = { shiftWeek(-1) }, modifier = Modifier.testTag("timesheet_previous_week")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = t.previousWeek)
            }
            IconButton(onClick = { shiftWeek(1) }, modifier = Modifier.testTag("timesheet_next_week")) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = t.nextWeek)
            }
            Text(DateRanges.label(ReportPeriodType.WEEK, weekStart), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.width(12.dp))
            Text(t.weekTotal(TimeFormat.hm(days.sumOf { it.workedSeconds(now) })), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            if (!week.contains(now)) {
                TextButton(onClick = { weekStart = DateRanges.weekRange(now).start; selection = null }) { Text(t.thisWeek) }
            }
            Spacer(Modifier.weight(1f))
            SingleChoiceSegmentedButtonRow {
                TimesheetMode.entries.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(index, TimesheetMode.entries.size),
                        modifier = Modifier.testTag("timesheet_mode_${m.name.lowercase()}")
                    ) { Text(if (m == TimesheetMode.CALENDAR) t.timesheetCalendar else t.timesheetTable) }
                }
            }
        }
        Text(
            if (mode == TimesheetMode.CALENDAR) t.timesheetCalendarKeys else t.timesheetTableKeys,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        when (mode) {
            TimesheetMode.CALENDAR -> Row(modifier = Modifier.fillMaxSize()) {
                WeekCalendar(
                    days = days,
                    latestDays = { daysState.value },
                    now = now,
                    selection = selection,
                    onSelect = { selection = it },
                    actions = actions,
                    onEditSelected = { runCatching { inspectorStartFocus.requestFocus() } },
                    focusRequester = calendarFocus,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Spacer(Modifier.width(16.dp))
                Inspector(
                    days = days,
                    now = now,
                    selection = selection,
                    activeProjects = activeProjects,
                    allProjects = allProjects,
                    tasks = tasks,
                    actions = actions,
                    startFocus = inspectorStartFocus,
                    modifier = Modifier.width(320.dp).fillMaxHeight()
                )
            }
            TimesheetMode.TABLE -> WeekTable(
                days = days,
                now = now,
                activeProjects = activeProjects,
                allProjects = allProjects,
                tasks = tasks,
                actions = actions,
                focusEntryId = focusEntryId,
                onFocusEntryHandled = { focusEntryId = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** The side panel of the calendar: the selected activity or clock-in period, every field typeable. */
@Composable
private fun Inspector(
    days: List<TimesheetDay>,
    now: Long,
    selection: TimesheetSelection?,
    activeProjects: List<Project>,
    allProjects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    actions: TimesheetActions,
    startFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val t = strings
    val colors = MaterialTheme.colorScheme
    val all = days.flatMap { it.entries }
    Surface(color = colors.surfaceVariant.copy(alpha = 0.35f), shape = MaterialTheme.shapes.large, modifier = modifier.testTag("timesheet_inspector")) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selection) {
                is TimesheetSelection.Entry -> {
                    val entry = all.firstOrNull { it.id == selection.id }
                    val day = entry?.let { e -> days.firstOrNull { it.range.contains(e.startTime) } }
                    if (entry == null || day == null) {
                        Text(t.timesheetSelectHint, color = colors.onSurfaceVariant)
                        return@Column
                    }
                    Text(DateFormats.fullDate(day.range.start), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    ProjectDropdown(
                        projects = projectChoices(activeProjects, allProjects, entry.projectId),
                        selectedProjectId = entry.projectId,
                        onSelect = { id -> if (id != entry.projectId) actions.save(listOf(entry.toEntity().copy(projectId = id, taskId = null))) },
                        testTag = "inspector_project"
                    )
                    if (entry.projectId != null) {
                        TaskDropdown(
                            tasks = taskChoices(tasks, entry.projectId, entry.taskId),
                            selectedTaskId = entry.taskId,
                            onSelect = { id -> if (id != entry.taskId) actions.save(listOf(entry.toEntity().copy(taskId = id))) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledTime(t.startLabel) {
                            TimeField(
                                minutes = ClockTime.minutesOf(entry.startTime, day.range.start),
                                onCommit = { m ->
                                    val saved = TimesheetEdits.retimed(entry, ClockTime.at(day.range.start, m), entry.endTime, all)
                                    saved?.let(actions.save)
                                    saved != null
                                },
                                focusRequester = startFocus,
                                testTag = "inspector_start"
                            )
                        }
                        LabeledTime(t.endLabel) {
                            TimeField(
                                minutes = entry.endTime?.let { ClockTime.minutesOf(it, day.range.start) },
                                enabled = !entry.isRunning,
                                placeholder = "…",
                                onCommit = { m ->
                                    val saved = TimesheetEdits.retimed(entry, entry.startTime, ClockTime.at(day.range.start, m), all)
                                    saved?.let(actions.save)
                                    saved != null
                                },
                                testTag = "inspector_end"
                            )
                        }
                        LabeledTime(t.durationLabel) {
                            Text(TimeFormat.hm(entry.durationSeconds(now)), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 7.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t.noteLabel, fontSize = 12.sp, color = colors.onSurfaceVariant)
                        NoteField(
                            text = entry.description,
                            onCommit = { note -> actions.save(listOf(entry.toEntity().copy(description = note))) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "inspector_note"
                        )
                    }
                    OutlinedButton(onClick = { actions.delete(entry) }, modifier = Modifier.testTag("inspector_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t.delete)
                    }
                }
                is TimesheetSelection.Session -> {
                    val session = days.flatMap { it.sessions }.firstOrNull { it.id == selection.id }
                    val day = session?.let { s -> days.firstOrNull { it.range.contains(s.clockIn) } }
                    if (session == null || day == null) {
                        Text(t.timesheetSelectHint, color = colors.onSurfaceVariant)
                        return@Column
                    }
                    Text(DateFormats.fullDate(day.range.start), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(t.attendancePeriod, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledTime(t.clockInLabel) {
                            TimeField(
                                minutes = ClockTime.minutesOf(session.clockIn, day.range.start),
                                onCommit = { m ->
                                    val start = ClockTime.at(day.range.start, m)
                                    val ok = session.clockOut == null || start < session.clockOut
                                    if (ok) actions.saveSession(session.copy(clockIn = start))
                                    ok
                                },
                                focusRequester = startFocus,
                                testTag = "inspector_clock_in"
                            )
                        }
                        LabeledTime(t.clockOutLabel) {
                            TimeField(
                                minutes = session.clockOut?.let { ClockTime.minutesOf(it, day.range.start) },
                                enabled = session.clockOut != null,
                                placeholder = "…",
                                onCommit = { m ->
                                    val end = ClockTime.at(day.range.start, m)
                                    val ok = end > session.clockIn
                                    if (ok) actions.saveSession(session.copy(clockOut = end))
                                    ok
                                },
                                testTag = "inspector_clock_out"
                            )
                        }
                    }
                    if (session.clockOut != null) {
                        OutlinedButton(onClick = { actions.deleteSession(session.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t.delete)
                        }
                    }
                }
                null -> Text(t.timesheetSelectHint, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LabeledTime(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        Spacer(Modifier.height(0.dp))
    }
}

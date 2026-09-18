package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.EntryNeighbours
import com.suw1labs.worktracker.data.report.NeighbourAdjustment
import com.suw1labs.worktracker.ui.theme.AmberWarning
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.suw1labs.worktracker.ui.components.FormDialog
import com.suw1labs.worktracker.ui.components.TaskDropdown
import com.suw1labs.worktracker.ui.components.DateTimePickerDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.ProjectDropdown
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.ui.i18n.strings

/** Button that shows a date/time and opens the picker. */
@Composable
private fun DateTimeField(
    label: String,
    millis: Long?,
    placeholder: String,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    timeOnly: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(millis?.let { if (timeOnly) DateFormats.hourMinute(it) else DateFormats.dateTime(it) } ?: placeholder, fontSize = 13.sp)
        }
    }
    if (showPicker) {
        DateTimePickerDialog(
            initialMillis = millis,
            title = label,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPick(it)
                showPicker = false
            },
            dateSelectable = !timeOnly
        )
    }
}

/**
 * Add or edit an activity entry. For a running entry the end time stays open.
 *
 * When editing, [previous] / [next] are the activities that touch the entry's old start / end
 * (see [EntryNeighbours]). Moving a boundary then asks whether that neighbour should move along
 * (keeping the day contiguous) or stay, which leaves a gap to assign later or an overlap.
 * The choice comes back in [onSave] as a [NeighbourAdjustment].
 */
@Composable
fun EntryFormDialog(
    entry: TimeEntryWithDetails?,
    projects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    onDismiss: () -> Unit,
    onSave: (projectId: Long?, taskId: Long?, description: String, start: Long, end: Long?, adjust: NeighbourAdjustment) -> Unit,
    initialDayStart: Long? = null,
    previous: TimeEntryWithDetails? = null,
    next: TimeEntryWithDetails? = null,
    onQuickTask: ((projectId: Long, title: String, onCreated: (Long) -> Unit) -> Unit)? = null,
    /** Create a project from inside the dialog ("+ Add new project…"); the new project gets selected. */
    onQuickProject: ((code: String, name: String, client: String, colorHex: String, budgetHours: Double, isProductive: Boolean, onCreated: (Long) -> Unit) -> Unit)? = null,
    /** Show and pick only the time of day (the entry belongs to a known day). */
    timeOnly: Boolean = false,
    /** Suggested start/end for a new entry (e.g. end of the previous activity until now). */
    defaultStart: Long? = null,
    defaultEnd: Long? = null
) {
    val t = strings
    val isRunning = entry?.isRunning == true
    val defaultEnd = defaultEnd ?: initialDayStart?.let { it + 17 * 3600_000L } ?: currentTimeMillis()
    var projectId by remember { mutableStateOf(entry?.projectId) }
    var taskId by remember { mutableStateOf(entry?.taskId) }
    var showQuickTask by remember { mutableStateOf(false) }
    var showQuickProject by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(entry?.description ?: "") }
    var start by remember { mutableStateOf(entry?.startTime ?: defaultStart?.takeIf { it < defaultEnd } ?: (defaultEnd - 3600_000L)) }
    var end by remember { mutableStateOf<Long?>(if (isRunning) null else (entry?.endTime ?: defaultEnd)) }
    // null = not decided yet: a neighbour is moved when the entry now overlaps it, left alone when a gap opens.
    var movePrevious by remember { mutableStateOf<Boolean?>(null) }
    var moveNext by remember { mutableStateOf<Boolean?>(null) }

    val projectTasks = remember(projectId, tasks) { tasks.filter { it.projectId == projectId && it.status != "DONE" } }
    val endValue = end
    val invalidRange = !isRunning && endValue != null && endValue <= start

    // Neighbour handling only matters once the boundary next to it moved.
    val previousTouched = entry != null && previous?.endTime != null && start != entry.startTime
    val nextTouched = entry != null && next != null && endValue != null && endValue != entry.endTime
    val previousDelta = if (previousTouched) start - previous.endTime else 0L
    val nextDelta = if (nextTouched) next.startTime - endValue else 0L
    val canMovePrevious = previousTouched && start > previous!!.startTime
    val canMoveNext = nextTouched && (next!!.endTime == null || endValue!! < next.endTime)
    val effectiveMovePrevious = canMovePrevious && (movePrevious ?: (previousDelta < 0))
    val effectiveMoveNext = canMoveNext && (moveNext ?: (nextDelta < 0))

    FormDialog(
        title = if (entry == null) t.logActivity else t.editActivity,
        onDismiss = onDismiss,
        onSave = { onSave(projectId, taskId, description.trim(), start, end, NeighbourAdjustment(previous = effectiveMovePrevious, next = effectiveMoveNext)) },
        saveEnabled = !invalidRange && (isRunning || end != null),
        saveTestTag = "save_entry_button",
        modifier = Modifier.testTag("entry_form_dialog")
    ) {
        ProjectDropdown(
            projects = projects,
            selectedProjectId = projectId,
            onSelect = {
                projectId = it
                taskId = null
            },
            testTag = "entry_project_dropdown",
            onAddProject = if (onQuickProject != null) ({ showQuickProject = true }) else null
        )

        val currentProjectId = projectId
        if (currentProjectId != null) {
            Spacer(modifier = Modifier.height(10.dp))
            TaskDropdown(
                tasks = projectTasks,
                selectedTaskId = taskId,
                onSelect = { taskId = it },
                onAddTask = if (onQuickTask != null) ({ showQuickTask = true }) else null
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(t.noteOptional) },
            placeholder = { Text(t.notePlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("entry_description_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DateTimeField(label = t.startLabel, millis = start, placeholder = t.pickStart, onPick = { start = it; movePrevious = null }, timeOnly = timeOnly, modifier = Modifier.weight(1f))
            if (!isRunning) {
                DateTimeField(label = t.endLabel, millis = end, placeholder = t.pickEnd, onPick = { end = it; moveNext = null }, timeOnly = timeOnly, modifier = Modifier.weight(1f))
            }
        }
        if (previousTouched && !invalidRange) {
            NeighbourChoice(
                heading = t.previousActivityEnds(previous!!.shortLabel(t), DateFormats.hourMinute(previous.endTime!!)),
                deltaMillis = previousDelta,
                boundary = start,
                canMove = canMovePrevious,
                move = effectiveMovePrevious,
                onMove = { movePrevious = it },
                testTag = "neighbour_previous"
            )
        }
        if (nextTouched && !invalidRange) {
            NeighbourChoice(
                heading = t.nextActivityStarts(next!!.shortLabel(t), DateFormats.hourMinute(next.startTime)),
                deltaMillis = nextDelta,
                boundary = endValue!!,
                canMove = canMoveNext,
                move = effectiveMoveNext,
                onMove = { moveNext = it },
                testTag = "neighbour_next"
            )
        }
        if (!isRunning) {
            // Quick durations: set the end relative to the start with one tap.
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 240 to "4h").forEach { (minutes, label) ->
                    AssistChip(
                        onClick = { end = start + minutes * 60_000L; moveNext = null },
                        label = { Text(t.durationChip(label), fontSize = 12.sp) },
                        modifier = Modifier.testTag("duration_chip_$minutes")
                    )
                }
                AssistChip(
                    onClick = { end = currentTimeMillis(); moveNext = null },
                    label = { Text(t.untilNow, fontSize = 12.sp) },
                    modifier = Modifier.testTag("duration_chip_now")
                )
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(t.stillRunning, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (invalidRange) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(t.endAfterStart, fontSize = 12.sp, color = RoseUrgent)
        } else if (!isRunning && endValue != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                t.duration(TimeFormat.hoursMinutes((endValue - start) / 1000)),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showQuickProject && onQuickProject != null) {
        ProjectFormDialog(
            project = null,
            onDismiss = { showQuickProject = false },
            onSave = { code, name, client, colorHex, budgetHours, _, productive ->
                onQuickProject(code, name, client, colorHex, budgetHours, productive) { id ->
                    projectId = id
                    taskId = null
                }
                showQuickProject = false
            }
        )
    }

    val quickProjectId = projectId
    if (showQuickTask && quickProjectId != null && onQuickTask != null) {
        QuickTaskDialog(
            onDismiss = { showQuickTask = false },
            onSave = { title ->
                onQuickTask(quickProjectId, title) { id -> taskId = id }
                showQuickTask = false
            }
        )
    }

}

/** "0002 · VEGA / General" for the neighbour headings. */
private fun TimeEntryWithDetails.shortLabel(t: com.suw1labs.worktracker.ui.i18n.AppStrings): String =
    (projectCode ?: t.noProject) + " / " + (taskTitle ?: t.noSpecificTask)

/**
 * What happens to the activity next to a moved boundary. [deltaMillis] > 0 means a gap opened
 * between the two, < 0 that they overlap now. Moving the neighbour puts its boundary at [boundary].
 */
@Composable
private fun NeighbourChoice(
    heading: String,
    deltaMillis: Long,
    boundary: Long,
    canMove: Boolean,
    move: Boolean,
    onMove: (Boolean) -> Unit,
    testTag: String
) {
    val t = strings
    val amount = TimeFormat.hoursMinutes(kotlin.math.abs(deltaMillis) / 1000L)
    val isGap = deltaMillis > 0
    Spacer(modifier = Modifier.height(10.dp))
    Column(modifier = Modifier.fillMaxWidth().testTag(testTag)) {
        Text(heading, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (isGap) t.gapBetween(amount) else t.overlapBetween(amount),
            fontSize = 12.sp,
            color = if (isGap || move) MaterialTheme.colorScheme.onSurfaceVariant else AmberWarning
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = move,
                enabled = canMove,
                onClick = { onMove(true) },
                label = { Text(t.moveNeighbourTo(DateFormats.hourMinute(boundary)), fontSize = 12.sp) },
                modifier = Modifier.testTag("${testTag}_move")
            )
            FilterChip(
                selected = !move,
                onClick = { onMove(false) },
                label = { Text(if (isGap) t.leaveGap else t.keepOverlap, fontSize = 12.sp) },
                modifier = Modifier.testTag("${testTag}_keep")
            )
        }
        if (!canMove) {
            Text(t.neighbourWouldBeEmpty, fontSize = 11.sp, color = AmberWarning)
        }
    }
}

/**
 * Add or edit an attendance (clock-in) session.
 */
@Composable
fun SessionFormDialog(
    session: AttendanceSession?,
    onDismiss: () -> Unit,
    onSave: (clockIn: Long, clockOut: Long?, reason: ClockOutReason?) -> Unit,
    initialDayStart: Long? = null
) {
    val t = strings
    val isOpen = session?.isOpen == true
    val defaultIn = initialDayStart?.let { it + 8 * 3600_000L } ?: (currentTimeMillis() - 8 * 3600_000L)
    val defaultOut = initialDayStart?.let { it + 17 * 3600_000L } ?: currentTimeMillis()
    var clockIn by remember { mutableStateOf(session?.clockIn ?: defaultIn) }
    var clockOut by remember { mutableStateOf<Long?>(if (isOpen) null else (session?.clockOut ?: defaultOut)) }
    var reason by remember { mutableStateOf(ClockOutReason.fromName(session?.clockOutReason) ?: ClockOutReason.END_OF_DAY) }

    val outValue = clockOut
    val invalidRange = !isOpen && outValue != null && outValue <= clockIn

    FormDialog(
        title = if (session == null) t.addPeriod else t.editPeriod,
        onDismiss = onDismiss,
        onSave = { onSave(clockIn, clockOut, if (isOpen) null else reason) },
        saveEnabled = !invalidRange && (isOpen || clockOut != null),
        saveTestTag = "save_session_button",
        modifier = Modifier.testTag("session_form_dialog")
    ) {
        DateTimeField(label = t.clockInLabel, millis = clockIn, placeholder = t.pickTime, onPick = { clockIn = it })
        Spacer(modifier = Modifier.height(10.dp))
        if (isOpen) {
            Text(t.stillClockedIn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            DateTimeField(label = t.clockOutLabel, millis = clockOut, placeholder = t.pickTime, onPick = { clockOut = it })
            Spacer(modifier = Modifier.height(10.dp))
            LabeledDropdown(
                label = t.clockOutReason,
                selectedText = t.reasonLabel(reason),
                options = ClockOutReason.entries,
                optionText = { t.reasonLabel(it) },
                onSelect = { reason = it }
            )
        }
        if (invalidRange) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(t.clockOutAfterIn, fontSize = 12.sp, color = RoseUrgent)
        } else if (!isOpen && outValue != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(t.duration(TimeFormat.hoursMinutes((outValue - clockIn) / 1000)), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Minimal dialog to create a task by title (e.g. "Meeting" on the Unproductive project). */
@Composable
fun QuickTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String) -> Unit
) {
    val t = strings
    var title by remember { mutableStateOf("") }
    FormDialog(
        title = t.newTaskTitle,
        onDismiss = onDismiss,
        onSave = { if (title.isNotBlank()) onSave(title.trim()) },
        saveEnabled = title.isNotBlank(),
        saveTestTag = "save_quick_task_button",
        modifier = Modifier.testTag("quick_task_dialog")
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(t.taskTitle) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("quick_task_title_input")
        )
    }
}

package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.components.TaskDropdown
import com.suw1labs.worktracker.ui.components.DateTimePickerDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.ProjectDropdown
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
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
 */
@Composable
fun EntryFormDialog(
    entry: TimeEntryWithDetails?,
    projects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    onDismiss: () -> Unit,
    onSave: (projectId: Long?, taskId: Long?, description: String, start: Long, end: Long?) -> Unit,
    initialDayStart: Long? = null,
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

    val projectTasks = remember(projectId, tasks) { tasks.filter { it.projectId == projectId && it.status != "DONE" } }
    val endValue = end
    val invalidRange = !isRunning && endValue != null && endValue <= start

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("entry_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (entry == null) t.logActivity else t.editActivity,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

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
                    modifier = Modifier.fillMaxWidth().testTag("entry_description_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                DateTimeField(label = t.startLabel, millis = start, placeholder = t.pickStart, onPick = { start = it }, timeOnly = timeOnly)
                if (!isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DateTimeField(label = t.endLabel, millis = end, placeholder = t.pickEnd, onPick = { end = it }, timeOnly = timeOnly)
                    // Quick durations: set the end relative to the start with one tap.
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 240 to "4h").forEach { (minutes, label) ->
                            AssistChip(
                                onClick = { end = start + minutes * 60_000L },
                                label = { Text(t.durationChip(label), fontSize = 12.sp) },
                                modifier = Modifier.testTag("duration_chip_$minutes")
                            )
                        }
                        AssistChip(
                            onClick = { end = currentTimeMillis() },
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { onSave(projectId, taskId, description, start, end) },
                        enabled = !invalidRange && (isRunning || end != null),
                        modifier = Modifier.weight(1f).testTag("save_entry_button")
                    ) { Text(t.save) }
                }
            }
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("session_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (session == null) t.addPeriod else t.editPeriod,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { onSave(clockIn, clockOut, if (isOpen) null else reason) },
                        enabled = !invalidRange && (isOpen || clockOut != null),
                        modifier = Modifier.weight(1f).testTag("save_session_button")
                    ) { Text(t.save) }
                }
            }
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("quick_task_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(t.newTaskTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t.taskTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_task_title_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_quick_task_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

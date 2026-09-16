package com.suw1labs.worktracker.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.ui.i18n.emoji
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.ui.components.TaskDropdown
import com.suw1labs.worktracker.ui.components.DeadlineUrgencyBadge
import com.suw1labs.worktracker.ui.components.EmptyStateCard
import com.suw1labs.worktracker.ui.components.ProjectDropdown
import com.suw1labs.worktracker.ui.theme.AmberWarning
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.parseHexColor
import com.suw1labs.worktracker.util.projectColor
import com.suw1labs.worktracker.ui.i18n.strings

@Composable
fun TodayScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier,
) {
    val t = strings
    val now by viewModel.now.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    val runningEntry by viewModel.runningEntry.collectAsState()
    val todayReport by viewModel.todayReport.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val urgentTasks by viewModel.urgentTasks.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showActivitySelector by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimeEntryWithDetails?>(null) }
    var showAbsenceDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var newlyCreatedProjectId by remember { mutableStateOf<Long?>(null) }

    val isClockedIn = openSession != null

    // Hide the selector again once an activity started.
    LaunchedEffect(runningEntry?.id) { if (runningEntry != null) showActivitySelector = false }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // --- URGENT DEADLINES BANNER ---
        if (urgentTasks.isNotEmpty()) {
            item {
                val topUrgent = urgentTasks.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseUrgent.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("urgent_deadlines_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = RoseUrgent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t.deadlineAlerts(urgentTasks.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoseUrgent
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topUrgent.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${topUrgent.projectCode} · ${topUrgent.projectName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DeadlineUrgencyBadge(deadlineTimestamp = topUrgent.deadlineTimestamp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.startActivityFromTask(topUrgent) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseUrgent),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t.workOnIt, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.snoozeTask(topUrgent.id, 24) },
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) { Text(t.snooze, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        // --- ATTENDANCE CARD ---
        item {
            AttendanceCard(
                isClockedIn = isClockedIn,
                clockedInSince = openSession?.clockIn,
                attendanceSeconds = todayReport.attendanceSeconds,
                onClockIn = { viewModel.clockIn() },
                onClockOut = { viewModel.clockOut() }
            )
        }

        // --- CURRENT ACTIVITY CARD ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("activity_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val running = runningEntry
                    if (running != null && !showActivitySelector) {
                        // Total time on this project + task today, across all sessions (lunch, breaks, …).
                        val todayTaskSeconds = todayEntries
                            .filter { it.projectId == running.projectId && it.taskId == running.taskId }
                            .sumOf { it.durationSeconds(now) }
                        RunningActivity(
                            entry = running,
                            now = now,
                            todayTaskSeconds = todayTaskSeconds,
                            onSwitch = { showActivitySelector = true },
                            onStop = { viewModel.stopActivity() },
                            onEdit = { editingEntry = running },
                            onNoteChange = { viewModel.updateEntryNote(running, it) }
                        )
                    } else {
                        ActivitySelector(
                            projects = activeProjects,
                            tasks = allTasks,
                            isClockedIn = isClockedIn,
                            isSwitching = running != null,
                            preselectProjectId = newlyCreatedProjectId,
                            onAddProject = { showAddProjectDialog = true },
                            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
                            onCancel = { showActivitySelector = false },
                            onStart = { projectId, taskId -> viewModel.startActivity(projectId, taskId) }
                        )
                    }
                }
            }
        }

        // --- COMPLIANCE WARNINGS (break rules, forgot to clock out) ---
        if (todayReport.warnings.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("compliance_warning_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        todayReport.warnings.forEach { warning ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(warning.message, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // --- TODAY SUMMARY ---
        item {
            DaySummaryCard(
                report = todayReport,
                onBookAbsence = { showAbsenceDialog = true }
            )
        }

        // --- ACTIVITIES LIST ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(t.activities, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        t.activitiesSubtitle(todayEntries.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showManualEntry = true },
                    modifier = Modifier.testTag("manual_time_entry_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t.manualEntry, fontSize = 12.sp)
                }
            }
        }

        if (todayEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Work,
                    title = t.noActivitiesTitle,
                    subtitle = t.noActivitiesSubtitle
                )
            }
        } else {
            items(todayEntries, key = { it.id }) { entry ->
                TimeEntryRowCard(
                    entry = entry,
                    now = now,
                    onEdit = { editingEntry = entry },
                    onDelete = { viewModel.deleteTimeEntry(entry.id) },
                    onContinue = if (entry.isRunning) null else ({ viewModel.continueEntry(entry) }),
                    isSomethingRunning = runningEntry != null
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showManualEntry) {
        EntryFormDialog(
            entry = null,
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { showManualEntry = false },
            onSave = { projectId, taskId, description, start, end ->
                if (end != null) viewModel.addManualEntry(projectId, taskId, description, start, end)
                showManualEntry = false
            },
            initialDayStart = todayReport.range.start,
            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
            onQuickProject = { code, name, client, color, budget, productive, onCreated -> viewModel.addProject(code, name, client, color, budget, productive, onCreated) },
            timeOnly = true
        )
    }

    editingEntry?.let { entry ->
        EntryFormDialog(
            entry = entry,
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { editingEntry = null },
            onSave = { projectId, taskId, description, start, end ->
                viewModel.updateEntry(
                    entry.toEntity().copy(
                        projectId = projectId,
                        taskId = taskId,
                        description = description,
                        startTime = start,
                        endTime = end
                    )
                )
                editingEntry = null
            },
            initialDayStart = todayReport.range.start,
            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
            onQuickProject = { code, name, client, color, budget, productive, onCreated -> viewModel.addProject(code, name, client, color, budget, productive, onCreated) },
            timeOnly = true
        )
    }

    if (showAddProjectDialog) {
        ProjectFormDialog(
            project = null,
            onDismiss = { showAddProjectDialog = false },
            onSave = { code, name, client, colorHex, budgetHours, _, isProductive ->
                viewModel.addProject(code, name, client, colorHex, budgetHours, isProductive) { id -> newlyCreatedProjectId = id }
                showAddProjectDialog = false
            }
        )
    }

    if (showAbsenceDialog) {
        val today = todayReport.days.firstOrNull()
        DayRecordDialog(
            record = today?.absence,
            initialDayStart = todayReport.range.start,
            defaultHours = settings.dailyTargetHours,
            onDismiss = { showAbsenceDialog = false },
            onSave = { dayStart, type, hours, label, note ->
                viewModel.saveDayRecord(dayStart, type, hours, label, note)
                showAbsenceDialog = false
            }
        )
    }

}

@Composable
private fun AttendanceCard(
    isClockedIn: Boolean,
    clockedInSince: Long?,
    attendanceSeconds: Long,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    val t = strings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isClockedIn) 1.25f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale",
    )
    val statusColor = if (isClockedIn) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("attendance_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).scale(pulseScale).clip(CircleShape).background(statusColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isClockedIn) t.clockedInSince(DateFormats.hourMinute(clockedInSince ?: 0L)) else t.clockedOut,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = TimeFormat.hms(attendanceSeconds),
                fontFamily = FontFamily.Monospace,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("attendance_total_text")
            )
            Text(
                text = t.clockedInToday,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!isClockedIn) {
                Button(
                    onClick = onClockIn,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clock_in_button")
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t.clockIn, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Button(
                    onClick = onClockOut,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseUrgent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clock_out_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t.clockOut, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun RunningActivity(
    entry: TimeEntryWithDetails,
    now: Long,
    todayTaskSeconds: Long,
    onSwitch: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onNoteChange: (String) -> Unit
) {
    val t = strings
    val color = entry.projectColor?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.tertiary
    var showNoteDialog by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreen))
        Spacer(modifier = Modifier.width(8.dp))
        Text(t.currentActivity, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = EmeraldGreen)
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            // Big number: time on this task today (all sessions). Small: the running session only.
            Text(
                text = TimeFormat.hms(todayTaskSeconds),
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("activity_today_total_text")
            )
            Text(t.todayOnThisTask, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${t.thisSession} ${TimeFormat.hms(entry.durationSeconds(now))}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("activity_elapsed_text")
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            val unassigned = entry.isUnassigned
            Text(
                text = entry.projectCode?.let { "$it · ${entry.projectName}" } ?: if (unassigned) "⚠ ${t.unassignedProject}" else t.noProject,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (unassigned) AmberWarning else MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(name = entry.taskTitle ?: t.noSpecificTask, productive = entry.isProductive)
            }
            if (entry.description.isNotBlank()) {
                Text(entry.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).testTag("running_note_text"))
            }
            Text(t.since(DateFormats.hourMinute(entry.startTime)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        // Note icon: add or edit the optional note in a small dialog.
        IconButton(onClick = { showNoteDialog = true }, modifier = Modifier.size(32.dp).testTag("running_note_button")) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = if (entry.description.isBlank()) t.addNote else t.editNote,
                tint = if (entry.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = t.edit, modifier = Modifier.size(16.dp))
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    if (showNoteDialog) {
        var noteText by remember { mutableStateOf(entry.description) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(if (entry.description.isBlank()) t.addNote else t.editNote) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(t.noteHint) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("running_note_input")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNoteChange(noteText.trim())
                    showNoteDialog = false
                }) { Text(t.save) }
            },
            dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text(t.cancel) } }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onSwitch, modifier = Modifier.weight(1f).height(46.dp).testTag("switch_activity_button")) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(t.switchShort)
        }
        OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).height(46.dp).testTag("stop_activity_button")) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(t.stop)
        }
    }
}

@Composable
private fun ActivitySelector(
    projects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    isClockedIn: Boolean,
    isSwitching: Boolean,
    preselectProjectId: Long?,
    onAddProject: () -> Unit,
    onQuickTask: (projectId: Long, title: String, onCreated: (Long) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onStart: (projectId: Long?, taskId: Long?) -> Unit
) {
    val t = strings
    var projectId by remember { mutableStateOf<Long?>(projects.firstOrNull { it.isProductive }?.id) }
    var taskId by remember { mutableStateOf<Long?>(null) }
    var showQuickTask by remember { mutableStateOf(false) }

    // Default to the first productive project once projects are loaded.
    LaunchedEffect(projects) { if (projectId == null && projects.isNotEmpty()) projectId = projects.firstOrNull { it.isProductive }?.id ?: projects.first().id }
    // A project created from "+ Add new project…" becomes the selection once it is loaded.
    LaunchedEffect(preselectProjectId, projects) {
        if (preselectProjectId != null && projects.any { it.id == preselectProjectId }) {
            projectId = preselectProjectId
            taskId = null
        }
    }

    val projectTasks = remember(projectId, tasks) { tasks.filter { it.projectId == projectId && it.status != "DONE" } }

    Text(
        text = if (isSwitching) t.switchActivity else t.whatWorkingOn,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    ProjectDropdown(
        projects = projects,
        selectedProjectId = projectId,
        onSelect = {
            projectId = it
            taskId = null
        },
        testTag = "activity_project_dropdown",
        onAddProject = onAddProject
    )
    val currentProjectId = projectId
    if (currentProjectId != null) {
        Spacer(modifier = Modifier.height(8.dp))
        TaskDropdown(
            tasks = projectTasks,
            selectedTaskId = taskId,
            onSelect = { taskId = it },
            testTag = "activity_task_dropdown",
            onAddTask = { showQuickTask = true }
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (isSwitching) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(48.dp)) { Text(t.cancel) }
        }
        Button(
            onClick = { onStart(projectId, taskId) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(if (isSwitching) 1.5f else 1f).height(48.dp).testTag("start_activity_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSwitching) t.switchShort else t.start, fontWeight = FontWeight.Bold)
        }
    }
    if (!isClockedIn) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(t.autoClockInHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showQuickTask && currentProjectId != null) {
        QuickTaskDialog(
            onDismiss = { showQuickTask = false },
            onSave = { title ->
                onQuickTask(currentProjectId, title) { id -> taskId = id }
                showQuickTask = false
            }
        )
    }
}

@Composable
fun DaySummaryCard(
    report: PeriodReport,
    onBookAbsence: () -> Unit
) {
    val t = strings
    val absence: DayRecord? = report.days.firstOrNull()?.absence
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("day_summary_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(t.tabToday, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBookAbsence, modifier = Modifier.testTag("book_absence_button")) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (absence == null) t.absence else t.editAbsence, fontSize = 12.sp)
                }
            }
            if (absence != null) {
                Surface(color = AmberWarning.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${absence.absenceType.emoji()} ${if (absence.absenceType == AbsenceType.OTHER) absence.displayLabel else t.absenceLabel(absence.absenceType)} · ${TimeFormat.hoursMinutes((absence.hours * 3600).toLong())}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            SummaryRow(t.clockedIn, report.attendanceSeconds, MaterialTheme.colorScheme.onSurface)
            SummaryRow(t.projectWork, report.productiveSeconds, EmeraldGreen)
            if (report.unassignedProductiveSeconds > 0) {
                SummaryRow("↳ ${t.unassignedProject}", report.unassignedProductiveSeconds, AmberWarning)
            }
            SummaryRow(t.unproductive, report.unproductiveSeconds, AmberWarning)
            SignedRow(t.overtimeToday, report.overtimeSeconds)
        }
    }
}

@Composable
private fun SignedRow(label: String, seconds: Long) {
    val color = when {
        seconds > 0 -> EmeraldGreen
        seconds < 0 -> RoseUrgent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val abs = kotlin.math.abs(seconds)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            (if (seconds < 0) "-" else if (seconds > 0) "+" else "") + TimeFormat.hoursMinutes(abs),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SummaryRow(label: String, seconds: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            TimeFormat.hoursMinutes(seconds),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryChip(name: String?, productive: Boolean?) {
    val color = if (productive == true) MaterialTheme.colorScheme.primary else AmberWarning
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = name ?: strings.noSpecificTask,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TimeEntryRowCard(
    entry: TimeEntryWithDetails,
    now: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onContinue: (() -> Unit)? = null,
    isSomethingRunning: Boolean = false
) {
    val t = strings
    val projColor = entry.projectColor?.let { projectColor(it) } ?: MaterialTheme.colorScheme.tertiary
    val seconds = entry.durationSeconds(now)
    var showActions by remember { mutableStateOf(false) }
    val switchLabel = if (isSomethingRunning) t.switchToThis else t.continueThis

    // Tapping the card opens the actions (switch / continue, edit, delete).
    if (showActions) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showActions = false },
            title = { Text(entry.projectCode?.let { "$it · ${entry.projectName}" } ?: t.noProject, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(entry.taskTitle ?: t.noSpecificTask, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (entry.description.isNotBlank()) Text(entry.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (onContinue != null) {
                        Button(onClick = { showActions = false; onContinue() }, modifier = Modifier.fillMaxWidth().testTag("entry_action_switch")) {
                            Icon(if (isSomethingRunning) Icons.Default.SwapHoriz else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(switchLabel)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    OutlinedButton(onClick = { showActions = false; onEdit() }, modifier = Modifier.fillMaxWidth().testTag("entry_action_edit")) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t.edit)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { showActions = false; onDelete() }, modifier = Modifier.fillMaxWidth().testTag("entry_action_delete")) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t.delete, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showActions = false }) { Text(t.cancel) } }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { showActions = true },
        modifier = Modifier.fillMaxWidth().testTag("time_entry_card_${entry.id}")
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(projColor))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val unassigned = entry.isUnassigned
                Text(
                    text = entry.projectCode?.let { "$it · ${entry.projectName}" } ?: if (unassigned) "⚠ ${t.unassignedProject}" else t.noProject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (unassigned) AmberWarning else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(name = entry.taskTitle ?: t.noSpecificTask, productive = entry.isProductive)
                    if (entry.description.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(entry.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Text(
                    text = "${DateFormats.hourMinute(entry.startTime)} – ${entry.endTime?.let { DateFormats.hourMinute(it) } ?: t.running}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TimeFormat.hoursMinutes(seconds),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    if (onContinue != null) {
                        IconButton(onClick = onContinue, modifier = Modifier.size(32.dp).testTag("continue_entry_${entry.id}")) {
                            Icon(
                                if (isSomethingRunning) Icons.Default.SwapHoriz else Icons.Default.PlayArrow,
                                contentDescription = switchLabel,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = t.edit, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = t.delete, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

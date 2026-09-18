package com.suw1labs.worktracker.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.ui.i18n.emoji
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.DayGaps
import com.suw1labs.worktracker.data.report.EntryNeighbours
import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.data.report.UnassignedGap
import com.suw1labs.worktracker.data.report.WarningKind
import com.suw1labs.worktracker.ui.components.TaskDropdown
import com.suw1labs.worktracker.ui.components.DeadlineUrgencyBadge
import com.suw1labs.worktracker.ui.components.EmptyStateCard
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
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
import kotlinx.coroutines.launch

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
    val todaySessions by viewModel.todaySessions.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val urgentTasks by viewModel.urgentTasks.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showActivitySelector by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimeEntryWithDetails?>(null) }
    var assigningGap by remember { mutableStateOf<UnassignedGap?>(null) }
    var showAbsenceDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var newlyCreatedProjectId by remember { mutableStateOf<Long?>(null) }

    val isClockedIn = openSession != null
    val snackbarHost = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    // Activities are deleted immediately; the snackbar offers to put them back.
    fun deleteWithUndo(entry: TimeEntryWithDetails) {
        viewModel.deleteTimeEntry(entry.id)
        scope.launch {
            val result = snackbarHost.showSnackbar(message = t.activityDeleted, actionLabel = t.undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
        }
    }

    // Hide the selector again once an activity started.
    LaunchedEffect(runningEntry?.id) { if (runningEntry != null) showActivitySelector = false }

    // The day as one timeline, newest first: activities, the clock-in / clock-out moments and the
    // clocked-in stretches nothing was logged for (recomputed once a minute so an open gap keeps growing).
    val nowMinute = now / 60_000L
    val timeline = remember(todayEntries, todaySessions, nowMinute) { buildTimeline(todayEntries, todaySessions, now) }

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
                // While clocked out, the last period of today tells whether this is a lunch / break.
                lastClosedSession = if (isClockedIn) null else todaySessions.filter { it.clockOut != null }.maxByOrNull { it.clockOut!! },
                now = now,
                onClockIn = { viewModel.clockIn() },
                onClockOut = { reason -> viewModel.clockOut(reason) }
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
                    } else if (!isClockedIn) {
                        // The day starts with a clock-in; the task is chosen afterwards.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(t.clockInFirst, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("clock_in_first_hint"))
                        }
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
                                Text(t.warning(warning), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            }
                            if (warning.kind == WarningKind.STILL_CLOCKED_IN_PAST_DAY) {
                                // One tap closes the forgotten period at the most plausible time.
                                val at = viewModel.forgottenClockOutTimeFor(warning.dayStart)
                                if (at != null) {
                                    FilledTonalButton(
                                        onClick = { viewModel.fixForgottenClockOut(warning.dayStart) },
                                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                                        modifier = Modifier.padding(start = 26.dp, bottom = 4.dp).testTag("fix_clock_out_${warning.dayStart}")
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(t.clockOutAt(DateFormats.hourMinute(at)), fontSize = 12.sp)
                                    }
                                }
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(t.activities, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        t.activitiesSubtitle(todayEntries.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showManualEntry = true },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("manual_time_entry_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t.add, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        if (timeline.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Work,
                    title = t.noActivitiesTitle,
                    subtitle = t.noActivitiesSubtitle
                )
            }
        } else {
            // One block so the rail between the dots is continuous.
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    timeline.forEachIndexed { index, item ->
                        when (item) {
                            is TimelineItem.Activity -> TimelineRow(
                                dotColor = item.entry.projectColor?.let { projectColor(it) } ?: MaterialTheme.colorScheme.tertiary,
                                dotY = 30.dp,
                                lineAbove = index > 0,
                                lineBelow = index < timeline.lastIndex
                            ) {
                                TimeEntryRowCard(
                                    entry = item.entry,
                                    now = now,
                                    onEdit = { editingEntry = item.entry },
                                    onDelete = { deleteWithUndo(item.entry) },
                                    onContinue = if (item.entry.isRunning) null else ({ viewModel.continueEntry(item.entry) }),
                                    isSomethingRunning = runningEntry != null,
                                    showColorBar = false
                                )
                            }
                            is TimelineItem.Gap -> TimelineRow(
                                dotColor = MaterialTheme.colorScheme.outline,
                                dotY = 24.dp,
                                lineAbove = index > 0,
                                lineBelow = index < timeline.lastIndex
                            ) { GapRow(gap = item.gap, now = now, onAssign = { assigningGap = item.gap }) }
                            is TimelineItem.Attendance -> TimelineRow(
                                dotColor = when {
                                    item.isClockIn -> EmeraldGreen
                                    ClockOutReason.fromName(item.session.clockOutReason)?.let { it == ClockOutReason.LUNCH || it == ClockOutReason.BREAK } == true -> AmberWarning
                                    else -> RoseUrgent
                                },
                                dotY = 16.dp,
                                // The rail is broken while clocked out: nothing below a clock-in
                                // (newest first, so the pause is below it) and nothing above a clock-out.
                                lineAbove = index > 0 && item.isClockIn,
                                lineBelow = index < timeline.lastIndex && !item.isClockIn
                            ) { AttendanceEventRow(item, now) }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showManualEntry) {
        // Default: from the end of the last activity today (or the clock-in) until now.
        val lastEnd = todayEntries.filter { !it.isRunning }.maxOfOrNull { it.endTime ?: 0L } ?: openSession?.clockIn
        EntryFormDialog(
            entry = null,
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { showManualEntry = false },
            onSave = { projectId, taskId, description, start, end, _ ->
                if (end != null) viewModel.addManualEntry(projectId, taskId, description, start, end)
                showManualEntry = false
            },
            initialDayStart = todayReport.range.start,
            defaultStart = lastEnd,
            defaultEnd = now,
            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
            onQuickProject = { code, name, client, color, budget, productive, onCreated -> viewModel.addProject(code, name, client, color, budget, productive, onCreated) },
            timeOnly = true
        )
    }

    assigningGap?.let { gap ->
        // Fill a clocked-in stretch that has no activity yet; the times are preset to the gap.
        EntryFormDialog(
            entry = null,
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { assigningGap = null },
            onSave = { projectId, taskId, description, start, end, _ ->
                if (end != null) viewModel.addManualEntry(projectId, taskId, description, start, end)
                assigningGap = null
            },
            initialDayStart = todayReport.range.start,
            defaultStart = gap.start,
            defaultEnd = gap.end ?: now,
            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
            onQuickProject = { code, name, client, color, budget, productive, onCreated -> viewModel.addProject(code, name, client, color, budget, productive, onCreated) },
            timeOnly = true
        )
    }

    editingEntry?.let { entry ->
        val previous = remember(entry, todayEntries) { EntryNeighbours.previousOf(entry, todayEntries) }
        val next = remember(entry, todayEntries) { EntryNeighbours.nextOf(entry, todayEntries) }
        EntryFormDialog(
            entry = entry,
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { editingEntry = null },
            onSave = { projectId, taskId, description, start, end, adjust ->
                viewModel.updateEntry(
                    entry.toEntity().copy(
                        projectId = projectId,
                        taskId = taskId,
                        description = description,
                        startTime = start,
                        endTime = end
                    ),
                    movedNeighbours = EntryNeighbours.adjusted(previous, next, start, end, adjust).map { it.toEntity() }
                )
                editingEntry = null
            },
            initialDayStart = todayReport.range.start,
            previous = previous,
            next = next,
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

/** Symbol for a tagged pause: a meal for lunch, a coffee cup for a break, otherwise the door. */
fun ClockOutReason.icon(): ImageVector = when (this) {
    ClockOutReason.LUNCH -> Icons.Default.Restaurant
    ClockOutReason.BREAK -> Icons.Default.Coffee
    else -> Icons.Default.Logout
}

/** One row of the day's timeline. */
sealed interface TimelineItem {
    val time: Long
    val key: String

    data class Activity(val entry: TimeEntryWithDetails) : TimelineItem {
        override val time: Long get() = entry.startTime
        override val key: String get() = "entry-${entry.id}"
    }

    /** Clocked-in time with no activity; tapping it logs one for exactly that stretch. */
    data class Gap(val gap: UnassignedGap) : TimelineItem {
        override val time: Long get() = gap.start
        override val key: String get() = "gap-${gap.start}"
    }

    data class Attendance(
        val session: AttendanceSession,
        override val time: Long,
        val isClockIn: Boolean,
        /** For a clock-out: when the next period of the day started, or null while still out. */
        val nextClockIn: Long? = null
    ) : TimelineItem {
        override val key: String get() = "session-${session.id}-${if (isClockIn) "in" else "out"}"

        /** Length of the pause that started with this clock-out (ongoing until [now] when not back yet). */
        fun pauseSeconds(now: Long): Long? = if (isClockIn) null else (((nextClockIn ?: now) - time) / 1000L).coerceAtLeast(0L)
    }
}

/**
 * Activities, unassigned gaps and clock-in / clock-out events merged, newest first; an activity
 * (or gap) that started at the same moment as a clock-in sits above it.
 */
fun buildTimeline(entries: List<TimeEntryWithDetails>, sessions: List<AttendanceSession>, now: Long): List<TimelineItem> {
    val items = mutableListOf<TimelineItem>()
    entries.mapTo(items) { TimelineItem.Activity(it) }
    DayGaps.compute(entries, sessions, now).mapTo(items) { TimelineItem.Gap(it) }
    val ordered = sessions.sortedBy { it.clockIn }
    ordered.forEachIndexed { index, session ->
        items += TimelineItem.Attendance(session, session.clockIn, isClockIn = true)
        session.clockOut?.let { out ->
            items += TimelineItem.Attendance(session, out, isClockIn = false, nextClockIn = ordered.getOrNull(index + 1)?.clockIn)
        }
    }
    return items.sortedWith(compareByDescending<TimelineItem> { it.time }.thenBy { it is TimelineItem.Attendance && it.isClockIn })
}

/**
 * One step of the day's timeline: a dot on a vertical rail and the content to its right. The rail
 * segments above and below are drawn only when asked, so gaps (clocked-out time) stay visible.
 * [dotY] is where the dot sits so it lines up with the content's first line.
 */
@Composable
private fun TimelineRow(
    dotColor: Color,
    dotY: androidx.compose.ui.unit.Dp,
    lineAbove: Boolean,
    lineBelow: Boolean,
    content: @Composable () -> Unit
) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    val ringColor = MaterialTheme.colorScheme.background
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(modifier = Modifier.width(26.dp).fillMaxHeight()) {
            val cx = size.width / 2f
            val y = dotY.toPx()
            val stroke = 2.dp.toPx()
            if (lineAbove) drawLine(railColor, Offset(cx, 0f), Offset(cx, y), stroke)
            if (lineBelow) drawLine(railColor, Offset(cx, y), Offset(cx, size.height), stroke)
            drawCircle(ringColor, radius = 8.dp.toPx(), center = Offset(cx, y))
            drawCircle(dotColor, radius = 5.5.dp.toPx(), center = Offset(cx, y))
        }
        Box(modifier = Modifier.weight(1f).padding(bottom = 8.dp)) { content() }
    }
}

/** "No activity logged · 08:16 – 08:19" with an Assign button; the whole row is tappable. */
@Composable
fun GapRow(gap: UnassignedGap, now: Long, onAssign: () -> Unit) {
    val t = strings
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onAssign,
        modifier = Modifier.fillMaxWidth().testTag("gap_card_${gap.start}")
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.noActivityLogged, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = muted)
                Text(
                    text = "${DateFormats.hourMinute(gap.start)} – ${gap.end?.let { DateFormats.hourMinute(it) } ?: t.running}",
                    fontSize = 11.sp,
                    color = muted.copy(alpha = 0.7f)
                )
            }
            Text(
                text = TimeFormat.hoursMinutes(gap.durationSeconds(now)) + if (gap.end == null) " …" else "",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = muted
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilledTonalButton(onClick = onAssign, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.testTag("gap_assign_${gap.start}")) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(t.assignGap, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** Plain, read-only "Clocked in 08:02" / "Clocked out · Lunch 12:00" line on the rail (periods are edited in Reports). */
@Composable
private fun AttendanceEventRow(item: TimelineItem.Attendance, now: Long) {
    val t = strings
    val reason = if (item.isClockIn) null else ClockOutReason.fromName(item.session.clockOutReason)
    val pause = item.pauseSeconds(now)
    val color = when {
        item.isClockIn -> EmeraldGreen
        reason == ClockOutReason.LUNCH || reason == ClockOutReason.BREAK -> AmberWarning
        else -> RoseUrgent
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp).testTag(item.key),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (item.isClockIn) Icons.Default.Login else (reason?.icon() ?: Icons.Default.Logout),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = (if (item.isClockIn) t.eventClockedIn else t.eventClockedOut) + (reason?.let { " · ${t.reasonLabel(it)}" } ?: ""),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (pause != null && pause > 0) {
            // How long the pause lasted (or has lasted so far).
            Text(
                text = TimeFormat.hoursMinutes(pause) + if (item.nextClockIn == null) " …" else "",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp).testTag("${item.key}-pause")
            )
        }
        Text(
            text = DateFormats.hourMinute(item.time),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttendanceCard(
    isClockedIn: Boolean,
    clockedInSince: Long?,
    attendanceSeconds: Long,
    lastClosedSession: AttendanceSession?,
    now: Long,
    onClockIn: () -> Unit,
    /** null = plain clock-out; LUNCH / BREAK tag the pause. */
    onClockOut: (ClockOutReason?) -> Unit
) {
    val t = strings
    var reasonMenuOpen by remember { mutableStateOf(false) }
    // A tagged pause (lunch, break) that is still going on: show what it is and how long already.
    val pauseReason = lastClosedSession?.let { ClockOutReason.fromName(it.clockOutReason) }
        ?.takeIf { it == ClockOutReason.LUNCH || it == ClockOutReason.BREAK }
    val pauseSeconds = if (pauseReason != null) ((now - (lastClosedSession?.clockOut ?: now)) / 1000L).coerceAtLeast(0L) else 0L
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isClockedIn) 1.25f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "scale",
    )
    val statusColor = when {
        isClockedIn -> EmeraldGreen
        pauseReason != null -> AmberWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("attendance_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pauseReason != null) {
                    Icon(pauseReason.icon(), contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                } else {
                    Box(modifier = Modifier.size(10.dp).scale(pulseScale).clip(CircleShape).background(statusColor))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isClockedIn -> t.clockedInSince(DateFormats.hourMinute(clockedInSince ?: 0L))
                        // e.g. "LUNCH · since 12:02 · 0h 23m"
                        pauseReason != null -> "${t.reasonLabel(pauseReason).uppercase()} · ${t.since(DateFormats.hourMinute(lastClosedSession?.clockOut ?: now))} · ${TimeFormat.hoursMinutes(pauseSeconds)}"
                        else -> t.clockedOut
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = statusColor,
                    modifier = Modifier.testTag("attendance_status_text")
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
                // One tap simply clocks out; the small trailing button offers "Lunch" and "Break"
                // so the pause can be tagged (and lunch time summed) when it matters.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onClockOut(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseUrgent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("clock_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t.clockOut, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Box {
                        FilledTonalIconButton(
                            onClick = { reasonMenuOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(48.dp).testTag("clock_out_reason_button")
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = t.clockOutReason)
                        }
                        DropdownMenu(expanded = reasonMenuOpen, onDismissRequest = { reasonMenuOpen = false }) {
                            listOf(ClockOutReason.LUNCH, ClockOutReason.BREAK).forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(t.reasonLabel(reason)) },
                                    leadingIcon = { Icon(reason.icon(), contentDescription = null) },
                                    onClick = {
                                        reasonMenuOpen = false
                                        onClockOut(reason)
                                    },
                                    modifier = Modifier.testTag("clock_out_reason_${reason.name}")
                                )
                            }
                        }
                    }
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
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = t.edit, modifier = Modifier.size(16.dp))
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

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
            if (report.targetSeconds > 0) {
                TargetProgress(attendanceSeconds = report.accountedSeconds, targetSeconds = report.targetSeconds)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Clocked in = project work + unproductive + time with no activity running.
            // Optional rows appear once they hold at least a minute (whole seconds would print as 0h 00m).
            SummaryRow(t.clockedIn, report.attendanceSeconds, MaterialTheme.colorScheme.onSurface)
            SummaryRow(t.projectWork, report.productiveSeconds, EmeraldGreen)
            if (report.unassignedProductiveSeconds >= 60) {
                SummaryRow("↳ ${t.unassignedProject}", report.unassignedProductiveSeconds, AmberWarning, indent = true)
            }
            if (report.noTaskProductiveSeconds >= 60) {
                SummaryRow("↳ ${t.generalTaskTime}", report.noTaskProductiveSeconds, MaterialTheme.colorScheme.onSurfaceVariant, indent = true)
            }
            SummaryRow(t.unproductive, report.unproductiveSeconds, AmberWarning)
            if (report.unallocatedSeconds >= 60) {
                SummaryRow(t.noActivity, report.unallocatedSeconds, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Credited on top of the clocked-in time; lunch is shown for information only.
            if (report.paidBreakSeconds >= 60) {
                SummaryRow("+ ${t.paidBreak}", report.paidBreakSeconds, EmeraldGreen)
            }
            if (report.deductedBreakSeconds >= 60) {
                SummaryRow(t.missingBreakDeducted, report.deductedBreakSeconds, RoseUrgent)
            }
            if (report.lunchSeconds >= 60) {
                SummaryRow(t.reasonLunch, report.lunchSeconds, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Overtime only once the day's target is exceeded; until then the target row says what is left.
            if (report.overtimeSeconds > 0) SignedRow(t.overtimeToday, report.overtimeSeconds)
        }
    }
}

/** Clocked-in time against today's target: bar plus "2h 10m to go" / "Target reached". */
@Composable
private fun TargetProgress(attendanceSeconds: Long, targetSeconds: Long) {
    val t = strings
    val reached = attendanceSeconds >= targetSeconds
    val remaining = (targetSeconds - attendanceSeconds).coerceAtLeast(0)
    val progress = (attendanceSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(t.targetLabel(TimeFormat.hoursMinutes(targetSeconds)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (reached) t.targetReached else t.remainingToTarget(TimeFormat.hoursMinutes(remaining)),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (reached) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("target_remaining_text")
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = if (reached) EmeraldGreen else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).testTag("target_progress_bar")
        )
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
private fun SummaryRow(label: String, seconds: Long, color: Color, indent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (indent) 12.dp else 0.dp, top = 2.dp, bottom = 2.dp),
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
    isSomethingRunning: Boolean = false,
    /** The project colour bar on the left; off inside the timeline, whose dot already carries the colour. */
    showColorBar: Boolean = true
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
            if (showColorBar) {
                Box(modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(projColor))
                Spacer(modifier = Modifier.width(12.dp))
            }

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

            Text(
                text = TimeFormat.hoursMinutes(seconds),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            // One-tap continue / switch stays on the row; edit and delete are in the tap sheet.
            if (onContinue != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onContinue, modifier = Modifier.size(40.dp).testTag("continue_entry_${entry.id}")) {
                    Icon(
                        if (isSomethingRunning) Icons.Default.SwapHoriz else Icons.Default.PlayArrow,
                        contentDescription = switchLabel,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.report.DayRow
import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.data.report.WarningKind
import com.suw1labs.worktracker.ui.components.ConfirmDeleteDialog
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
import com.suw1labs.worktracker.ui.components.SessionRow
import com.suw1labs.worktracker.ui.i18n.emoji
import com.suw1labs.worktracker.ui.theme.AmberWarning
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.ReportPeriodType
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.parseHexColor
import com.suw1labs.worktracker.util.toLocalDate
import com.suw1labs.worktracker.ui.i18n.strings
import kotlinx.coroutines.launch
import kotlinx.datetime.isoDayNumber

@Composable
fun ReportsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val t = strings
    val now by viewModel.now.collectAsState()
    val periodType by viewModel.periodType.collectAsState()
    val periodAnchor by viewModel.periodAnchor.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val report by viewModel.periodReport.collectAsState()
    val calendarReport by viewModel.calendarReport.collectAsState()
    val weekReport by viewModel.weekReport.collectAsState()
    val overtimeBalance by viewModel.overtimeBalance.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val daySessions by viewModel.selectedDaySessions.collectAsState()
    val dayEntries by viewModel.selectedDayEntries.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showAddAbsence by remember { mutableStateOf(false) }
    var editingAbsence by remember { mutableStateOf<DayRecord?>(null) }
    var showAddSession by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<AttendanceSession?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimeEntryWithDetails?>(null) }
    var deletingSession by remember { mutableStateOf<AttendanceSession?>(null) }
    var deletingAbsence by remember { mutableStateOf<DayRecord?>(null) }
    val snackbarHost = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(entry: TimeEntryWithDetails) {
        viewModel.deleteTimeEntry(entry.id)
        scope.launch {
            val result = snackbarHost.showSnackbar(message = t.activityDeleted, actionLabel = t.undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month scope: full calendar. Day / week scope: strip with the working days of the week.
        item {
            if (periodType == ReportPeriodType.MONTH) {
                MonthCalendar(
                    report = calendarReport,
                    selectedDay = periodAnchor,
                    today = DateRanges.dayRange(now).start,
                    onPreviousMonth = { viewModel.shiftCalendarMonth(-1) },
                    onNextMonth = { viewModel.shiftCalendarMonth(1) },
                    onSelectDay = { viewModel.selectDay(it) },
                    onToday = { viewModel.resetPeriodToToday() }
                )
            } else {
                WeekStrip(
                    report = weekReport,
                    selectedDay = periodAnchor,
                    today = DateRanges.dayRange(now).start,
                    onPreviousWeek = { viewModel.shiftCalendarWeek(-1) },
                    onNextWeek = { viewModel.shiftCalendarWeek(1) },
                    onSelectDay = { viewModel.selectDay(it) },
                    onToday = { viewModel.resetPeriodToToday() }
                )
            }
        }

        // Scope + export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = periodType == type,
                            onClick = { viewModel.setPeriodType(type) },
                            label = { Text(t.periodLabel(type), fontSize = 12.sp) },
                            modifier = Modifier.testTag("period_chip_${type.name}")
                        )
                    }
                }
                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("export_reports_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t.sapExport, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Month-end check: what still needs fixing before the hours go into SAP.
        if (periodType == ReportPeriodType.MONTH) {
            item {
                MonthEndCheckCard(
                    report = report,
                    isCurrentMonth = report.range.start <= now && now < report.range.endExclusive,
                    stillClockedInToday = viewModel.openSession.collectAsState().value != null,
                    onFixClockOut = { viewModel.fixForgottenClockOut(it) },
                    clockOutTimeFor = { viewModel.forgottenClockOutTimeFor(it) }
                )
            }
        }

        // Summary for the selected scope
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("period_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(periodLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("period_label"))
                    Spacer(modifier = Modifier.height(8.dp))
                    PlainRow(t.clockedIn, TimeFormat.hoursMinutes(report.attendanceSeconds))
                    PlainRow(t.projectWork, TimeFormat.hoursMinutes(report.productiveSeconds), EmeraldGreen)
                    PlainRow(t.unproductive, TimeFormat.hoursMinutes(report.unproductiveSeconds), AmberWarning)
                    if (report.unallocatedSeconds >= 60) PlainRow(t.noActivity, TimeFormat.hoursMinutes(report.unallocatedSeconds), MaterialTheme.colorScheme.onSurfaceVariant)
                    if (report.lunchSeconds >= 60) PlainRow(t.reasonLunch, TimeFormat.hoursMinutes(report.lunchSeconds), MaterialTheme.colorScheme.onSurfaceVariant)
                    if (report.paidBreakSeconds >= 60) PlainRow("+ ${t.paidBreak}", TimeFormat.hoursMinutes(report.paidBreakSeconds), EmeraldGreen)
                    if (report.creditedSeconds > 0) PlainRow(t.absencesCredited, TimeFormat.hoursMinutes(report.creditedSeconds))
                    PlainRow(t.target, TimeFormat.hoursMinutes(report.targetSeconds))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    SignedHoursRow(t.overtime, report.overtimeSeconds)
                    SignedHoursRow(t.balanceToDate, overtimeBalance)
                }
            }
        }

        // Hours per SAP project
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("project_hours_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(t.hoursPerProject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val unproductiveIds = remember(report) { report.days.flatMap { it.cells }.filter { !it.isProductive }.map { it.projectId }.toSet() }
                    val projectRows = report.projects.filter { it.projectId != null && it.projectId !in unproductiveIds }
                    if (report.unassignedProductiveSeconds > 0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚠ ${t.unassignedProject}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                Text(t.unassignedHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(TimeFormat.hoursMinutes(report.unassignedProductiveSeconds), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AmberWarning)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    if (projectRows.isEmpty()) {
                        Text(t.noProjectHours, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val totalProjectSeconds = projectRows.sumOf { it.seconds }.coerceAtLeast(1)
                        projectRows.forEachIndexed { index, p ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            val color = parseHexColor(p.colorHex) ?: MaterialTheme.colorScheme.primary
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(p.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(TimeFormat.hoursMinutes(p.seconds), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            // Share of the project total, so the split is visible at a glance.
                            LinearProgressIndicator(
                                progress = { (p.seconds.toFloat() / totalProjectSeconds).coerceIn(0f, 1f) },
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        PlainRow(t.total, TimeFormat.hoursMinutes(projectRows.sumOf { it.seconds }))
                    }
                }
            }
        }

        // Warnings only when there are any
        if (report.warnings.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth().testTag("compliance_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t.workingTimeRules, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        report.warnings.forEach { w ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(DateFormats.monthDay(w.dayStart), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                                Text(t.warning(w), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (w.kind == WarningKind.STILL_CLOCKED_IN_PAST_DAY) {
                                    val at = viewModel.forgottenClockOutTimeFor(w.dayStart)
                                    if (at != null) {
                                        TextButton(onClick = { viewModel.fixForgottenClockOut(w.dayStart) }, modifier = Modifier.testTag("fix_clock_out_${w.dayStart}")) {
                                            Text(t.clockOutAt(DateFormats.hourMinute(at)), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Absences (week / month) – add button lives here and in the day view
        if (periodType != ReportPeriodType.DAY) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("absences_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t.absences, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAddAbsence = true }, modifier = Modifier.testTag("add_absence_button")) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t.add, fontSize = 12.sp)
                            }
                        }
                        val absences = report.absences
                        if (absences.isEmpty()) {
                            Text(t.noneInPeriod, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            absences.forEach { a -> AbsenceRow(a, onEdit = { editingAbsence = a }, onDelete = { deletingAbsence = a }) }
                        }
                    }
                }
            }
        }

        // Day view: clock-in periods and activities of the selected day
        if (periodType == ReportPeriodType.DAY) {
            item {
                val day = report.days.firstOrNull()
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("attendance_timeline_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t.clockInPeriods, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(onClick = { showAddAbsence = true }, modifier = Modifier.testTag("add_absence_button")) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (day?.absence == null) t.absence else t.editAbsence, fontSize = 12.sp)
                                }
                                TextButton(onClick = { showAddSession = true }, modifier = Modifier.testTag("add_session_button")) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(t.add, fontSize = 12.sp)
                                }
                            }
                        }
                        day?.absence?.let { a -> AbsenceRow(a, onEdit = { editingAbsence = a }, onDelete = { deletingAbsence = a }) }
                        if (daySessions.isEmpty()) {
                            Text(t.notClockedInOnDay, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                        } else {
                            daySessions.forEachIndexed { index, session ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                SessionRow(session = session, now = now, onEdit = { editingSession = session }, onDelete = { deletingSession = session })
                            }
                            if (day != null && day.breakSeconds > 0) {
                                Text(
                                    "${t.breaks}: ${TimeFormat.hoursMinutes(day.breakSeconds)}" +
                                        if (day.lunchSeconds > 0) " · ${t.reasonLunch} ${TimeFormat.hoursMinutes(day.lunchSeconds)}" else "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(t.activities, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showManualEntry = true }, modifier = Modifier.testTag("manual_time_entry_button")) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t.add, fontSize = 12.sp)
                    }
                }
            }
            if (dayEntries.isEmpty()) {
                item { Text(t.noActivitiesOnDay, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(dayEntries, key = { it.id }) { entry ->
                    TimeEntryRowCard(entry = entry, now = now, onEdit = { editingEntry = entry }, onDelete = { deleteWithUndo(entry) })
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    deletingSession?.let { session ->
        ConfirmDeleteDialog(
            title = t.clockInPeriods,
            message = t.deletePeriodQuestion,
            onConfirm = {
                viewModel.deleteSession(session.id)
                deletingSession = null
            },
            onDismiss = { deletingSession = null }
        )
    }

    deletingAbsence?.let { record ->
        ConfirmDeleteDialog(
            title = t.absence,
            message = t.deleteAbsenceQuestion,
            onConfirm = {
                viewModel.deleteDayRecord(record.id)
                deletingAbsence = null
            },
            onDismiss = { deletingAbsence = null }
        )
    }

    if (showExportDialog) {
        SapExportDialog(
            periodLabel = periodLabel,
            buildContent = { type, quarter, format -> viewModel.buildSapExport(type, quarter, format) },
            onShare = { type, format, content, action -> viewModel.shareSapExport(type, format, content, action) },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showAddAbsence) {
        val existing = if (periodType == ReportPeriodType.DAY) report.days.firstOrNull()?.absence else null
        DayRecordDialog(
            record = existing,
            initialDayStart = periodAnchor,
            defaultHours = settings.dailyTargetHours,
            onDismiss = { showAddAbsence = false },
            onSave = { dayStart, type, hours, label, note ->
                viewModel.saveDayRecord(dayStart, type, hours, label, note)
                showAddAbsence = false
            }
        )
    }

    editingAbsence?.let { record ->
        DayRecordDialog(
            record = record,
            initialDayStart = record.dayStart,
            defaultHours = settings.dailyTargetHours,
            onDismiss = { editingAbsence = null },
            onSave = { dayStart, type, hours, label, note ->
                viewModel.saveDayRecord(dayStart, type, hours, label, note)
                editingAbsence = null
            }
        )
    }

    if (showAddSession) {
        SessionFormDialog(
            session = null,
            initialDayStart = periodAnchor,
            onDismiss = { showAddSession = false },
            onSave = { clockIn, clockOut, reason ->
                if (clockOut != null) viewModel.addManualSession(clockIn, clockOut, reason)
                showAddSession = false
            }
        )
    }

    editingSession?.let { session ->
        SessionFormDialog(
            session = session,
            initialDayStart = periodAnchor,
            onDismiss = { editingSession = null },
            onSave = { clockIn, clockOut, reason ->
                viewModel.updateSession(session.copy(clockIn = clockIn, clockOut = clockOut, clockOutReason = reason?.name))
                editingSession = null
            }
        )
    }

    if (showManualEntry) {
        EntryFormDialog(
            entry = null,
            projects = activeProjects,
            tasks = allTasks,
            initialDayStart = periodAnchor,
            onDismiss = { showManualEntry = false },
            onSave = { projectId, taskId, description, start, end ->
                if (end != null) viewModel.addManualEntry(projectId, taskId, description, start, end)
                showManualEntry = false
            },
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
            initialDayStart = periodAnchor,
            onDismiss = { editingEntry = null },
            onSave = { projectId, taskId, description, start, end ->
                viewModel.updateEntry(entry.toEntity().copy(projectId = projectId, taskId = taskId, description = description, startTime = start, endTime = end))
                editingEntry = null
            },
            onQuickTask = { projectId, title, onCreated -> viewModel.addQuickTask(projectId, title, onCreated) },
            onQuickProject = { code, name, client, color, budget, productive, onCreated -> viewModel.addProject(code, name, client, color, budget, productive, onCreated) },
            timeOnly = true
        )
    }
}

/**
 * "Ready to book" or the list of things to fix first: forgotten clock-outs (with a one-tap fix),
 * productive time without a project, working-time warnings, and the rounded project total.
 */
@Composable
private fun MonthEndCheckCard(
    report: PeriodReport,
    isCurrentMonth: Boolean,
    stillClockedInToday: Boolean,
    onFixClockOut: (Long) -> Unit,
    clockOutTimeFor: (Long) -> Long?
) {
    val t = strings
    val forgotten = report.warnings.filter { it.kind == WarningKind.STILL_CLOCKED_IN_PAST_DAY }
    val ruleWarnings = report.warnings.size - forgotten.size
    val unassigned = report.unassignedProductiveSeconds
    val issues = (if (forgotten.isNotEmpty()) 1 else 0) + (if (unassigned >= 60) 1 else 0) + (if (ruleWarnings > 0) 1 else 0)
    val ready = issues == 0
    val color = if (ready) EmeraldGreen else AmberWarning
    val unproductiveIds = report.days.flatMap { it.cells }.filter { !it.isProductive }.map { it.projectId }.toSet()
    val projectSeconds = report.projects.filter { it.projectId != null && it.projectId !in unproductiveIds }.map { it.seconds }
    val exact = projectSeconds.sumOf { TimeFormat.decimalHours(it) }
    val rounded = projectSeconds.sumOf { TimeFormat.quarterHours(it) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth().testTag("month_end_check_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ready) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(t.monthEndCheck, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (ready) t.readyToBook else t.thingsToFix(issues), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.testTag("month_end_status"))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (forgotten.isNotEmpty()) {
                CheckRow(t.checkForgotClockOut(forgotten.size), ok = false)
                forgotten.forEach { w ->
                    val at = clockOutTimeFor(w.dayStart)
                    if (at != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 26.dp)) {
                            Text(DateFormats.monthDay(w.dayStart), fontSize = 12.sp, modifier = Modifier.width(56.dp))
                            TextButton(onClick = { onFixClockOut(w.dayStart) }, modifier = Modifier.testTag("month_fix_clock_out_${w.dayStart}")) {
                                Text(t.clockOutAt(DateFormats.hourMinute(at)), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (unassigned >= 60) CheckRow(t.checkUnassigned(TimeFormat.hoursMinutes(unassigned)), ok = false)
            if (ruleWarnings > 0) CheckRow(t.checkRuleWarnings(ruleWarnings), ok = false)
            CheckRow(t.checkRoundedTotal(TimeFormat.sapHours(rounded), TimeFormat.sapHours(exact)), ok = true)
            if (isCurrentMonth && stillClockedInToday) CheckRow(t.checkOpenToday, ok = true, muted = true)
        }
    }
}

@Composable
private fun CheckRow(text: String, ok: Boolean, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else if (ok) EmeraldGreen else AmberWarning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MonthCalendar(
    report: PeriodReport,
    selectedDay: Long,
    today: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onToday: () -> Unit
) {
    val t = strings
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("month_calendar")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.testTag("calendar_previous")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = t.previousMonth)
                }
                Text(
                    text = DateFormats.monthYear(report.range.start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onToday).testTag("calendar_month_label")
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.testTag("calendar_next")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = t.nextMonth)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                t.weekdaysTwo.forEach { name ->
                    Text(
                        name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            val leadingBlanks = report.range.start.toLocalDate().dayOfWeek.isoDayNumber - 1
            val cells: List<DayRow?> = List(leadingBlanks) { null } + report.days
            val warningDays = remember(report) { report.warnings.map { it.dayStart }.toSet() }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f).padding(2.dp)) {
                            if (day != null) {
                                CalendarCell(
                                    day = day,
                                    selected = day.range.start == selectedDay,
                                    isToday = day.range.start == today,
                                    hasWarning = day.range.start in warningDays,
                                    onClick = { onSelectDay(day.range.start) }
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Working days of one week (Mon–Fri by default; follows the schedule settings). */
@Composable
private fun WeekStrip(
    report: PeriodReport,
    selectedDay: Long,
    today: Long,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onToday: () -> Unit
) {
    val t = strings
    val workDays = report.days.filter { it.isWorkday }.ifEmpty { report.days }
    val warningDays = remember(report) { report.warnings.map { it.dayStart }.toSet() }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("week_strip")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousWeek, modifier = Modifier.testTag("week_previous")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = t.previousWeek)
                }
                Text(
                    text = DateRanges.label(ReportPeriodType.WEEK, report.range.start),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onToday).testTag("week_label")
                )
                IconButton(onClick = onNextWeek, modifier = Modifier.testTag("week_next")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = t.nextWeek)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                workDays.forEach { day ->
                    Column(modifier = Modifier.weight(1f).padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            t.weekdaysTwo[day.range.start.toLocalDate().dayOfWeek.isoDayNumber - 1],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CalendarCell(
                            day = day,
                            selected = day.range.start == selectedDay,
                            isToday = day.range.start == today,
                            hasWarning = day.range.start in warningDays,
                            onClick = { onSelectDay(day.range.start) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(day: DayRow, selected: Boolean, isToday: Boolean, hasWarning: Boolean, onClick: () -> Unit) {
    val absence = day.absence
    val background = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        absence != null -> AmberWarning.copy(alpha = 0.14f)
        day.attendanceSeconds > 0 -> EmeraldGreen.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val dayNumber = day.range.start.toLocalDate().day
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .let { if (isToday) it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else it }
            .clickable(onClick = onClick)
            .testTag("calendar_day_$dayNumber")
            .padding(top = 4.dp)
    ) {
        Text(
            dayNumber.toString(),
            fontSize = 12.sp,
            fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isWorkday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Green dot: clocked in on this day. Red dot: working-time warning. Emoji: absence.
            if (day.attendanceSeconds > 0) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldGreen))
            }
            if (hasWarning) {
                if (day.attendanceSeconds > 0) Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(RoseUrgent))
            }
            if (absence != null) {
                if (day.attendanceSeconds > 0 || hasWarning) Spacer(modifier = Modifier.width(2.dp))
                Text(absence.absenceType.emoji(), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AbsenceRow(a: DayRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    val t = strings
    val label = if (a.absenceType == AbsenceType.OTHER && a.label.isNotBlank()) a.label else t.absenceLabel(a.absenceType)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${a.absenceType.emoji()} ${DateFormats.monthDay(a.dayStart)} · $label", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                TimeFormat.hoursMinutes((a.hours * 3600).toLong()) + " " + (if (a.absenceType.creditsHours) t.credited else t.fromOvertime) + if (a.note.isNotBlank()) " · ${a.note}" else "",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = t.edit, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = t.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PlainRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SignedHoursRow(label: String, seconds: Long) {
    val color = when {
        seconds > 0 -> EmeraldGreen
        seconds < 0 -> RoseUrgent
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val abs = kotlin.math.abs(seconds)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            (if (seconds < 0) "-" else if (seconds > 0) "+" else "") + TimeFormat.hoursMinutes(abs),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

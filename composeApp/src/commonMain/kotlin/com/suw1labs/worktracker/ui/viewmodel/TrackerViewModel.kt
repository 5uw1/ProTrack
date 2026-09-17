package com.suw1labs.worktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suw1labs.worktracker.data.backup.AutoBackup
import com.suw1labs.worktracker.data.backup.AutoBackupState
import com.suw1labs.worktracker.data.backup.BackupCodec
import com.suw1labs.worktracker.data.backup.BackupError
import com.suw1labs.worktracker.data.backup.BackupException
import com.suw1labs.worktracker.data.backup.BackupFile
import com.suw1labs.worktracker.data.backup.BackupManager
import com.suw1labs.worktracker.data.backup.BackupSummary
import com.suw1labs.worktracker.data.export.ExportFormat
import com.suw1labs.worktracker.data.export.SapExport
import com.suw1labs.worktracker.data.export.SapExportType
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.ProjectSummary
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.data.import.ProjectImporter
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import com.suw1labs.worktracker.platform.BackupFolder
import com.suw1labs.worktracker.platform.ExportAction
import com.suw1labs.worktracker.platform.FileExporter
import com.suw1labs.worktracker.platform.NoOpFileExporter
import com.suw1labs.worktracker.platform.NoOpReminderScheduler
import com.suw1labs.worktracker.platform.ReminderScheduler
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.DateRange
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.ReportPeriodType
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.toLocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(
    private val repository: TimeTrackerRepository,
    private val reminderScheduler: ReminderScheduler = NoOpReminderScheduler,
    private val fileExporter: FileExporter = NoOpFileExporter,
    private val backupManager: BackupManager? = null,
    private val autoBackup: AutoBackup? = null
) : ViewModel() {

    private fun <T> kotlinx.coroutines.flow.Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    /** Wall clock, updated every second while the UI is visible (drives running durations). */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(currentTimeMillis())
            delay(1000L)
        }
    }.asState(currentTimeMillis())

    private val nowMinute = now.map { it / 60_000L }.distinctUntilChanged()

    // --- Database streams ---
    val allProjects: StateFlow<List<Project>> = repository.allProjects.asState(emptyList())
    val activeProjects: StateFlow<List<Project>> = repository.activeProjects.asState(emptyList())
    val allTasks: StateFlow<List<WorkTaskWithProject>> = repository.allTasksWithProject.asState(emptyList())
    val allEntries: StateFlow<List<TimeEntryWithDetails>> = repository.allTimeEntries.asState(emptyList())
    val allSessions: StateFlow<List<AttendanceSession>> = repository.allSessions.asState(emptyList())
    val projectSummaries: StateFlow<List<ProjectSummary>> = repository.projectSummaries.asState(emptyList())
    val dayRecords: StateFlow<List<DayRecord>> = repository.allDayRecords.asState(emptyList())
    val settings: StateFlow<AppSettings> = repository.settings.map { it ?: AppSettings() }
        .map { s ->
            // Keep date formatting in sync with the UI language.
            val t = com.suw1labs.worktracker.ui.i18n.Translations.forLanguage(com.suw1labs.worktracker.ui.i18n.Language.fromCode(s.language))
            com.suw1labs.worktracker.util.DateFormats.names = com.suw1labs.worktracker.util.DateNames(t.monthsShort, t.monthsLong, t.weekdaysShort, t.weekdaysLong)
            s
        }
        .asState(AppSettings())

    fun setLanguage(language: com.suw1labs.worktracker.ui.i18n.Language) {
        viewModelScope.launch { repository.saveSettings(settings.value.copy(language = language.code)) }
    }

    /** The open attendance session, or null when clocked out. */
    val openSession: StateFlow<AttendanceSession?> = repository.openSession.asState(null)

    /** The activity currently running, or null. */
    val runningEntry: StateFlow<TimeEntryWithDetails?> =
        allEntries.map { entries -> entries.firstOrNull { it.isRunning } }.asState(null)

    // Tasks nearing deadlines (Urgent Deadline Alerts)
    val urgentTasks: StateFlow<List<WorkTaskWithProject>> = allTasks.map { tasks ->
        val now = currentTimeMillis()
        val seventyTwoHours = 72 * 3600 * 1000L
        tasks.filter {
            it.status != "DONE" &&
                    it.deadlineTimestamp != null &&
                    it.deadlineTimestamp <= (now + seventyTwoHours)
        }.sortedBy { it.deadlineTimestamp }
    }.asState(emptyList())

    // --- Today ---
    private val todayRange: StateFlow<DateRange> = now.map { DateRanges.dayRange(it) }.distinctUntilChanged().asState(DateRanges.dayRange(currentTimeMillis()))

    val todayReport: StateFlow<PeriodReport> = combine(allSessions, allEntries, todayRange, now, settings, dayRecords) { values ->
        @Suppress("UNCHECKED_CAST")
        ReportCalculator.compute(
            sessions = values[0] as List<AttendanceSession>,
            entries = values[1] as List<TimeEntryWithDetails>,
            range = values[2] as DateRange,
            now = values[3] as Long,
            settings = values[4] as AppSettings,
            dayRecords = values[5] as List<DayRecord>
        )
    }.asState(PeriodReport.empty(DateRanges.dayRange(currentTimeMillis())))

    /** Overtime balance (worked + credited absences − target) from the first recorded day until today. */
    val overtimeBalance: StateFlow<Long> = combine(allSessions, dayRecords, settings, nowMinute) { sessions, records, settings, _ ->
        ReportCalculator.overtimeBalanceToDate(sessions, records, currentTimeMillis(), settings)
    }.asState(0L)

    val todaySessions: StateFlow<List<AttendanceSession>> = combine(allSessions, todayRange) { sessions, range ->
        sessions.filter { (it.clockOut ?: Long.MAX_VALUE) > range.start && it.clockIn < range.endExclusive }
            .sortedBy { it.clockIn }
    }.asState(emptyList())

    val todayEntries: StateFlow<List<TimeEntryWithDetails>> = combine(allEntries, todayRange) { entries, range ->
        entries.filter { (it.endTime ?: Long.MAX_VALUE) > range.start && it.startTime < range.endExclusive }
            .sortedByDescending { it.startTime }
    }.asState(emptyList())

    // --- Reports (day / week / month navigation) ---
    private val _periodType = MutableStateFlow(ReportPeriodType.MONTH)
    val periodType: StateFlow<ReportPeriodType> = _periodType.asStateFlow()

    private val _periodAnchor = MutableStateFlow(DateRanges.startOfToday())
    val periodAnchor: StateFlow<Long> = _periodAnchor.asStateFlow()

    val periodLabel: StateFlow<String> = combine(_periodType, _periodAnchor, settings) { type, anchor, _ ->
        DateRanges.label(type, anchor)
    }.asState(DateRanges.label(ReportPeriodType.MONTH, DateRanges.startOfToday()))

    val periodReport: StateFlow<PeriodReport> = combine(
        allSessions, allEntries, _periodType, _periodAnchor, nowMinute, settings, dayRecords
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ReportCalculator.compute(
            sessions = values[0] as List<AttendanceSession>,
            entries = values[1] as List<TimeEntryWithDetails>,
            range = DateRanges.rangeFor(values[2] as ReportPeriodType, values[3] as Long),
            now = currentTimeMillis(),
            settings = values[5] as AppSettings,
            dayRecords = values[6] as List<DayRecord>
        )
    }.asState(PeriodReport.empty(DateRanges.monthRange(currentTimeMillis())))

    /** Report for the whole calendar month around the selected day (drives the calendar grid). */
    val calendarReport: StateFlow<PeriodReport> = combine(
        allSessions, allEntries, _periodAnchor, nowMinute, settings, dayRecords
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ReportCalculator.compute(
            sessions = values[0] as List<AttendanceSession>,
            entries = values[1] as List<TimeEntryWithDetails>,
            range = DateRanges.monthRange(values[2] as Long),
            now = currentTimeMillis(),
            settings = values[4] as AppSettings,
            dayRecords = values[5] as List<DayRecord>
        )
    }.asState(PeriodReport.empty(DateRanges.monthRange(currentTimeMillis())))

    /** Report for the week around the selected day (drives the working-day strip). */
    val weekReport: StateFlow<PeriodReport> = combine(
        allSessions, allEntries, _periodAnchor, nowMinute, settings, dayRecords
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ReportCalculator.compute(
            sessions = values[0] as List<AttendanceSession>,
            entries = values[1] as List<TimeEntryWithDetails>,
            range = DateRanges.weekRange(values[2] as Long),
            now = currentTimeMillis(),
            settings = values[4] as AppSettings,
            dayRecords = values[5] as List<DayRecord>
        )
    }.asState(PeriodReport.empty(DateRanges.weekRange(currentTimeMillis())))

    fun shiftCalendarWeek(delta: Int) {
        _periodAnchor.value = DateRanges.shiftAnchor(ReportPeriodType.WEEK, _periodAnchor.value, delta)
    }

    /** Clock-in periods of the selected day (for editing in the day view). */
    val selectedDaySessions: StateFlow<List<AttendanceSession>> = combine(allSessions, _periodAnchor) { sessions, anchor ->
        val range = DateRanges.dayRange(anchor)
        sessions.filter { (it.clockOut ?: Long.MAX_VALUE) > range.start && it.clockIn < range.endExclusive }.sortedBy { it.clockIn }
    }.asState(emptyList())

    /** Activities of the selected day (for editing in the day view). */
    val selectedDayEntries: StateFlow<List<TimeEntryWithDetails>> = combine(allEntries, _periodAnchor) { entries, anchor ->
        val range = DateRanges.dayRange(anchor)
        entries.filter { (it.endTime ?: Long.MAX_VALUE) > range.start && it.startTime < range.endExclusive }.sortedByDescending { it.startTime }
    }.asState(emptyList())

    fun setPeriodType(type: ReportPeriodType) {
        _periodType.value = type
    }

    fun selectDay(dayStart: Long) {
        _periodAnchor.value = DateRanges.dayRange(dayStart).start
    }

    fun shiftCalendarMonth(delta: Int) {
        _periodAnchor.value = DateRanges.shiftAnchor(ReportPeriodType.MONTH, _periodAnchor.value, delta)
    }

    fun shiftPeriod(delta: Int) {
        _periodAnchor.value = DateRanges.shiftAnchor(_periodType.value, _periodAnchor.value, delta)
    }

    fun resetPeriodToToday() {
        _periodAnchor.value = DateRanges.startOfToday()
    }

    init {
        // Daily-target notification: (re)schedule whenever the clock-in state, the sessions or the
        // schedule change; cancel when clocked out or when the target was already reached.
        viewModelScope.launch {
            combine(openSession, allSessions, settings) { open, sessions, settings -> Triple(open, sessions, settings) }
                .collect { (open, sessions, s) ->
                    if (open == null) {
                        reminderScheduler.cancelDailyTargetReminder()
                        reminderScheduler.cancelStillClockedInReminder()
                        return@collect
                    }
                    val now = currentTimeMillis()
                    val today = DateRanges.dayRange(now)
                    val iso = today.start.toLocalDate().dayOfWeek.isoDayNumber
                    val target = s.targetSecondsFor(iso)
                    val worked = ReportCalculator.attendanceSeconds(sessions, today, now)
                    val remaining = target - worked
                    val t = com.suw1labs.worktracker.ui.i18n.Translations.forLanguage(com.suw1labs.worktracker.ui.i18n.Language.fromCode(s.language))
                    if (target <= 0 || remaining <= 0) {
                        reminderScheduler.cancelDailyTargetReminder()
                    } else {
                        reminderScheduler.scheduleDailyTargetReminder(
                            triggerAtMillis = now + remaining * 1000L,
                            title = t.targetReachedTitle,
                            message = t.targetReachedBody(com.suw1labs.worktracker.util.TimeFormat.hoursMinutes(target))
                        )
                    }
                    // Safety net: still clocked in well past the target (or 9 h on a day off)? Probably forgotten.
                    val stillInAfter = if (target > 0) target + STILL_CLOCKED_IN_MARGIN_SECONDS else 9 * 3600L
                    val stillInAt = now + (stillInAfter - worked).coerceAtLeast(60L) * 1000L
                    reminderScheduler.scheduleStillClockedInReminder(
                        triggerAtMillis = stillInAt,
                        title = t.stillClockedInTitle,
                        message = t.stillClockedInBody(com.suw1labs.worktracker.util.TimeFormat.hoursMinutes(stillInAfter))
                    )
                }
        }
    }

    /** Closes the clock-in period left open on [dayStart] at the most plausible time (see [ReportCalculator.forgottenClockOutTime]). */
    fun fixForgottenClockOut(dayStart: Long) {
        viewModelScope.launch {
            val day = DateRanges.dayRange(dayStart)
            val session = allSessions.value.firstOrNull { it.clockOut == null && it.clockIn >= day.start && it.clockIn < day.endExclusive } ?: return@launch
            repository.closeForgottenSession(session, ReportCalculator.forgottenClockOutTime(session, allEntries.value, settings.value))
        }
    }

    /** When the open period's day is over, the time at which "Clock out at …" would close it. */
    fun forgottenClockOutTimeFor(dayStart: Long): Long? {
        val day = DateRanges.dayRange(dayStart)
        val session = allSessions.value.firstOrNull { it.clockOut == null && it.clockIn >= day.start && it.clockIn < day.endExclusive } ?: return null
        return ReportCalculator.forgottenClockOutTime(session, allEntries.value, settings.value)
    }

    private companion object {
        /** How long past the daily target the "still clocked in?" reminder fires. */
        const val STILL_CLOCKED_IN_MARGIN_SECONDS = 45 * 60L
    }

    // --- Attendance ---
    /**
     * Clocks in and continues the activity that was running before the last clock-out
     * (same project, task and note), so a lunch break does not require re-selecting the work.
     */
    fun clockIn() {
        viewModelScope.launch { repository.clockIn(currentTimeMillis()) }
    }

    /** Starts a new activity with the same project, task and note as [entry] (clocks in if needed). */
    fun continueEntry(entry: TimeEntryWithDetails) {
        startActivity(projectId = entry.projectId, taskId = entry.taskId, description = entry.description)
    }

    /** Clocks out; the running activity is stopped at the same moment so nothing is counted while away. */
    /** Clocks out; [reason] tags the pause (lunch, break) and is null for a plain clock-out. */
    fun clockOut(reason: ClockOutReason? = null) {
        viewModelScope.launch { repository.clockOut(currentTimeMillis(), reason?.name) }
    }

    fun updateSession(session: AttendanceSession) {
        viewModelScope.launch { repository.updateSession(session) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteSessionById(sessionId) }
    }

    fun addManualSession(clockIn: Long, clockOut: Long, reason: ClockOutReason?) {
        if (clockOut <= clockIn) return
        viewModelScope.launch {
            repository.insertSession(AttendanceSession(clockIn = clockIn, clockOut = clockOut, clockOutReason = reason?.name))
        }
    }

    // --- Activities ---
    /**
     * Starts a new activity (stopping the running one). Clocks in automatically if needed so that
     * starting work from a task is a single tap.
     */
    fun startActivity(projectId: Long?, taskId: Long? = null, description: String = "") {
        viewModelScope.launch { repository.startActivity(projectId, taskId, description, currentTimeMillis()) }
    }

    fun startActivityFromTask(task: WorkTaskWithProject) {
        startActivity(projectId = task.projectId, taskId = task.id)
    }

    /** Updates the note of an entry (used while an activity is running or afterwards). */
    fun updateEntryNote(entry: TimeEntryWithDetails, note: String) {
        viewModelScope.launch { repository.updateTimeEntry(entry.toEntity().copy(description = note)) }
    }

    /** Creates a task with just a title (e.g. "Meeting" on the Unproductive project) unless it exists. */
    fun addQuickTask(projectId: Long, title: String, onCreated: (Long) -> Unit = {}) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val existing = repository.findTaskByTitle(projectId, clean)
            val id = existing?.id ?: repository.insertTask(WorkTask(projectId = projectId, title = clean, priority = "LOW", reminderEnabled = false))
            onCreated(id)
        }
    }

    fun stopActivity() {
        viewModelScope.launch { repository.closeRunningEntries(currentTimeMillis()) }
    }

    fun addManualEntry(
        projectId: Long?,
        taskId: Long?,
        description: String,
        startTime: Long,
        endTime: Long
    ) {
        if (endTime <= startTime) return
        viewModelScope.launch {
            repository.insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    taskId = taskId,
                    description = description,
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }
    }

    /**
     * Saves an edited entry together with [movedNeighbours]: the activities before / after it whose
     * boundary the user chose to move along, so the day stays contiguous (see EntryNeighbours).
     */
    fun updateEntry(entry: TimeEntry, movedNeighbours: List<TimeEntry> = emptyList()) {
        viewModelScope.launch {
            if (movedNeighbours.isEmpty()) repository.updateTimeEntry(entry) else repository.updateTimeEntries(listOf(entry) + movedNeighbours)
        }
    }

    fun deleteTimeEntry(entryId: Long) {
        viewModelScope.launch { repository.deleteTimeEntryById(entryId) }
    }

    /** Puts a just-deleted entry back (undo from the snackbar); the original id is kept. */
    fun restoreEntry(entry: TimeEntryWithDetails) {
        viewModelScope.launch { repository.insertTimeEntry(entry.toEntity()) }
    }

    // --- Projects & categories ---
    fun addProject(code: String, name: String, client: String, colorHex: String, budgetHours: Double, isProductive: Boolean = true, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertProject(
                Project(code = code.trim(), name = name.trim(), client = client.trim(), colorHex = colorHex, budgetHours = budgetHours, isProductive = isProductive)
            )
            onCreated(id)
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch { repository.updateProject(project) }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch { repository.deleteProjectById(projectId) }
    }

    // --- Tasks (defined by project management) ---
    fun addTask(
        projectId: Long,
        title: String,
        description: String,
        priority: String,
        estimatedHours: Double,
        deadlineTimestamp: Long?,
        reminderLeadHours: Int
    ) {
        viewModelScope.launch {
            val taskId = repository.insertTask(
                WorkTask(
                    projectId = projectId,
                    title = title,
                    description = description,
                    priority = priority,
                    estimatedHours = estimatedHours,
                    deadlineTimestamp = deadlineTimestamp,
                    reminderLeadHours = reminderLeadHours,
                    reminderEnabled = deadlineTimestamp != null
                )
            )
            val project = allProjects.value.find { it.id == projectId }
            if (project != null && deadlineTimestamp != null) {
                reminderScheduler.scheduleTaskReminder(
                    WorkTaskWithProject(
                        id = taskId,
                        projectId = projectId,
                        projectCode = project.code,
                        projectName = project.name,
                        projectColor = project.colorHex,
                        client = project.client,
                        title = title,
                        description = description,
                        priority = priority,
                        status = "TODO",
                        estimatedHours = estimatedHours,
                        deadlineTimestamp = deadlineTimestamp,
                        reminderLeadHours = reminderLeadHours,
                        reminderEnabled = true,
                        createdAt = currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateTask(task: WorkTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            val project = allProjects.value.find { it.id == task.projectId }
            if (project != null) {
                reminderScheduler.scheduleTaskReminder(
                    WorkTaskWithProject(
                        id = task.id,
                        projectId = task.projectId,
                        projectCode = project.code,
                        projectName = project.name,
                        projectColor = project.colorHex,
                        client = project.client,
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        status = task.status,
                        estimatedHours = task.estimatedHours,
                        deadlineTimestamp = task.deadlineTimestamp,
                        reminderLeadHours = task.reminderLeadHours,
                        reminderEnabled = task.reminderEnabled,
                        createdAt = task.createdAt
                    )
                )
            }
        }
    }

    /** Moves a task and its logged time to another project (e.g. from Unproductive to a real project). */
    fun moveTask(taskId: Long, projectId: Long) {
        viewModelScope.launch { repository.moveTask(taskId, projectId) }
    }

    fun updateTaskStatus(taskId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, newStatus)
            if (newStatus == "DONE") reminderScheduler.cancelTaskReminder(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelTaskReminder(taskId)
            val task = allTasks.value.find { it.id == taskId }
            if (task != null) {
                repository.deleteTask(WorkTask(id = task.id, projectId = task.projectId, title = task.title))
            }
        }
    }

    fun snoozeTask(taskId: Long, addHours: Int) {
        val taskWithP = allTasks.value.find { it.id == taskId } ?: return
        val currentDeadline = taskWithP.deadlineTimestamp ?: currentTimeMillis()
        val newDeadline = currentDeadline + (addHours * 3600 * 1000L)

        viewModelScope.launch {
            repository.updateTask(
                WorkTask(
                    id = taskWithP.id,
                    projectId = taskWithP.projectId,
                    title = taskWithP.title,
                    description = taskWithP.description,
                    priority = taskWithP.priority,
                    status = taskWithP.status,
                    estimatedHours = taskWithP.estimatedHours,
                    deadlineTimestamp = newDeadline,
                    reminderLeadHours = taskWithP.reminderLeadHours,
                    reminderEnabled = true,
                    createdAt = taskWithP.createdAt
                )
            )
            reminderScheduler.scheduleTaskReminder(taskWithP.copy(deadlineTimestamp = newDeadline))
        }
    }

    fun checkUpcomingDeadlines() {
        reminderScheduler.checkAndNotifyUrgentDeadlines(allTasks.value)
    }

    // --- Absences (sick, holiday, public holiday, compensation, education, other) ---
    fun saveDayRecord(dayStart: Long, type: AbsenceType, hours: Double, label: String, note: String) {
        viewModelScope.launch {
            val existing = repository.getDayRecord(dayStart)
            val record = DayRecord(
                id = existing?.id ?: 0,
                dayStart = dayStart,
                type = type.name,
                hours = hours,
                label = label.trim(),
                note = note.trim()
            )
            if (existing == null) repository.insertDayRecord(record) else repository.updateDayRecord(record)
        }
    }

    fun deleteDayRecord(recordId: Long) {
        viewModelScope.launch { repository.deleteDayRecordById(recordId) }
    }

    // --- Work schedule ---
    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch { repository.saveSettings(settings) }
    }

    // --- Project import (pasted from Excel / SAP / CSV) ---
    fun previewProjectImport(text: String): List<ProjectImporter.ParsedProject> = ProjectImporter.parse(text)

    fun importProjects(text: String, onDone: (added: Int, skipped: Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val parsed = ProjectImporter.parse(text)
            val projects = ProjectImporter.toProjects(parsed, colorOffset = allProjects.value.size)
            val added = repository.importProjects(projects, allProjects.value)
            onDone(added, parsed.size - added)
        }
    }

    // --- SAP export ---
    fun buildSapExport(type: SapExportType, roundToQuarter: Boolean, format: ExportFormat): String =
        SapExport.generate(type, periodReport.value, periodLabel.value, roundToQuarter, format)

    fun shareSapExport(type: SapExportType, format: ExportFormat, content: String, action: ExportAction = ExportAction.SHARE) {
        val stamp = DateFormats.yearMonth(_periodAnchor.value)
        val prefix = when (type) {
            SapExportType.MONTHLY_SUMMARY -> "sap_hours"
            SapExportType.DAILY_TIMESHEET -> "sap_timesheet"
            SapExportType.ATTENDANCE -> "attendance_overtime"
        }
        val filename = "${prefix}_${stamp}.${format.extension}"
        viewModelScope.launch {
            when (action) {
                ExportAction.SAVE -> fileExporter.saveText(content, filename, "text/csv")
                ExportAction.SHARE -> fileExporter.shareText(content, filename, "text/csv", "Export time report")
            }
        }
    }

    // --- Backup & restore (move all data to another device) ---
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    /** A backup the user picked, waiting for confirmation before it replaces the local data. */
    private val _pendingRestore = MutableStateFlow<BackupFile?>(null)
    val pendingRestore: StateFlow<BackupFile?> = _pendingRestore.asStateFlow()

    /** Outcome of the last backup action, shown once by the UI and then cleared. */
    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage.asStateFlow()
    fun clearBackupMessage() { _backupMessage.value = null }

    /** Writes every table to one JSON file and saves or shares it. */
    fun exportBackup(action: ExportAction) {
        val manager = backupManager ?: return
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val backup = manager.createBackup()
                val text = BackupCodec.encode(backup)
                val filename = BackupCodec.fileName(backup.exportedAt)
                when (action) {
                    ExportAction.SAVE -> fileExporter.saveText(text, filename, BackupCodec.MIME_TYPE)
                    ExportAction.SHARE -> fileExporter.shareText(text, filename, BackupCodec.MIME_TYPE, "WorkTracker backup")
                }
                _backupMessage.value = BackupMessage.Exported(backup.summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backupMessage.value = BackupMessage.Failed(BackupError.IO)
            } finally {
                _backupBusy.value = false
            }
        }
    }

    /** Opens the file picker; a readable backup becomes [pendingRestore] for the confirmation dialog. */
    fun pickBackupToRestore() {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val text = fileExporter.openText(BackupCodec.OPEN_MIME_TYPES, listOf(BackupCodec.FILE_EXTENSION)) ?: return@launch
                _pendingRestore.value = BackupCodec.decode(text)
            } catch (e: BackupException) {
                _backupMessage.value = BackupMessage.Failed(e.error)
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun cancelRestore() { _pendingRestore.value = null }

    /** Replaces all local data with [pendingRestore] and re-arms the deadline reminders for the restored tasks. */
    fun confirmRestore() {
        val manager = backupManager ?: return
        val backup = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val summary = manager.restore(backup)
                rescheduleTaskReminders()
                _backupMessage.value = BackupMessage.Restored(summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backupMessage.value = BackupMessage.Failed(BackupError.IO)
            } finally {
                _backupBusy.value = false
            }
        }
    }

    // --- Automatic backup into a user-chosen folder (Drive, iCloud, local) ---
    val autoBackupFolder: StateFlow<BackupFolder?> = autoBackup?.folder ?: MutableStateFlow(null)
    val autoBackupState: StateFlow<AutoBackupState> = autoBackup?.state ?: MutableStateFlow(AutoBackupState())
    /** Whether this platform can pick a folder at all (desktop and mobile yes, tests no). */
    val autoBackupAvailable: Boolean = autoBackup != null

    /** Opens the folder picker; once chosen, the first backup is written right away. */
    fun chooseAutoBackupFolder() {
        val auto = autoBackup ?: return
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                if (auto.pickFolder() != null) auto.backupNow()
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun disableAutoBackup() { autoBackup?.clearFolder() }

    fun runAutoBackupNow() {
        val auto = autoBackup ?: return
        viewModelScope.launch { auto.backupNow() }
    }

    /** Reads the file in the backup folder; it becomes [pendingRestore] for the usual confirmation. */
    fun restoreFromAutoBackup() {
        val auto = autoBackup ?: return
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val backup = auto.readLatest()
                if (backup == null) _backupMessage.value = BackupMessage.NoAutoBackup else _pendingRestore.value = backup
            } catch (e: BackupException) {
                _backupMessage.value = BackupMessage.Failed(e.error)
            } finally {
                _backupBusy.value = false
            }
        }
    }

    private suspend fun rescheduleTaskReminders() {
        val now = currentTimeMillis()
        repository.allTasksWithProject.first().forEach { task ->
            val deadline = task.deadlineTimestamp
            if (task.reminderEnabled && task.status != "DONE" && deadline != null && deadline > now) {
                reminderScheduler.scheduleTaskReminder(task)
            } else {
                reminderScheduler.cancelTaskReminder(task.id)
            }
        }
    }
}

/** Result of a backup export or restore, translated by the UI. */
sealed interface BackupMessage {
    data class Exported(val summary: BackupSummary) : BackupMessage
    data class Restored(val summary: BackupSummary) : BackupMessage
    data class Failed(val error: BackupError) : BackupMessage
    /** "Restore from folder" found no backup file there yet. */
    data object NoAutoBackup : BackupMessage
}

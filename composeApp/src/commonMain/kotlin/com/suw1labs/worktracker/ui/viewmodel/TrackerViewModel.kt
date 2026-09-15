package com.suw1labs.worktracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.suw1labs.worktracker.data.model.WorkCategory
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.data.import.ProjectImporter
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(
    private val repository: TimeTrackerRepository,
    private val reminderScheduler: ReminderScheduler = NoOpReminderScheduler,
    private val fileExporter: FileExporter = NoOpFileExporter
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
    val categories: StateFlow<List<WorkCategory>> = repository.allCategories.asState(emptyList())
    val allTasks: StateFlow<List<WorkTaskWithProject>> = repository.allTasksWithProject.asState(emptyList())
    val allEntries: StateFlow<List<TimeEntryWithDetails>> = repository.allTimeEntries.asState(emptyList())
    val allSessions: StateFlow<List<AttendanceSession>> = repository.allSessions.asState(emptyList())
    val projectSummaries: StateFlow<List<ProjectSummary>> = repository.projectSummaries.asState(emptyList())
    val dayRecords: StateFlow<List<DayRecord>> = repository.allDayRecords.asState(emptyList())
    val settings: StateFlow<AppSettings> = repository.settings.map { it ?: AppSettings() }
        .map { s ->
            // Keep date formatting in sync with the UI language.
            val t = com.suw1labs.worktracker.ui.i18n.Translations.forLanguage(com.suw1labs.worktracker.ui.i18n.Language.fromCode(s.language))
            com.suw1labs.worktracker.util.DateFormats.names = com.suw1labs.worktracker.util.DateNames(t.monthsShort, t.monthsLong, t.weekdaysShort)
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
                        return@collect
                    }
                    val now = currentTimeMillis()
                    val today = DateRanges.dayRange(now)
                    val iso = today.start.toLocalDate().dayOfWeek.isoDayNumber
                    val target = s.targetSecondsFor(iso)
                    val worked = ReportCalculator.attendanceSeconds(sessions, today, now)
                    val remaining = target - worked
                    if (target <= 0 || remaining <= 0) {
                        reminderScheduler.cancelDailyTargetReminder()
                    } else {
                        val t = com.suw1labs.worktracker.ui.i18n.Translations.forLanguage(com.suw1labs.worktracker.ui.i18n.Language.fromCode(s.language))
                        reminderScheduler.scheduleDailyTargetReminder(
                            triggerAtMillis = now + remaining * 1000L,
                            title = t.targetReachedTitle,
                            message = t.targetReachedBody(com.suw1labs.worktracker.util.TimeFormat.hoursMinutes(target))
                        )
                    }
                }
        }
    }

    // --- Attendance ---
    fun clockIn() {
        viewModelScope.launch {
            if (repository.getOpenSession() == null) {
                repository.insertSession(AttendanceSession(clockIn = currentTimeMillis()))
            }
        }
    }

    /** Clocks out; the running activity is stopped at the same moment so nothing is counted while away. */
    fun clockOut(reason: ClockOutReason? = null) {
        viewModelScope.launch {
            val now = currentTimeMillis()
            repository.closeRunningEntries(now)
            repository.closeOpenSessions(now, reason?.name)
        }
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
    fun startActivity(projectId: Long?, categoryId: Long?, taskId: Long? = null, description: String = "") {
        viewModelScope.launch {
            val now = currentTimeMillis()
            if (repository.getOpenSession() == null) {
                repository.insertSession(AttendanceSession(clockIn = now))
            }
            repository.closeRunningEntries(now)
            repository.insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    taskId = taskId,
                    categoryId = categoryId,
                    description = description,
                    startTime = now,
                    endTime = null
                )
            )
        }
    }

    fun startActivityFromTask(task: WorkTaskWithProject) {
        val defaultCategory = categories.value.firstOrNull { it.isProductive }
        startActivity(
            projectId = task.projectId,
            categoryId = defaultCategory?.id,
            taskId = task.id,
            description = task.title
        )
    }

    fun stopActivity() {
        viewModelScope.launch { repository.closeRunningEntries(currentTimeMillis()) }
    }

    fun addManualEntry(
        projectId: Long?,
        categoryId: Long?,
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
                    categoryId = categoryId,
                    description = description,
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }
    }

    fun updateEntry(entry: TimeEntry) {
        viewModelScope.launch { repository.updateTimeEntry(entry) }
    }

    fun deleteTimeEntry(entryId: Long) {
        viewModelScope.launch { repository.deleteTimeEntryById(entryId) }
    }

    // --- Projects & categories ---
    fun addProject(code: String, name: String, client: String, colorHex: String, budgetHours: Double) {
        viewModelScope.launch {
            repository.insertProject(
                Project(code = code.trim(), name = name.trim(), client = client.trim(), colorHex = colorHex, budgetHours = budgetHours)
            )
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch { repository.updateProject(project) }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch { repository.deleteProjectById(projectId) }
    }

    fun addCategory(name: String, isProductive: Boolean, colorHex: String) {
        viewModelScope.launch {
            val order = (categories.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            repository.insertCategory(WorkCategory(name = name.trim(), isProductive = isProductive, colorHex = colorHex, sortOrder = order))
        }
    }

    fun updateCategory(category: WorkCategory) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch { repository.deleteCategoryById(categoryId) }
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
}

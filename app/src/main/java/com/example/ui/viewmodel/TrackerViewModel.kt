package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.ExportManager
import com.example.data.model.Project
import com.example.data.model.ProjectSummary
import com.example.data.model.TimeEntry
import com.example.data.model.TimeEntryWithDetails
import com.example.data.model.WorkTask
import com.example.data.model.WorkTaskWithProject
import com.example.data.reminder.DeadlineReminderScheduler
import com.example.data.repository.TimeTrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class DailyWorkStat(
    val dayLabel: String,
    val dateEpoch: Long,
    val totalHours: Double,
    val billableHours: Double
)

data class ProjectWorkStat(
    val projectName: String,
    val projectColor: String,
    val client: String,
    val hours: Double,
    val percentage: Float,
    val earnings: Double
)

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val selectedProjectId: Long? = null,
    val selectedTaskId: Long? = null,
    val description: String = "",
    val isBillable: Boolean = true,
    val tags: String = ""
)

class TrackerViewModel(
    private val repository: TimeTrackerRepository
) : ViewModel() {

    // Database reactive streams
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProjects: StateFlow<List<Project>> = repository.activeProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<WorkTaskWithProject>> = repository.allTasksWithProject
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<TimeEntryWithDetails>> = repository.allTimeEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectSummaries: StateFlow<List<ProjectSummary>> = repository.projectSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Timer State
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // Reports period filter
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_WEEK)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val _selectedReportProjectId = MutableStateFlow<Long?>(null)
    val selectedReportProjectId: StateFlow<Long?> = _selectedReportProjectId.asStateFlow()

    // Tasks nearing deadlines (Urgent Deadline Alerts)
    val urgentTasks: StateFlow<List<WorkTaskWithProject>> = allTasks.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val now = System.currentTimeMillis()
        val seventyTwoHours = 72 * 3600 * 1000L
        tasks.filter {
            it.status != "DONE" &&
                    it.deadlineTimestamp != null &&
                    it.deadlineTimestamp <= (now + seventyTwoHours)
        }.sortedBy { it.deadlineTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for Reports
    val filteredReportEntries: StateFlow<List<TimeEntryWithDetails>> = combine(
        allEntries,
        _selectedPeriod,
        _selectedReportProjectId
    ) { entries, period, filterProjectId ->
        val (startTime, endTime) = getPeriodRange(period)
        entries.filter { entry ->
            val matchTime = entry.startTime >= startTime && entry.startTime <= endTime
            val matchProject = filterProjectId == null || entry.projectId == filterProjectId
            matchTime && matchProject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily breakdown for visual chart
    val dailyStats: StateFlow<List<DailyWorkStat>> = filteredReportEntries.combine(_selectedPeriod) { entries, period ->
        computeDailyStats(entries, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Project distribution breakdown for visual chart
    val projectDistribution: StateFlow<List<ProjectWorkStat>> = filteredReportEntries.combine(allProjects) { entries, _ ->
        val totalSecs = entries.sumOf { it.durationSeconds }.toDouble()
        if (totalSecs == 0.0) return@combine emptyList()

        entries.groupBy { it.projectId }.map { (projId, pEntries) ->
            val pSecs = pEntries.sumOf { it.durationSeconds }
            val first = pEntries.first()
            val pHours = pSecs / 3600.0
            val pEarnings = pEntries.filter { it.isBillable }.sumOf { (it.durationSeconds / 3600.0) * it.hourlyRate }
            ProjectWorkStat(
                projectName = first.projectName,
                projectColor = first.projectColor,
                client = first.client,
                hours = pHours,
                percentage = (pSecs / totalSecs).toFloat(),
                earnings = pEarnings
            )
        }.sortedByDescending { it.hours }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReportPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setReportProjectFilter(projectId: Long?) {
        _selectedReportProjectId.value = projectId
    }

    // --- TIMER CONTROL ---
    fun startTimer(
        projectId: Long,
        taskId: Long? = null,
        description: String = "",
        isBillable: Boolean = true,
        tags: String = ""
    ) {
        val now = System.currentTimeMillis()
        _timerState.value = TimerState(
            isRunning = true,
            isPaused = false,
            elapsedSeconds = 0L,
            startTime = now,
            selectedProjectId = projectId,
            selectedTaskId = taskId,
            description = description,
            isBillable = isBillable,
            tags = tags
        )
        startTicker()
    }

    fun pauseTimer() {
        if (_timerState.value.isRunning && !_timerState.value.isPaused) {
            timerJob?.cancel()
            _timerState.value = _timerState.value.copy(isPaused = true)
        }
    }

    fun resumeTimer() {
        if (_timerState.value.isRunning && _timerState.value.isPaused) {
            _timerState.value = _timerState.value.copy(isPaused = false)
            startTicker()
        }
    }

    fun stopTimer() {
        val current = _timerState.value
        if (!current.isRunning || current.selectedProjectId == null) return

        val duration = current.elapsedSeconds
        if (duration >= 5) { // Log entries of at least 5 seconds
            val now = System.currentTimeMillis()
            val start = now - (duration * 1000L)
            val project = allProjects.value.find { it.id == current.selectedProjectId }
            val rate = project?.hourlyRate ?: 0.0

            viewModelScope.launch {
                repository.insertTimeEntry(
                    TimeEntry(
                        projectId = current.selectedProjectId,
                        taskId = current.selectedTaskId,
                        description = current.description.ifBlank { "Work session" },
                        startTime = start,
                        endTime = now,
                        durationSeconds = duration,
                        isBillable = current.isBillable,
                        hourlyRate = rate,
                        tags = current.tags
                    )
                )
            }
        }

        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun discardTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    fun quickStartFromTask(task: WorkTaskWithProject) {
        startTimer(
            projectId = task.projectId,
            taskId = task.id,
            description = task.title,
            isBillable = true
        )
    }

    fun quickStartFromEntry(entry: TimeEntryWithDetails) {
        startTimer(
            projectId = entry.projectId,
            taskId = entry.taskId,
            description = entry.description,
            isBillable = entry.isBillable,
            tags = entry.tags
        )
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    elapsedSeconds = _timerState.value.elapsedSeconds + 1
                )
            }
        }
    }

    // --- PROJECT & TASK MANAGEMENT ---
    fun addProject(
        name: String,
        client: String,
        colorHex: String,
        hourlyRate: Double,
        budgetHours: Double
    ) {
        viewModelScope.launch {
            repository.insertProject(
                Project(
                    name = name,
                    client = client,
                    colorHex = colorHex,
                    hourlyRate = hourlyRate,
                    budgetHours = budgetHours
                )
            )
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
    }

    fun addTask(
        context: Context,
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
                DeadlineReminderScheduler.scheduleTaskReminder(
                    context,
                    WorkTaskWithProject(
                        id = taskId,
                        projectId = projectId,
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
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateTask(context: Context, task: WorkTask) {
        viewModelScope.launch {
            repository.updateTask(task)
            val project = allProjects.value.find { it.id == task.projectId }
            if (project != null) {
                DeadlineReminderScheduler.scheduleTaskReminder(
                    context,
                    WorkTaskWithProject(
                        id = task.id,
                        projectId = task.projectId,
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

    fun updateTaskStatus(context: Context, taskId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, newStatus)
            if (newStatus == "DONE") {
                DeadlineReminderScheduler.cancelTaskReminder(context, taskId)
            }
        }
    }

    fun deleteTask(context: Context, taskId: Long) {
        viewModelScope.launch {
            DeadlineReminderScheduler.cancelTaskReminder(context, taskId)
            val task = allTasks.value.find { it.id == taskId }
            if (task != null) {
                repository.deleteTask(
                    WorkTask(
                        id = task.id,
                        projectId = task.projectId,
                        title = task.title
                    )
                )
            }
        }
    }

    fun snoozeTask(context: Context, taskId: Long, addHours: Int) {
        val taskWithP = allTasks.value.find { it.id == taskId } ?: return
        val currentDeadline = taskWithP.deadlineTimestamp ?: System.currentTimeMillis()
        val newDeadline = currentDeadline + (addHours * 3600 * 1000L)

        viewModelScope.launch {
            val updated = WorkTask(
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
            repository.updateTask(updated)
            DeadlineReminderScheduler.scheduleTaskReminder(
                context,
                taskWithP.copy(deadlineTimestamp = newDeadline)
            )
        }
    }

    fun checkUpcomingDeadlines(context: Context) {
        DeadlineReminderScheduler.checkAndNotifyUrgentDeadlines(context, allTasks.value)
    }

    // --- MANUAL TIME ENTRY & DELETION ---
    fun addManualTimeEntry(
        projectId: Long,
        taskId: Long?,
        description: String,
        startTime: Long,
        endTime: Long,
        isBillable: Boolean,
        tags: String
    ) {
        val durationSecs = (endTime - startTime).coerceAtLeast(60L * 1000L) / 1000L
        val project = allProjects.value.find { it.id == projectId }
        val rate = project?.hourlyRate ?: 0.0

        viewModelScope.launch {
            repository.insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    taskId = taskId,
                    description = description.ifBlank { "Manual entry" },
                    startTime = startTime,
                    endTime = endTime,
                    durationSeconds = durationSecs,
                    isBillable = isBillable,
                    hourlyRate = rate,
                    tags = tags
                )
            )
        }
    }

    fun deleteTimeEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteTimeEntryById(entryId)
        }
    }

    // --- EXPORT FUNCTIONALITY ---
    fun exportTimeEntriesCsv(context: Context, filterProjectId: Long? = null) {
        viewModelScope.launch {
            val entries = if (filterProjectId == null) {
                allEntries.value
            } else {
                allEntries.value.filter { it.projectId == filterProjectId }
            }
            val csvContent = ExportManager.generateCsv(entries)
            val filename = "time_entries_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
            val file = ExportManager.saveExportFile(context, csvContent, filename)
            ExportManager.shareFile(context, file, "text/csv", "Export Time Entries (CSV)")
        }
    }

    fun exportExcelWorkbook(context: Context, filterProjectId: Long? = null) {
        viewModelScope.launch {
            val entries = if (filterProjectId == null) {
                allEntries.value
            } else {
                allEntries.value.filter { it.projectId == filterProjectId }
            }
            val projects = if (filterProjectId == null) {
                projectSummaries.value
            } else {
                projectSummaries.value.filter { it.id == filterProjectId }
            }
            val excelCsvContent = ExportManager.generateExcelCsv(entries, projects)
            val filename = "productivity_report_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
            val file = ExportManager.saveExportFile(context, excelCsvContent, filename)
            ExportManager.shareFile(context, file, "text/csv", "Export Excel / Spreadsheet Report")
        }
    }

    // --- DATE RANGE HELPERS ---
    private fun getPeriodRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        return when (period) {
            ReportPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            ReportPeriod.THIS_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            ReportPeriod.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    private fun computeDailyStats(
        entries: List<TimeEntryWithDetails>,
        period: ReportPeriod
    ): List<DailyWorkStat> {
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE d", Locale.getDefault())
        val daysToGenerate = when (period) {
            ReportPeriod.TODAY -> 1
            ReportPeriod.THIS_WEEK -> 7
            ReportPeriod.THIS_MONTH -> 14
            ReportPeriod.ALL_TIME -> 7
        }

        val result = mutableListOf<DailyWorkStat>()
        val baseCal = Calendar.getInstance()

        for (i in (daysToGenerate - 1) downTo 0) {
            cal.timeInMillis = baseCal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val dayEnd = cal.timeInMillis

            val dayEntries = entries.filter { it.startTime in dayStart..dayEnd }
            val totalSecs = dayEntries.sumOf { it.durationSeconds }
            val billableSecs = dayEntries.filter { it.isBillable }.sumOf { it.durationSeconds }

            result.add(
                DailyWorkStat(
                    dayLabel = dayFormat.format(Date(dayStart)),
                    dateEpoch = dayStart,
                    totalHours = totalSecs / 3600.0,
                    billableHours = billableSecs / 3600.0
                )
            )
        }

        return result
    }
}

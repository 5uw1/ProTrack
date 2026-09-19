package com.suw1labs.worktracker.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.suw1labs.worktracker.data.backup.BackupError
import com.suw1labs.worktracker.data.backup.BackupSummary
import com.suw1labs.worktracker.data.export.ExportFormat
import com.suw1labs.worktracker.data.export.SapExportType
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.report.ComplianceWarning
import com.suw1labs.worktracker.data.report.WarningKind
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.ReportPeriodType
import com.suw1labs.worktracker.util.TimeFormat

enum class Language(val code: String, val displayName: String) {
    EN("en", "English"),
    DE("de", "Deutsch"),
    FR("fr", "Français");

    companion object {
        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: EN
    }
}

/** All user-visible texts. One instance per language. */
class AppStrings(val language: Language) {
    // Every text is a lateinit property (the JVM allows at most 255 constructor parameters).
    // StringsCompletenessTest (desktop) checks that each language sets all of them.
    // common
    lateinit var cancel: String
    lateinit var save: String
    lateinit var add: String
    lateinit var edit: String
    lateinit var delete: String
    lateinit var close: String
    lateinit var next: String
    lateinit var back: String
    lateinit var ok: String
    lateinit var yes: String
    lateinit var no: String
    lateinit var monthsShort: List<String>
    lateinit var monthsLong: List<String>
    lateinit var weekdaysShort: List<String>
    lateinit var weekdaysTwo: List<String>
    lateinit var weekdaysLong: List<String>
    // tabs / app bar
    lateinit var tabToday: String
    lateinit var tabProjects: String
    lateinit var tabTasks: String
    lateinit var tabReports: String
    lateinit var badgeWorking: String
    lateinit var badgeClockedIn: String
    // today
    lateinit var clockedOut: String
    lateinit var clockedInSince: (String) -> String
    lateinit var clockedInToday: String
    lateinit var clockIn: String
    lateinit var clockOut: String
    lateinit var currentActivity: String
    lateinit var whatWorkingOn: String
    lateinit var searchProjectsTasks: String
    lateinit var focus: String
    lateinit var focusAll: String
    lateinit var focusOn: String
    lateinit var focusOff: String
    lateinit var focusHint: String
    lateinit var noMatches: String
    lateinit var switchActivity: String
    lateinit var switchShort: String
    lateinit var start: String
    lateinit var stop: String
    lateinit var noteOptional: String
    lateinit var notePlaceholder: String
    lateinit var autoClockInHint: String
    lateinit var absence: String
    lateinit var editAbsence: String
    lateinit var clockedIn: String
    lateinit var projectWork: String
    lateinit var unproductive: String
    lateinit var overtimeToday: String
    lateinit var activities: String
    lateinit var activitiesSubtitle: (Int) -> String
    lateinit var manualEntry: String
    lateinit var noActivitiesTitle: String
    lateinit var noActivitiesSubtitle: String
    lateinit var noProject: String
    lateinit var since: (String) -> String
    lateinit var running: String
    lateinit var deadlineAlerts: (Int) -> String
    lateinit var workOnIt: String
    lateinit var snooze: String
    lateinit var uncategorized: String
    lateinit var credited: String
    lateinit var fromOvertime: String
    lateinit var targetReachedTitle: String
    // still clocked in safety net + forgotten clock-out fix
    lateinit var stillClockedInTitle: String
    lateinit var stillClockedInBody: (String) -> String
    lateinit var clockOutAt: (String) -> String
    // month-end check (Reports, month scope)
    lateinit var monthEndCheck: String
    lateinit var readyToBook: String
    lateinit var thingsToFix: (Int) -> String
    lateinit var checkForgotClockOut: (Int) -> String
    lateinit var checkUnassigned: (String) -> String
    lateinit var checkRuleWarnings: (Int) -> String
    lateinit var checkRoundedTotal: (rounded: String, exact: String) -> String
    lateinit var checkOpenToday: String
    lateinit var targetReachedBody: (String) -> String
    // reports
    lateinit var sapExport: String
    lateinit var target: String
    lateinit var absencesCredited: String
    lateinit var overtime: String
    lateinit var balanceToDate: String
    lateinit var hoursPerProject: String
    lateinit var noProjectHours: String
    lateinit var total: String
    lateinit var workingTimeRules: String
    lateinit var absences: String
    lateinit var noneInPeriod: String
    lateinit var clockInPeriods: String
    lateinit var notClockedInOnDay: String
    lateinit var noActivitiesOnDay: String
    lateinit var breaks: String
    lateinit var previousMonth: String
    lateinit var nextMonth: String
    lateinit var previousWeek: String
    lateinit var nextWeek: String
    lateinit var stillClockedIn: String
    lateinit var warnBreak: (worked: String, brk: String, required: String) -> String
    lateinit var warnWeekMax: (weekStart: String, worked: String, max: String) -> String
    lateinit var warnForgotClockOut: (date: String) -> String
    // projects & setup
    lateinit var sapProjects: String
    lateinit var projectsSubtitle: String
    lateinit var importBtn: String
    lateinit var importedResult: (Int, Int) -> String
    lateinit var filterAll: String
    lateinit var filterActive: String
    lateinit var filterOnHold: String
    lateinit var filterCompleted: String
    lateinit var noProjectsTitle: String
    lateinit var noProjectsSubtitle: String
    lateinit var booked: String
    lateinit var tasks: String
    lateinit var done: String
    lateinit var newProject: String
    lateinit var editProject: String
    lateinit var deleteProject: String
    lateinit var projectNumber: String
    lateinit var projectNumberHint: String
    lateinit var projectName: String
    lateinit var customer: String
    lateinit var plannedHours: String
    lateinit var status: String
    lateinit var colourTag: String
    lateinit var addProject: String
    lateinit var workSchedule: String
    lateinit var editSchedule: String
    lateinit var workCategories: String
    lateinit var categoriesSubtitle: String
    lateinit var productive: String
    lateinit var unproductiveLabel: String
    lateinit var newCategory: String
    lateinit var editCategory: String
    lateinit var categoryName: String
    lateinit var categoryPlaceholder: String
    lateinit var productiveWork: String
    lateinit var unproductiveTime: String
    lateinit var productiveHint: String
    lateinit var unproductiveHint: String
    lateinit var colour: String
    lateinit var languageTitle: String
    lateinit var languageSubtitle: String
    lateinit var statusActive: String
    lateinit var statusOnHold: String
    lateinit var statusCompleted: String
    lateinit var hrsLogged: (String) -> String
    lateinit var budget: (String) -> String
    // schedule dialog
    lateinit var scheduleSubtitle: String
    lateinit var hoursPerWeekAt100: String
    lateinit var legalMaxPerWeek: String
    lateinit var hoursPerDay: String
    lateinit var hoursPerDayHint: String
    lateinit var schedulePreview: (weekly: String, percent: String, days: Int) -> String
    lateinit var checkValues: String
    lateinit var perWeek: String
    lateinit var perDay: String
    lateinit var maxPerWeek: String
    // absence dialog
    lateinit var bookAbsence: String
    lateinit var day: String
    lateinit var reason: String
    lateinit var reasonRequired: String
    lateinit var reasonPlaceholder: String
    lateinit var hoursFullDay: (String) -> String
    lateinit var creditsHint: String
    lateinit var fromOvertimeHint: String
    lateinit var enterHours: String
    // import dialog
    lateinit var importProjects: String
    lateinit var importDescription: String
    lateinit var projectList: String
    lateinit var importPlaceholder: String
    lateinit var noneRecognised: String
    lateinit var recognised: (Int, String) -> String
    lateinit var importN: (Int) -> String
    // entry dialog
    lateinit var logActivity: String
    lateinit var editActivity: String
    lateinit var sapProject: String
    lateinit var category: String
    lateinit var selectCategory: String
    lateinit var taskOptional: String
    lateinit var noSpecificTask: String
    lateinit var whatDidYouDo: String
    lateinit var startLabel: String
    lateinit var endLabel: String
    lateinit var pickStart: String
    lateinit var pickEnd: String
    lateinit var stillRunning: String
    lateinit var endAfterStart: String
    lateinit var duration: (String) -> String
    lateinit var noProjectOption: String
    lateinit var unproductiveSuffix: String
    lateinit var addProjectOption: String
    lateinit var unassignedProject: String
    lateinit var unassignedHint: String
    lateinit var addTaskOption: String
    lateinit var newTaskTitle: String
    lateinit var noteHint: String
    lateinit var moveTask: String
    lateinit var moveTaskTo: String
    lateinit var noTasksInProject: String
    lateinit var tasksCount: (Int) -> String
    lateinit var todayOnThisTask: String
    lateinit var thisSession: String
    lateinit var switchToThis: String
    lateinit var continueThis: String
    lateinit var addNote: String
    lateinit var editNote: String
    lateinit var untilNow: String
    lateinit var durationChip: (String) -> String
    lateinit var quickDuration: String
    // unassigned gaps and neighbour adjustment
    lateinit var noActivityLogged: String
    lateinit var assignGap: String
    lateinit var previousActivityEnds: (String, String) -> String
    lateinit var nextActivityStarts: (String, String) -> String
    lateinit var gapBetween: (String) -> String
    lateinit var overlapBetween: (String) -> String
    lateinit var moveNeighbourTo: (String) -> String
    lateinit var leaveGap: String
    lateinit var keepOverlap: String
    lateinit var neighbourWouldBeEmpty: String
    // session dialog
    lateinit var addPeriod: String
    lateinit var editPeriod: String
    lateinit var clockInLabel: String
    lateinit var clockOutLabel: String
    lateinit var pickTime: String
    lateinit var clockOutReason: String
    lateinit var clockOutAfterIn: String
    // export dialog
    lateinit var exportTitle: String
    lateinit var report: String
    lateinit var fileFormat: String
    lateinit var roundQuarter: String
    lateinit var preview: String
    lateinit var copy: String
    lateinit var copied: String
    lateinit var copiedToClipboard: String
    lateinit var saveShare: String
    lateinit var saveFile: String
    lateinit var shareFile: String
    lateinit var moreRows: String
    // tasks
    lateinit var taskFilterTodo: String
    lateinit var taskFilterInProgress: String
    lateinit var taskFilterDone: String
    lateinit var noTasksTitle: String
    lateinit var noTasksSubtitle: String
    lateinit var toggleComplete: String
    lateinit var track: String
    lateinit var bookedPlanned: (String, String) -> String
    lateinit var newTask: String
    lateinit var editTask: String
    lateinit var taskTitle: String
    lateinit var descriptionNotes: String
    lateinit var priority: String
    lateinit var estHours: String
    lateinit var deadlineReminders: String
    lateinit var noDeadline: String
    lateinit var setDeadline: String
    lateinit var saveTask: String
    lateinit var deadlineTime: String
    lateinit var selectProject: String
    lateinit var addTask: String
    lateinit var priorityLow: String
    lateinit var priorityMedium: String
    lateinit var priorityHigh: String
    lateinit var priorityUrgent: String
    lateinit var statusTodo: String
    lateinit var statusInProgress: String
    lateinit var statusReview: String
    lateinit var statusDone: String
    lateinit var overdueDays: (Int) -> String
    lateinit var overdueHours: (Int) -> String
    lateinit var dueUnder2h: String
    lateinit var dueInHours: (Int) -> String
    lateinit var dueTomorrow: String
    lateinit var dueOn: (String) -> String
    fun absenceLabel(type: AbsenceType): String = when (type) {
        AbsenceType.SICK -> absenceSick
        AbsenceType.HOLIDAY -> absenceHoliday
        AbsenceType.PUBLIC_HOLIDAY -> absencePublicHoliday
        AbsenceType.COMPENSATION -> absenceCompensation
        AbsenceType.EDUCATION -> absenceEducation
        AbsenceType.OTHER -> absenceOther
    }
    var absenceSick = ""; var absenceHoliday = ""; var absencePublicHoliday = ""
    var absenceCompensation = ""; var absenceEducation = ""; var absenceOther = ""

    fun reasonLabel(reason: ClockOutReason): String = when (reason) {
        ClockOutReason.LUNCH -> reasonLunch
        ClockOutReason.BREAK -> reasonBreak
        ClockOutReason.OUT -> reasonOut
        ClockOutReason.END_OF_DAY -> reasonHome
    }
    var reasonLunch = ""; var reasonBreak = ""; var reasonOut = ""; var reasonHome = ""

    fun periodLabel(type: ReportPeriodType): String = when (type) {
        ReportPeriodType.DAY -> periodDay
        ReportPeriodType.WEEK -> periodWeek
        ReportPeriodType.MONTH -> periodMonth
    }
    var periodDay = ""; var periodWeek = ""; var periodMonth = ""

    fun exportTypeLabel(type: SapExportType): String = when (type) {
        SapExportType.SAP_WEEK -> exportSapWeek
        SapExportType.MONTHLY_SUMMARY -> exportSummary
        SapExportType.DAILY_TIMESHEET -> exportTimesheet
        SapExportType.ATTENDANCE -> exportAttendance
    }
    var exportSummary = ""; var exportTimesheet = ""; var exportAttendance = ""
    lateinit var exportSapWeek: String
    lateinit var sapWeekHint: String
    lateinit var sapSettingsTitle: String
    lateinit var sapSettingsSubtitle: String
    lateinit var sapProductiveType: String
    lateinit var sapUnproductiveType: String
    lateinit var sapUnproductiveNumber: String

    fun formatLabel(format: ExportFormat): String = when (format) {
        ExportFormat.CSV -> formatCsv
        ExportFormat.EXCEL_CSV -> formatExcel
    }
    var formatCsv = ""; var formatExcel = ""

    // Delete / undo / target progress. Body fields: the constructor is at the JVM limit of 255 parameters.
    var undo = ""; var activityDeleted = ""; var moreActions = ""
    var deleteProjectQuestion: (String) -> String = { it }; var deleteProjectWarning = ""
    var deleteTaskQuestion: (String) -> String = { it }; var deleteTaskWarning = ""
    var deletePeriodQuestion = ""; var deleteAbsenceQuestion = ""
    var targetLabel: (String) -> String = { it }; var remainingToTarget: (String) -> String = { it }; var targetReached = ""
    // Clock-in / clock-out events in the day's activity list.
    var eventClockedIn = ""; var eventClockedOut = ""
    // Today summary breakdown.
    var noActivity = ""; var generalTaskTime = ""; var clockInFirst = ""
    // Paid short breaks (company rule).
    var paidBreak = ""; var paidBreakPerDay = ""; var paidBreakHint = ""; var paidBreakSummary: (String) -> String = { it }
    // break rules
    lateinit var breakRulesTitle: String
    lateinit var breakRulesHint: String
    lateinit var ruleAfterHours: String
    lateinit var ruleBreakMinutes: String
    lateinit var addRule: String
    lateinit var deductMissingBreak: String
    lateinit var deductMissingBreakHint: String
    lateinit var missingBreakDeducted: String
    lateinit var breakRuleSummary: (String, String) -> String
    lateinit var warnBreakDeducted: (String) -> String
    // Backup & transfer to another device.
    var backupTitle = ""; var backupSubtitle = ""; var backupShare = ""; var backupRestore = ""
    var backupExported = ""; var backupRestored = ""; var backupContents: (BackupSummary) -> String = { "" }
    var restoreQuestion: (String) -> String = { it }; var restoreWarning = ""; var restoreConfirm = ""
    var backupErrorNotBackup = ""; var backupErrorNewer = ""; var backupErrorCorrupt = ""; var backupErrorIo = ""
    // automatic backup into a chosen folder
    lateinit var autoBackupTitle: String
    lateinit var autoBackupSubtitle: String
    lateinit var autoBackupOff: String
    lateinit var autoBackupFolder: (String) -> String
    lateinit var autoBackupLast: (String) -> String
    lateinit var autoBackupPending: String
    lateinit var autoBackupFailed: String
    lateinit var autoBackupNoFile: String
    lateinit var chooseFolder: String
    lateinit var changeFolder: String
    lateinit var turnOff: String
    lateinit var backupNow: String
    lateinit var restoreFromFolder: String
    // import from other apps
    lateinit var licensesTitle: String
    lateinit var licensesSubtitle: String
    lateinit var licensesIntro: String
    lateinit var licensesApacheNotice: String
    lateinit var importAppsTitle: String
    lateinit var importAppsSubtitle: String
    lateinit var importWorkButton: String
    lateinit var workImportTitle: String
    lateinit var workImportSummary: (sessions: Int, entries: Int, absences: Int, projects: Int, tasks: Int) -> String
    lateinit var workImportSkipped: (Int) -> String
    lateinit var workImportNothing: String
    lateinit var workImportConfirm: String
    lateinit var workImported: (sessions: Int, entries: Int) -> String
    lateinit var notWorkExport: String

    fun backupError(error: BackupError): String = when (error) {
        BackupError.NOT_A_BACKUP -> backupErrorNotBackup
        BackupError.NEWER_FORMAT -> backupErrorNewer
        BackupError.CORRUPT -> backupErrorCorrupt
        BackupError.IO -> backupErrorIo
    }

    fun projectStatus(status: String): String = when (status) {
        "ACTIVE" -> statusActive
        "ON_HOLD" -> statusOnHold
        "COMPLETED" -> statusCompleted
        else -> status
    }

    fun priorityName(priority: String): String = when (priority.uppercase()) {
        "URGENT" -> priorityUrgent
        "HIGH" -> priorityHigh
        "MEDIUM" -> priorityMedium
        else -> priorityLow
    }

    fun taskStatus(status: String): String = when (status.uppercase()) {
        "DONE" -> statusDone
        "IN_PROGRESS" -> statusInProgress
        "REVIEW" -> statusReview
        else -> statusTodo
    }

    fun warning(w: ComplianceWarning): String = when (w.kind) {
        WarningKind.BREAK_TOO_SHORT -> warnBreak(
            TimeFormat.hoursMinutes(w.workedSeconds), TimeFormat.hoursMinutes(w.breakSeconds), TimeFormat.hoursMinutes(w.requiredBreakSeconds)
        ) + if (w.deductedSeconds > 0) " " + warnBreakDeducted(TimeFormat.hoursMinutes(w.deductedSeconds)) else ""
        WarningKind.WEEK_OVER_LEGAL_MAX -> warnWeekMax(DateFormats.monthDay(w.dayStart), TimeFormat.hoursMinutes(w.workedSeconds), TimeFormat.sapHours(w.maxWeeklyHours))
        WarningKind.STILL_CLOCKED_IN_PAST_DAY -> warnForgotClockOut(DateFormats.monthDay(w.dayStart))
    }
}

val LocalStrings = staticCompositionLocalOf { Translations.EN }

/** Current translation for the composition. */
val strings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

object Translations {
    fun forLanguage(language: Language): AppStrings = when (language) {
        Language.EN -> EN
        Language.DE -> DE
        Language.FR -> FR
    }

    val EN: AppStrings = AppStrings(Language.EN).apply {
        cancel = "Cancel"
        save = "Save"
        add = "Add"
        edit = "Edit"
        delete = "Delete"
        close = "Close"
        next = "Next"
        back = "Back"
        ok = "OK"
        yes = "Yes"
        no = "No"
        monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthsLong = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        weekdaysTwo = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        weekdaysLong = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        tabToday = "Today"
        tabProjects = "Settings"
        tabTasks = "Projects"
        tabReports = "Reports"
        badgeWorking = "WORKING"
        badgeClockedIn = "CLOCKED IN"
        clockedOut = "CLOCKED OUT"
        clockedInSince = { "CLOCKED IN since $it" }
        clockedInToday = "clocked in today"
        clockIn = "CLOCK IN"
        clockOut = "CLOCK OUT"
        currentActivity = "CURRENT ACTIVITY"
        whatWorkingOn = "WHAT ARE YOU WORKING ON?"
        searchProjectsTasks = "Search project or task…"
        focus = "Focus"
        focusAll = "All"
        focusOn = "Add to focus"
        focusOff = "Remove from focus"
        focusHint = "Star the projects you work on these days: Today then offers only those."
        noMatches = "No project or task matches."
        switchActivity = "SWITCH ACTIVITY"
        switchShort = "Switch"
        start = "Start"
        stop = "Stop"
        noteOptional = "Note (optional)"
        notePlaceholder = "e.g. Spindle PLC safety logic, HMI screen…"
        autoClockInHint = "Starting an activity clocks you in automatically."
        absence = "Absence"
        editAbsence = "Edit absence"
        clockedIn = "Clocked in"
        projectWork = "Project work"
        unproductive = "Unproductive"
        overtimeToday = "Overtime today"
        activities = "Activities"
        activitiesSubtitle = { "$it today · edit to adjust project or times" }
        manualEntry = "Manual entry"
        noActivitiesTitle = "No activities yet"
        noActivitiesSubtitle = "Clock in and start an activity, or log one manually."
        noProject = "No project"
        since = { "since $it" }
        running = "running"
        deadlineAlerts = { "Deadline alerts ($it)" }
        workOnIt = "Work on it"
        snooze = "Snooze +24h"
        uncategorized = "Uncategorized"
        credited = "credited"
        fromOvertime = "from overtime"
        targetReachedTitle = "Daily target reached 🎉"
        stillClockedInTitle = "Still clocked in?"
        stillClockedInBody = { "You have been clocked in for over $it. Forgot to clock out?" }
        clockOutAt = { "Clock out at $it" }
        monthEndCheck = "Month-end check"
        readyToBook = "Ready to book into your timesheet"
        thingsToFix = { if (it == 1) "1 thing to fix before booking" else "$it things to fix before booking" }
        checkForgotClockOut = { if (it == 1) "1 day without clock-out" else "$it days without clock-out" }
        checkUnassigned = { "$it of work without a project – assign it in the day view" }
        checkRuleWarnings = { if (it == 1) "1 working-time warning" else "$it working-time warnings" }
        checkRoundedTotal = { rounded, exact -> "Project hours rounded: $rounded h (exact $exact h)" }
        checkOpenToday = "Today is still open – the month is complete once you have clocked out."
        targetReachedBody = { "You have clocked in $it today. Time to go home?" }
        sapExport = "Export"
        target = "Target"
        absencesCredited = "Absences credited"
        overtime = "Overtime"
        balanceToDate = "Balance to date"
        hoursPerProject = "Hours per project"
        noProjectHours = "No project hours in this period."
        total = "Total"
        workingTimeRules = "Working-time rules"
        absences = "Absences"
        noneInPeriod = "None in this period."
        clockInPeriods = "Clock-in periods"
        notClockedInOnDay = "Not clocked in on this day."
        noActivitiesOnDay = "No activities on this day."
        breaks = "Breaks"
        previousMonth = "Previous month"
        nextMonth = "Next month"
        previousWeek = "Previous week"
        nextWeek = "Next week"
        stillClockedIn = "still clocked in"
        warnBreak = { worked, brk, required -> "Worked $worked with only $brk break – at least $required required." }
        warnWeekMax = { week, worked, max -> "Week of $week: $worked worked – legal maximum is $max h." }
        warnForgotClockOut = { date -> "Forgot to clock out on $date – fix the clock-in period." }
        sapProjects = "Projects"
        projectsSubtitle = "Project numbers and names from project management, plus the built-in Unproductive project."
        importBtn = "Import"
        importedResult = { added, skipped -> "Imported $added project(s)" + if (skipped > 0) ", $skipped already existed." else "." }
        filterAll = "All"
        filterActive = "Active"
        filterOnHold = "On hold"
        filterCompleted = "Completed"
        noProjectsTitle = "No projects yet"
        noProjectsSubtitle = "Tap + to add the projects you book hours on."
        booked = "BOOKED"
        tasks = "TASKS"
        done = "done"
        newProject = "New project"
        editProject = "Edit project"
        deleteProject = "Delete project"
        projectNumber = "Project number *"
        projectNumberHint = "e.g. P-2026-0142"
        projectName = "Project name *"
        customer = "Customer / machine (optional)"
        plannedHours = "Planned hours"
        status = "Status"
        colourTag = "Colour tag"
        addProject = "Add project"
        workSchedule = "Work schedule"
        editSchedule = "Edit schedule"
        workCategories = "Work categories"
        categoriesSubtitle = "PLC, High Level Language, Meeting… Unproductive ones are reported separately."
        productive = "Productive"
        unproductiveLabel = "Unproductive"
        newCategory = "New work category"
        editCategory = "Edit category"
        categoryName = "Name *"
        categoryPlaceholder = "e.g. PLC, Meeting, Coffee break"
        productiveWork = "Productive project"
        unproductiveTime = "Unproductive project"
        productiveHint = "Hours count as project work and go into the timesheet export"
        unproductiveHint = "Time is recorded but reported separately (meetings, breaks, …)"
        colour = "Colour"
        languageTitle = "Language"
        languageSubtitle = "App language"
        statusActive = "Active"
        statusOnHold = "On hold"
        statusCompleted = "Completed"
        hrsLogged = { "$it hrs logged" }
        budget = { "Budget: $it hrs" }
        scheduleSubtitle = "Hours per weekday – e.g. 3 days + education, 80 % with a day off, 90 % with a half day."
        hoursPerWeekAt100 = "Hours / week at 100 %"
        legalMaxPerWeek = "Legal max h / week"
        hoursPerDay = "Target hours per day"
        hoursPerDayHint = "0 = day off"
        schedulePreview = { weekly, percent, days -> "$weekly h per week ($percent %) · $days working days" }
        checkValues = "Please check the values."
        perWeek = "h/week"
        perDay = "h/day"
        maxPerWeek = "max"
        bookAbsence = "Book absence"
        day = "Day"
        reason = "Reason"
        reasonRequired = "Reason *"
        reasonPlaceholder = "e.g. Military service, doctor"
        hoursFullDay = { "Hours (full day = $it)" }
        creditsHint = "Counts towards the weekly target (paid absence)."
        fromOvertimeHint = "Taken from your overtime balance – no hours are credited."
        enterHours = "Enter the hours, e.g. 8 or 4."
        importProjects = "Import projects"
        importDescription = "Paste one project per line: number, name and optionally customer – separated by tab, ';' or ','. Copying cells from a spreadsheet works directly. Existing project numbers are skipped."
        projectList = "Project list"
        importPlaceholder = "P-2026-0142;Spindle retrofit;Customer AG\nP-2026-0150;New HMI"
        noneRecognised = "No projects recognised yet."
        recognised = { n, list -> "$n project(s) recognised: $list" }
        importN = { "Import $it" }
        logActivity = "Log activity"
        editActivity = "Edit activity"
        sapProject = "Project"
        category = "Task"
        selectCategory = "Select task"
        taskOptional = "Task (optional)"
        noSpecificTask = "General"
        whatDidYouDo = "What did you do?"
        startLabel = "Start"
        endLabel = "End"
        pickStart = "Pick start"
        pickEnd = "Pick end"
        stillRunning = "This activity is still running."
        endAfterStart = "End must be after start."
        duration = { "Duration: $it" }
        noProjectOption = "No project yet / not in app"
        unproductiveSuffix = "(unproductive)"
        addProjectOption = "+ Add new project…"
        unassignedProject = "Project not assigned yet"
        unassignedHint = "Edit the entry to assign a project or task later."
        addTaskOption = "+ Add new task…"
        newTaskTitle = "New task"
        noteHint = "Note – add it now, while working or afterwards"
        moveTask = "Move to another project"
        moveTaskTo = "Move task to"
        noTasksInProject = "No tasks yet"
        tasksCount = { "$it tasks" }
        todayOnThisTask = "today on this task"
        thisSession = "this session"
        switchToThis = "Switch to this activity"
        continueThis = "Continue this activity"
        addNote = "Add note"
        editNote = "Edit note"
        untilNow = "Until now"
        durationChip = { "+$it" }
        quickDuration = "Duration"
        noActivityLogged = "No activity logged"
        assignGap = "Assign"
        previousActivityEnds = { name, time -> "Before: $name (ends $time)" }
        nextActivityStarts = { name, time -> "After: $name (starts $time)" }
        gapBetween = { "Leaves $it with no activity." }
        overlapBetween = { "Overlaps it by $it." }
        moveNeighbourTo = { "Move it to $it" }
        leaveGap = "Leave gap, assign later"
        keepOverlap = "Keep as is"
        neighbourWouldBeEmpty = "Moving it would leave it with no time; edit that activity instead."
        addPeriod = "Add clock-in period"
        editPeriod = "Edit clock-in period"
        clockInLabel = "Clock in"
        clockOutLabel = "Clock out"
        pickTime = "Pick time"
        clockOutReason = "Clock-out reason"
        clockOutAfterIn = "Clock out must be after clock in."
        exportTitle = "Hours export"
        report = "Report"
        fileFormat = "File format"
        roundQuarter = "Round to 0.25 h for booking (otherwise exact, e.g. 7.78)"
        preview = "Preview"
        copy = "Copy"
        copied = "Copied!"
        copiedToClipboard = "Copied to clipboard!"
        saveShare = "Save / share CSV"
        saveFile = "Save file"
        shareFile = "Share (e-mail, chat, …)"
        moreRows = "... [more rows]"
        taskFilterTodo = "To-Do"
        taskFilterInProgress = "In progress"
        taskFilterDone = "Done"
        noTasksTitle = "No tasks found"
        noTasksSubtitle = "Add the tasks your project manager assigned, with deadlines and reminders."
        toggleComplete = "Toggle complete"
        track = "Track"
        bookedPlanned = { b, p -> "${b}h booked / ${p}h planned" }
        newTask = "New project task"
        editTask = "Edit task"
        taskTitle = "Task title *"
        descriptionNotes = "Description & notes"
        priority = "Priority"
        estHours = "Est. hours"
        deadlineReminders = "Deadline & automated reminders"
        noDeadline = "No deadline set"
        setDeadline = "Set deadline"
        saveTask = "Save task"
        deadlineTime = "Deadline time"
        selectProject = "Select project"
        addTask = "Add task"
        priorityLow = "Low"
        priorityMedium = "Med"
        priorityHigh = "High"
        priorityUrgent = "Urgent"
        statusTodo = "Todo"
        statusInProgress = "In progress"
        statusReview = "Review"
        statusDone = "Done"
        overdueDays = { "${it}d overdue" }
        overdueHours = { "${it}h overdue" }
        dueUnder2h = "Due in < 2h!"
        dueInHours = { "Due in ${it}h" }
        dueTomorrow = "Due tomorrow"
        dueOn = { "Due $it" }
        absenceSick = "Sick"; absenceHoliday = "Holiday (own vacation)"; absencePublicHoliday = "Public holiday (paid by company)"
        absenceCompensation = "Compensation (from overtime)"; absenceEducation = "Education / training"; absenceOther = "Other reason"
        reasonLunch = "Lunch"; reasonBreak = "Break"; reasonOut = "Out of office"; reasonHome = "Go home"
        periodDay = "Day"; periodWeek = "Week"; periodMonth = "Month"
        exportSummary = "Summary per project"; exportTimesheet = "Daily timesheet"; exportAttendance = "Attendance & overtime"
        exportSapWeek = "Weekly sheet (copy & paste)"
        sapWeekHint = "Tab-separated, one block per calendar week, Monday first: copy it and paste it into your weekly time sheet. Hours have two decimals; empty cells mean nothing to book. Lines starting with ! are time without a project – book it manually or assign it first."
        sapSettingsTitle = "Timesheet booking"
        sapSettingsSubtitle = "Activity types and the cost object used by the weekly copy & paste export."
        sapProductiveType = "Activity type – project work"
        sapUnproductiveType = "Activity type – unproductive"
        sapUnproductiveNumber = "Cost object for unproductive hours"
        undo = "Undo"; activityDeleted = "Activity deleted"; moreActions = "More actions"
        deleteProjectQuestion = { "Delete project $it?" }
        deleteProjectWarning = "Its tasks are deleted. Time already booked on it is kept but loses its project and must be re-assigned."
        deleteTaskQuestion = { "Delete task \"$it\"?" }
        deleteTaskWarning = "Time booked on this task is kept but loses its task."
        deletePeriodQuestion = "Delete this clock-in period? The clocked-in time and overtime of the day change."
        deleteAbsenceQuestion = "Delete this absence? Its hours are no longer credited."
        targetLabel = { "Target $it" }; remainingToTarget = { "$it to go" }; targetReached = "Target reached"
        eventClockedIn = "Clocked in"; eventClockedOut = "Clocked out"
        noActivity = "No activity running"; generalTaskTime = "No specific task"
        clockInFirst = "Clock in first, then choose what you are working on."
        paidBreak = "Paid breaks"; paidBreakPerDay = "Paid break per day (min)"
        paidBreakHint = "Coffee / smoke breaks tagged as Break count as working time up to this many minutes a day. 0 = unpaid."
        paidBreakSummary = { "$it min paid break" }
        breakRulesTitle = "Required breaks"
        breakRulesHint = "Rest break a day must contain once this much time was clocked in. Lunch and breaks together count."
        ruleAfterHours = "More than (h)"
        ruleBreakMinutes = "Break (min)"
        addRule = "Add rule"
        deductMissingBreak = "Deduct a missing break automatically"
        deductMissingBreakHint = "If less break was taken than required, the difference is taken off the counted working time: 9 h 30 clocked in with 30 min break and 1 h required counts as 9 h."
        missingBreakDeducted = "− Missing break"
        breakRuleSummary = { hours, minutes -> "$minutes min > $hours h" }
        warnBreakDeducted = { "$it deducted." }
        formatCsv = "CSV (comma)"; formatExcel = "Spreadsheet (semicolon)"
        backupTitle = "Backup & transfer"; backupSubtitle = "Save everything (projects, tasks, times, absences, settings) as one file and restore it on another device."
        backupShare = "Share"; backupRestore = "Restore from file"
        backupExported = "Backup created"; backupRestored = "Backup restored"
        backupContents = { s -> "${s.projects} projects · ${s.tasks} tasks · ${s.timeEntries} activities · ${s.attendanceSessions} clock-in periods · ${s.dayRecords} absences" }
        restoreQuestion = { "Restore backup from $it?" }
        restoreWarning = "All data on this device is replaced by the backup. This cannot be undone – export a backup of this device first if in doubt."
        restoreConfirm = "Replace & restore"
        backupErrorNotBackup = "This file is not a WorkTracker backup."
        backupErrorNewer = "The backup was made with a newer app version. Update the app, then try again."
        backupErrorCorrupt = "The backup file is damaged and cannot be read."
        backupErrorIo = "The file could not be read or written."
        autoBackupTitle = "Automatic backup"
        autoBackupSubtitle = "Keeps a copy of everything in a folder you choose – Google Drive, iCloud Drive or a folder on this device – a few seconds after every change. After reinstalling, restore from that folder."
        autoBackupOff = "Off – no folder chosen"
        autoBackupFolder = { "Folder: $it" }
        autoBackupLast = { "Last backup $it" }
        autoBackupPending = "Not written yet"
        autoBackupFailed = "The last backup could not be written. Check that the folder still exists, or choose it again."
        autoBackupNoFile = "No backup file in that folder yet."
        chooseFolder = "Choose folder"
        changeFolder = "Change"
        turnOff = "Turn off"
        backupNow = "Back up now"
        restoreFromFolder = "Restore from folder"
        licensesTitle = "Open source licenses"
        licensesSubtitle = "The libraries this app is built with, and their terms."
        licensesIntro = "WorkTracker is built with open source software. The libraries below ship inside the app; their copyright stays with their authors."
        licensesApacheNotice = "Licensed under the Apache License, Version 2.0. You may not use these files except in compliance with the License; a copy is available at:"
        importAppsTitle = "Import from other apps"
        importAppsSubtitle = "Bring your history over. Supported: the CSV export of the iOS app WORK (Export → CSV). Days already present are skipped."
        importWorkButton = "WORK export (CSV)"
        workImportTitle = "Import WORK export"
        workImportSummary = { s, e, a, p, t -> "$s clock-in periods · $e activities · $a absences" + if (p > 0 || t > 0) " · creates $p projects and $t tasks" else "" }
        workImportSkipped = { "$it rows already exist and are skipped." }
        workImportNothing = "Nothing new to import."
        workImportConfirm = "Import"
        workImported = { s, e -> "Imported $s clock-in periods and $e activities" }
        notWorkExport = "This file is not a WORK export."
    }

    val DE: AppStrings = AppStrings(Language.DE).apply {
        cancel = "Abbrechen"
        save = "Speichern"
        add = "Hinzufügen"
        edit = "Bearbeiten"
        delete = "Löschen"
        close = "Schliessen"
        next = "Weiter"
        back = "Zurück"
        ok = "OK"
        yes = "Ja"
        no = "Nein"
        monthsShort = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        monthsLong = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        weekdaysShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        weekdaysTwo = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        weekdaysLong = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        tabToday = "Heute"
        tabProjects = "Einstellungen"
        tabTasks = "Projekte"
        tabReports = "Berichte"
        badgeWorking = "ARBEITET"
        badgeClockedIn = "EINGESTEMPELT"
        clockedOut = "AUSGESTEMPELT"
        clockedInSince = { "EINGESTEMPELT seit $it" }
        clockedInToday = "heute eingestempelt"
        clockIn = "EINSTEMPELN"
        clockOut = "AUSSTEMPELN"
        currentActivity = "AKTUELLE TÄTIGKEIT"
        whatWorkingOn = "WORAN ARBEITEST DU?"
        searchProjectsTasks = "Projekt oder Aufgabe suchen…"
        focus = "Fokus"
        focusAll = "Alle"
        focusOn = "Zum Fokus hinzufügen"
        focusOff = "Aus dem Fokus entfernen"
        focusHint = "Markiere die Projekte, an denen du zurzeit arbeitest: Heute bietet dann nur diese an."
        noMatches = "Kein Projekt und keine Aufgabe passt."
        switchActivity = "TÄTIGKEIT WECHSELN"
        switchShort = "Wechseln"
        start = "Start"
        stop = "Stopp"
        noteOptional = "Notiz (optional)"
        notePlaceholder = "z.B. SPS Sicherheitslogik Spindel, HMI-Bild…"
        autoClockInHint = "Beim Starten einer Tätigkeit wird automatisch eingestempelt."
        absence = "Abwesenheit"
        editAbsence = "Abwesenheit bearbeiten"
        clockedIn = "Eingestempelt"
        projectWork = "Projektarbeit"
        unproductive = "Unproduktiv"
        overtimeToday = "Überzeit heute"
        activities = "Tätigkeiten"
        activitiesSubtitle = { "$it heute · bearbeiten, um Projekt oder Zeiten anzupassen" }
        manualEntry = "Manueller Eintrag"
        noActivitiesTitle = "Noch keine Tätigkeiten"
        noActivitiesSubtitle = "Einstempeln und Tätigkeit starten oder manuell erfassen."
        noProject = "Kein Projekt"
        since = { "seit $it" }
        running = "läuft"
        deadlineAlerts = { "Termin-Warnungen ($it)" }
        workOnIt = "Daran arbeiten"
        snooze = "+24h verschieben"
        uncategorized = "Nicht zugeordnet"
        credited = "gutgeschrieben"
        fromOvertime = "aus Überzeit"
        targetReachedTitle = "Tagessoll erreicht 🎉"
        stillClockedInTitle = "Noch eingestempelt?"
        stillClockedInBody = { "Du bist seit über $it eingestempelt. Ausstempeln vergessen?" }
        clockOutAt = { "Um $it ausstempeln" }
        monthEndCheck = "Monatsabschluss-Check"
        readyToBook = "Bereit zum Buchen im Stundenrapport"
        thingsToFix = { if (it == 1) "1 Punkt vor dem Buchen korrigieren" else "$it Punkte vor dem Buchen korrigieren" }
        checkForgotClockOut = { if (it == 1) "1 Tag ohne Ausstempeln" else "$it Tage ohne Ausstempeln" }
        checkUnassigned = { "$it Arbeit ohne Projekt – in der Tagesansicht zuordnen" }
        checkRuleWarnings = { if (it == 1) "1 Arbeitszeit-Warnung" else "$it Arbeitszeit-Warnungen" }
        checkRoundedTotal = { rounded, exact -> "Projektstunden gerundet: $rounded h (exakt $exact h)" }
        checkOpenToday = "Heute ist noch offen – der Monat ist komplett, sobald du ausgestempelt hast."
        targetReachedBody = { "Du bist heute $it eingestempelt. Zeit für den Feierabend?" }
        sapExport = "Export"
        target = "Soll"
        absencesCredited = "Abwesenheiten gutgeschrieben"
        overtime = "Überzeit"
        balanceToDate = "Saldo bis heute"
        hoursPerProject = "Stunden pro Projekt"
        noProjectHours = "Keine Projektstunden in diesem Zeitraum."
        total = "Total"
        workingTimeRules = "Arbeitszeitregeln"
        absences = "Abwesenheiten"
        noneInPeriod = "Keine in diesem Zeitraum."
        clockInPeriods = "Stempelzeiten"
        notClockedInOnDay = "An diesem Tag nicht eingestempelt."
        noActivitiesOnDay = "Keine Tätigkeiten an diesem Tag."
        breaks = "Pausen"
        previousMonth = "Vorheriger Monat"
        nextMonth = "Nächster Monat"
        previousWeek = "Vorherige Woche"
        nextWeek = "Nächste Woche"
        stillClockedIn = "noch eingestempelt"
        warnBreak = { worked, brk, required -> "$worked gearbeitet mit nur $brk Pause – mindestens $required erforderlich." }
        warnWeekMax = { week, worked, max -> "Woche vom $week: $worked gearbeitet – gesetzliches Maximum ist $max h." }
        warnForgotClockOut = { date -> "Am $date nicht ausgestempelt – Stempelzeit korrigieren." }
        sapProjects = "Projekte"
        projectsSubtitle = "Projektnummern und Namen der Projektleitung sowie das eingebaute Projekt Unproduktiv."
        importBtn = "Import"
        importedResult = { added, skipped -> "$added Projekt(e) importiert" + if (skipped > 0) ", $skipped bereits vorhanden." else "." }
        filterAll = "Alle"
        filterActive = "Aktiv"
        filterOnHold = "Pausiert"
        filterCompleted = "Fertig"
        noProjectsTitle = "Noch keine Projekte"
        noProjectsSubtitle = "Mit + die Projekte erfassen, auf die du Stunden buchst."
        booked = "GEBUCHT"
        tasks = "AUFGABEN"
        done = "erledigt"
        newProject = "Neues Projekt"
        editProject = "Projekt bearbeiten"
        deleteProject = "Projekt löschen"
        projectNumber = "Projektnummer *"
        projectNumberHint = "z.B. P-2026-0142"
        projectName = "Projektname *"
        customer = "Kunde / Maschine (optional)"
        plannedHours = "Geplante Stunden"
        status = "Status"
        colourTag = "Farbe"
        addProject = "Projekt hinzufügen"
        workSchedule = "Arbeitszeitmodell"
        editSchedule = "Arbeitszeitmodell bearbeiten"
        workCategories = "Tätigkeitskategorien"
        categoriesSubtitle = "SPS, Hochsprache, Meeting… Unproduktive werden separat ausgewiesen."
        productive = "Produktiv"
        unproductiveLabel = "Unproduktiv"
        newCategory = "Neue Kategorie"
        editCategory = "Kategorie bearbeiten"
        categoryName = "Name *"
        categoryPlaceholder = "z.B. SPS, Meeting, Kaffeepause"
        productiveWork = "Produktives Projekt"
        unproductiveTime = "Unproduktives Projekt"
        productiveHint = "Stunden zählen als Projektarbeit und kommen in den Stundenrapport-Export"
        unproductiveHint = "Zeit wird erfasst, aber separat ausgewiesen (Meetings, Pausen, …)"
        colour = "Farbe"
        languageTitle = "Sprache"
        languageSubtitle = "App-Sprache"
        statusActive = "Aktiv"
        statusOnHold = "Pausiert"
        statusCompleted = "Fertig"
        hrsLogged = { "$it Std. gebucht" }
        budget = { "Budget: $it Std." }
        scheduleSubtitle = "Stunden pro Wochentag – z.B. 3 Tage + Ausbildung, 80 % mit freiem Tag, 90 % mit halbem Tag."
        hoursPerWeekAt100 = "Stunden / Woche bei 100 %"
        legalMaxPerWeek = "Gesetzl. Max. h / Woche"
        hoursPerDay = "Sollstunden pro Tag"
        hoursPerDayHint = "0 = frei"
        schedulePreview = { weekly, percent, days -> "$weekly h pro Woche ($percent %) · $days Arbeitstage" }
        checkValues = "Bitte Werte prüfen."
        perWeek = "h/Woche"
        perDay = "h/Tag"
        maxPerWeek = "max."
        bookAbsence = "Abwesenheit buchen"
        day = "Tag"
        reason = "Grund"
        reasonRequired = "Grund *"
        reasonPlaceholder = "z.B. Militär, Arzt"
        hoursFullDay = { "Stunden (ganzer Tag = $it)" }
        creditsHint = "Zählt zum Wochensoll (bezahlte Abwesenheit)."
        fromOvertimeHint = "Wird vom Überzeitsaldo abgezogen – keine Gutschrift."
        enterHours = "Stunden eingeben, z.B. 8 oder 4."
        importProjects = "Projekte importieren"
        importDescription = "Ein Projekt pro Zeile einfügen: Nummer, Name und optional Kunde – getrennt durch Tab, ';' oder ','. Aus einer Tabellenkalkulation kopierte Zellen funktionieren direkt. Vorhandene Projektnummern werden übersprungen."
        projectList = "Projektliste"
        importPlaceholder = "P-2026-0142;Spindel Retrofit;Kunde AG\nP-2026-0150;Neues HMI"
        noneRecognised = "Noch keine Projekte erkannt."
        recognised = { n, list -> "$n Projekt(e) erkannt: $list" }
        importN = { "$it importieren" }
        logActivity = "Tätigkeit erfassen"
        editActivity = "Tätigkeit bearbeiten"
        sapProject = "Projekt"
        category = "Aufgabe"
        selectCategory = "Aufgabe wählen"
        taskOptional = "Aufgabe (optional)"
        noSpecificTask = "Allgemein"
        whatDidYouDo = "Was hast du gemacht?"
        startLabel = "Start"
        endLabel = "Ende"
        pickStart = "Start wählen"
        pickEnd = "Ende wählen"
        stillRunning = "Diese Tätigkeit läuft noch."
        endAfterStart = "Ende muss nach Start liegen."
        duration = { "Dauer: $it" }
        noProjectOption = "Noch kein Projekt / nicht in der App"
        unproductiveSuffix = "(unproduktiv)"
        addProjectOption = "+ Neues Projekt…"
        unassignedProject = "Noch keinem Projekt zugeordnet"
        unassignedHint = "Eintrag bearbeiten, um später Projekt oder Aufgabe zuzuordnen."
        addTaskOption = "+ Neue Aufgabe…"
        newTaskTitle = "Neue Aufgabe"
        noteHint = "Notiz – jetzt, während der Arbeit oder danach"
        moveTask = "In anderes Projekt verschieben"
        moveTaskTo = "Aufgabe verschieben nach"
        noTasksInProject = "Noch keine Aufgaben"
        tasksCount = { "$it Aufgaben" }
        todayOnThisTask = "heute an dieser Aufgabe"
        thisSession = "diese Sitzung"
        switchToThis = "Zu dieser Tätigkeit wechseln"
        continueThis = "Diese Tätigkeit fortsetzen"
        addNote = "Notiz hinzufügen"
        editNote = "Notiz bearbeiten"
        untilNow = "Bis jetzt"
        durationChip = { "+$it" }
        quickDuration = "Dauer"
        noActivityLogged = "Keine Tätigkeit erfasst"
        assignGap = "Zuordnen"
        previousActivityEnds = { name, time -> "Davor: $name (endet $time)" }
        nextActivityStarts = { name, time -> "Danach: $name (beginnt $time)" }
        gapBetween = { "Lässt $it ohne Tätigkeit." }
        overlapBetween = { "Überschneidet sich um $it." }
        moveNeighbourTo = { "Auf $it verschieben" }
        leaveGap = "Lücke lassen, später zuordnen"
        keepOverlap = "So lassen"
        neighbourWouldBeEmpty = "Beim Verschieben bliebe keine Zeit übrig; bearbeite stattdessen diese Tätigkeit."
        addPeriod = "Stempelzeit hinzufügen"
        editPeriod = "Stempelzeit bearbeiten"
        clockInLabel = "Einstempeln"
        clockOutLabel = "Ausstempeln"
        pickTime = "Zeit wählen"
        clockOutReason = "Grund fürs Ausstempeln"
        clockOutAfterIn = "Ausstempeln muss nach Einstempeln liegen."
        exportTitle = "Stundenexport"
        report = "Bericht"
        fileFormat = "Dateiformat"
        roundQuarter = "Auf 0.25 h runden (sonst exakt, z.B. 7.78)"
        preview = "Vorschau"
        copy = "Kopieren"
        copied = "Kopiert!"
        copiedToClipboard = "In die Zwischenablage kopiert!"
        saveShare = "CSV speichern / teilen"
        saveFile = "Datei speichern"
        shareFile = "Teilen (E-Mail, Chat, …)"
        moreRows = "... [weitere Zeilen]"
        taskFilterTodo = "Offen"
        taskFilterInProgress = "In Arbeit"
        taskFilterDone = "Erledigt"
        noTasksTitle = "Keine Aufgaben"
        noTasksSubtitle = "Aufgaben der Projektleitung mit Terminen und Erinnerungen erfassen."
        toggleComplete = "Erledigt umschalten"
        track = "Starten"
        bookedPlanned = { b, p -> "${b}h gebucht / ${p}h geplant" }
        newTask = "Neue Aufgabe"
        editTask = "Aufgabe bearbeiten"
        taskTitle = "Titel *"
        descriptionNotes = "Beschreibung & Notizen"
        priority = "Priorität"
        estHours = "Gesch. Std."
        deadlineReminders = "Termin & automatische Erinnerung"
        noDeadline = "Kein Termin"
        setDeadline = "Termin setzen"
        saveTask = "Aufgabe speichern"
        deadlineTime = "Terminzeit"
        selectProject = "Projekt wählen"
        addTask = "Aufgabe hinzufügen"
        priorityLow = "Tief"
        priorityMedium = "Mittel"
        priorityHigh = "Hoch"
        priorityUrgent = "Dringend"
        statusTodo = "Offen"
        statusInProgress = "In Arbeit"
        statusReview = "Review"
        statusDone = "Erledigt"
        overdueDays = { "${it}T überfällig" }
        overdueHours = { "${it}h überfällig" }
        dueUnder2h = "Fällig in < 2h!"
        dueInHours = { "Fällig in ${it}h" }
        dueTomorrow = "Fällig morgen"
        dueOn = { "Fällig $it" }
        absenceSick = "Krank"; absenceHoliday = "Ferien"; absencePublicHoliday = "Feiertag (bezahlt)"
        absenceCompensation = "Kompensation (aus Überzeit)"; absenceEducation = "Ausbildung / Schulung"; absenceOther = "Anderer Grund"
        reasonLunch = "Mittag"; reasonBreak = "Pause"; reasonOut = "Ausser Haus"; reasonHome = "Feierabend"
        periodDay = "Tag"; periodWeek = "Woche"; periodMonth = "Monat"
        exportSummary = "Summe pro Projekt"; exportTimesheet = "Tagesrapport"; exportAttendance = "Anwesenheit & Überzeit"
        exportSapWeek = "Wochenrapport (kopieren & einfügen)"
        sapWeekHint = "Tab-getrennt, ein Block pro Kalenderwoche, Montag zuerst: kopieren und in den Wochenrapport einfügen. Stunden mit zwei Nachkommastellen; leere Zellen = nichts zu buchen. Zeilen mit ! sind Zeit ohne Projekt – manuell buchen oder zuerst zuordnen."
        sapSettingsTitle = "Stundenbuchung"
        sapSettingsSubtitle = "Leistungsarten und Kostenstelle für den wöchentlichen Kopieren-und-Einfügen-Export."
        sapProductiveType = "Leistungsart – Projektarbeit"
        sapUnproductiveType = "Leistungsart – unproduktiv"
        sapUnproductiveNumber = "Kostenstelle für unproduktive Stunden"
        undo = "Rückgängig"; activityDeleted = "Tätigkeit gelöscht"; moreActions = "Weitere Aktionen"
        deleteProjectQuestion = { "Projekt $it löschen?" }
        deleteProjectWarning = "Die Aufgaben werden gelöscht. Bereits gebuchte Zeit bleibt erhalten, verliert aber das Projekt und muss neu zugeordnet werden."
        deleteTaskQuestion = { "Aufgabe «$it» löschen?" }
        deleteTaskWarning = "Auf diese Aufgabe gebuchte Zeit bleibt erhalten, verliert aber die Aufgabe."
        deletePeriodQuestion = "Diese Stempelzeit löschen? Eingestempelte Zeit und Überzeit des Tages ändern sich."
        deleteAbsenceQuestion = "Diese Abwesenheit löschen? Die Stunden werden nicht mehr gutgeschrieben."
        targetLabel = { "Soll $it" }; remainingToTarget = { "noch $it" }; targetReached = "Soll erreicht"
        eventClockedIn = "Eingestempelt"; eventClockedOut = "Ausgestempelt"
        noActivity = "Keine Tätigkeit erfasst"; generalTaskTime = "Keine bestimmte Aufgabe"
        clockInFirst = "Zuerst einstempeln, dann wählen, woran du arbeitest."
        paidBreak = "Bezahlte Pausen"; paidBreakPerDay = "Bezahlte Pause pro Tag (Min.)"
        paidBreakHint = "Als Pause markierte Kaffee-/Raucherpausen zählen bis zu so vielen Minuten pro Tag als Arbeitszeit. 0 = unbezahlt."
        paidBreakSummary = { "$it Min. bezahlte Pause" }
        breakRulesTitle = "Pflichtpausen"
        breakRulesHint = "Pause, die ein Tag ab so viel eingestempelter Zeit enthalten muss. Mittag und Pausen zählen zusammen."
        ruleAfterHours = "Mehr als (h)"
        ruleBreakMinutes = "Pause (Min.)"
        addRule = "Regel hinzufügen"
        deductMissingBreak = "Fehlende Pause automatisch abziehen"
        deductMissingBreakHint = "Wurde weniger Pause gemacht als vorgeschrieben, wird die Differenz von der Arbeitszeit abgezogen: 9 h 30 eingestempelt mit 30 Min. Pause bei 1 h Pflicht zählt als 9 h."
        missingBreakDeducted = "− Fehlende Pause"
        breakRuleSummary = { hours, minutes -> "$minutes Min. > $hours h" }
        warnBreakDeducted = { "$it abgezogen." }
        formatCsv = "CSV (Komma)"; formatExcel = "Tabellenkalkulation (Semikolon)"
        backupTitle = "Sicherung & Übertragung"; backupSubtitle = "Alles (Projekte, Aufgaben, Zeiten, Abwesenheiten, Einstellungen) als eine Datei sichern und auf einem anderen Gerät wiederherstellen."
        backupShare = "Teilen"; backupRestore = "Aus Datei wiederherstellen"
        backupExported = "Sicherung erstellt"; backupRestored = "Sicherung wiederhergestellt"
        backupContents = { s -> "${s.projects} Projekte · ${s.tasks} Aufgaben · ${s.timeEntries} Tätigkeiten · ${s.attendanceSessions} Stempelzeiten · ${s.dayRecords} Abwesenheiten" }
        restoreQuestion = { "Sicherung vom $it wiederherstellen?" }
        restoreWarning = "Alle Daten auf diesem Gerät werden durch die Sicherung ersetzt. Das kann nicht rückgängig gemacht werden – im Zweifel zuerst dieses Gerät sichern."
        restoreConfirm = "Ersetzen & wiederherstellen"
        backupErrorNotBackup = "Diese Datei ist keine WorkTracker-Sicherung."
        backupErrorNewer = "Die Sicherung stammt von einer neueren App-Version. Bitte zuerst die App aktualisieren."
        backupErrorCorrupt = "Die Sicherungsdatei ist beschädigt und kann nicht gelesen werden."
        backupErrorIo = "Die Datei konnte nicht gelesen oder geschrieben werden."
        autoBackupTitle = "Automatische Sicherung"
        autoBackupSubtitle = "Legt wenige Sekunden nach jeder Änderung eine Kopie aller Daten in einem Ordner deiner Wahl ab – Google Drive, iCloud Drive oder ein Ordner auf diesem Gerät. Nach einer Neuinstallation aus diesem Ordner wiederherstellen."
        autoBackupOff = "Aus – kein Ordner gewählt"
        autoBackupFolder = { "Ordner: $it" }
        autoBackupLast = { "Letzte Sicherung $it" }
        autoBackupPending = "Noch nicht geschrieben"
        autoBackupFailed = "Die letzte Sicherung konnte nicht geschrieben werden. Prüfe, ob der Ordner noch existiert, oder wähle ihn neu."
        autoBackupNoFile = "In diesem Ordner liegt noch keine Sicherung."
        chooseFolder = "Ordner wählen"
        changeFolder = "Ändern"
        turnOff = "Ausschalten"
        backupNow = "Jetzt sichern"
        restoreFromFolder = "Aus Ordner wiederherstellen"
        licensesTitle = "Open-Source-Lizenzen"
        licensesSubtitle = "Die Bibliotheken, auf denen die App aufbaut, und ihre Bedingungen."
        licensesIntro = "WorkTracker baut auf Open-Source-Software auf. Die folgenden Bibliotheken stecken in der App; das Urheberrecht bleibt bei ihren Autoren."
        licensesApacheNotice = "Lizenziert unter der Apache License, Version 2.0. Die Nutzung dieser Dateien ist nur gemäss der Lizenz erlaubt; eine Kopie gibt es unter:"
        importAppsTitle = "Aus anderen Apps importieren"
        importAppsSubtitle = "Hol deine bisherigen Daten herüber. Unterstützt: der CSV-Export der iOS-App WORK (Export → CSV). Bereits vorhandene Tage werden übersprungen."
        importWorkButton = "WORK-Export (CSV)"
        workImportTitle = "WORK-Export importieren"
        workImportSummary = { s, e, a, p, t -> "$s Präsenzzeiten · $e Tätigkeiten · $a Abwesenheiten" + if (p > 0 || t > 0) " · legt $p Projekte und $t Aufgaben an" else "" }
        workImportSkipped = { "$it Zeilen sind schon vorhanden und werden übersprungen." }
        workImportNothing = "Nichts Neues zu importieren."
        workImportConfirm = "Importieren"
        workImported = { s, e -> "$s Präsenzzeiten und $e Tätigkeiten importiert" }
        notWorkExport = "Diese Datei ist kein WORK-Export."
    }

    val FR: AppStrings = AppStrings(Language.FR).apply {
        cancel = "Annuler"
        save = "Enregistrer"
        add = "Ajouter"
        edit = "Modifier"
        delete = "Supprimer"
        close = "Fermer"
        next = "Suivant"
        back = "Retour"
        ok = "OK"
        yes = "Oui"
        no = "Non"
        monthsShort = listOf("janv", "févr", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc")
        monthsLong = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
        weekdaysShort = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")
        weekdaysTwo = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")
        weekdaysLong = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
        tabToday = "Aujourd'hui"
        tabProjects = "Réglages"
        tabTasks = "Projets"
        tabReports = "Rapports"
        badgeWorking = "EN COURS"
        badgeClockedIn = "POINTÉ"
        clockedOut = "DÉPOINTÉ"
        clockedInSince = { "POINTÉ depuis $it" }
        clockedInToday = "pointé aujourd'hui"
        clockIn = "POINTER"
        clockOut = "DÉPOINTER"
        currentActivity = "ACTIVITÉ EN COURS"
        whatWorkingOn = "SUR QUOI TRAVAILLES-TU ?"
        searchProjectsTasks = "Rechercher un projet ou une tâche…"
        focus = "Focus"
        focusAll = "Tous"
        focusOn = "Ajouter au focus"
        focusOff = "Retirer du focus"
        focusHint = "Marquez d'une étoile les projets du moment : Aujourd'hui ne propose alors que ceux-là."
        noMatches = "Aucun projet ni tâche ne correspond."
        switchActivity = "CHANGER D'ACTIVITÉ"
        switchShort = "Changer"
        start = "Démarrer"
        stop = "Arrêter"
        noteOptional = "Note (optionnel)"
        notePlaceholder = "p.ex. logique de sécurité API broche, écran IHM…"
        autoClockInHint = "Démarrer une activité pointe automatiquement."
        absence = "Absence"
        editAbsence = "Modifier l'absence"
        clockedIn = "Pointé"
        projectWork = "Travail projet"
        unproductive = "Improductif"
        overtimeToday = "Heures sup. aujourd'hui"
        activities = "Activités"
        activitiesSubtitle = { "$it aujourd'hui · modifier pour ajuster projet ou heures" }
        manualEntry = "Saisie manuelle"
        noActivitiesTitle = "Pas encore d'activité"
        noActivitiesSubtitle = "Pointe et démarre une activité, ou saisis-la manuellement."
        noProject = "Sans projet"
        since = { "depuis $it" }
        running = "en cours"
        deadlineAlerts = { "Alertes d'échéance ($it)" }
        workOnIt = "Y travailler"
        snooze = "Reporter +24h"
        uncategorized = "Non catégorisé"
        credited = "crédité"
        fromOvertime = "des heures sup."
        targetReachedTitle = "Objectif du jour atteint 🎉"
        stillClockedInTitle = "Toujours pointé ?"
        stillClockedInBody = { "Tu es pointé depuis plus de $it. Oublié de dépointer ?" }
        clockOutAt = { "Dépointer à $it" }
        monthEndCheck = "Contrôle de fin de mois"
        readyToBook = "Prêt à saisir dans la feuille d'heures"
        thingsToFix = { if (it == 1) "1 point à corriger avant la saisie" else "$it points à corriger avant la saisie" }
        checkForgotClockOut = { if (it == 1) "1 jour sans dépointage" else "$it jours sans dépointage" }
        checkUnassigned = { "$it de travail sans projet – à attribuer dans la vue du jour" }
        checkRuleWarnings = { if (it == 1) "1 avertissement de temps de travail" else "$it avertissements de temps de travail" }
        checkRoundedTotal = { rounded, exact -> "Heures projet arrondies : $rounded h (exact $exact h)" }
        checkOpenToday = "Aujourd'hui est encore ouvert – le mois est complet une fois dépointé."
        targetReachedBody = { "Tu as pointé $it aujourd'hui. L'heure de rentrer ?" }
        sapExport = "Export"
        target = "Objectif"
        absencesCredited = "Absences créditées"
        overtime = "Heures supplémentaires"
        balanceToDate = "Solde à ce jour"
        hoursPerProject = "Heures par projet"
        noProjectHours = "Aucune heure projet sur cette période."
        total = "Total"
        workingTimeRules = "Règles du temps de travail"
        absences = "Absences"
        noneInPeriod = "Aucune sur cette période."
        clockInPeriods = "Périodes pointées"
        notClockedInOnDay = "Pas pointé ce jour-là."
        noActivitiesOnDay = "Aucune activité ce jour-là."
        breaks = "Pauses"
        previousMonth = "Mois précédent"
        nextMonth = "Mois suivant"
        previousWeek = "Semaine précédente"
        nextWeek = "Semaine suivante"
        stillClockedIn = "encore pointé"
        warnBreak = { worked, brk, required -> "$worked travaillées avec seulement $brk de pause – au moins $required requis." }
        warnWeekMax = { week, worked, max -> "Semaine du $week : $worked travaillées – maximum légal $max h." }
        warnForgotClockOut = { date -> "Oubli de dépointer le $date – corriger la période pointée." }
        sapProjects = "Projets"
        projectsSubtitle = "Numéros et noms de projets de la direction de projet, plus le projet intégré Improductif."
        importBtn = "Importer"
        importedResult = { added, skipped -> "$added projet(s) importé(s)" + if (skipped > 0) ", $skipped déjà existant(s)." else "." }
        filterAll = "Tous"
        filterActive = "Actifs"
        filterOnHold = "En pause"
        filterCompleted = "Terminés"
        noProjectsTitle = "Pas encore de projet"
        noProjectsSubtitle = "Touche + pour ajouter les projets sur lesquels tu imputes des heures."
        booked = "IMPUTÉ"
        tasks = "TÂCHES"
        done = "terminées"
        newProject = "Nouveau projet"
        editProject = "Modifier le projet"
        deleteProject = "Supprimer le projet"
        projectNumber = "Numéro de projet *"
        projectNumberHint = "p.ex. P-2026-0142"
        projectName = "Nom du projet *"
        customer = "Client / machine (optionnel)"
        plannedHours = "Heures planifiées"
        status = "Statut"
        colourTag = "Couleur"
        addProject = "Ajouter un projet"
        workSchedule = "Horaire de travail"
        editSchedule = "Modifier l'horaire"
        workCategories = "Catégories de travail"
        categoriesSubtitle = "API, langage évolué, réunion… Les improductives sont rapportées séparément."
        productive = "Productif"
        unproductiveLabel = "Improductif"
        newCategory = "Nouvelle catégorie"
        editCategory = "Modifier la catégorie"
        categoryName = "Nom *"
        categoryPlaceholder = "p.ex. API, Réunion, Pause café"
        productiveWork = "Projet productif"
        unproductiveTime = "Projet improductif"
        productiveHint = "Les heures comptent comme travail projet et vont dans l'export de la feuille d'heures"
        unproductiveHint = "Le temps est enregistré mais rapporté séparément (réunions, pauses, …)"
        colour = "Couleur"
        languageTitle = "Langue"
        languageSubtitle = "Langue de l'application"
        statusActive = "Actif"
        statusOnHold = "En pause"
        statusCompleted = "Terminé"
        hrsLogged = { "$it h imputées" }
        budget = { "Budget : $it h" }
        scheduleSubtitle = "Heures par jour de semaine – p.ex. 3 jours + formation, 80 % avec un jour libre, 90 % avec une demi-journée."
        hoursPerWeekAt100 = "Heures / semaine à 100 %"
        legalMaxPerWeek = "Max légal h / semaine"
        hoursPerDay = "Heures cibles par jour"
        hoursPerDayHint = "0 = congé"
        schedulePreview = { weekly, percent, days -> "$weekly h par semaine ($percent %) · $days jours de travail" }
        checkValues = "Veuillez vérifier les valeurs."
        perWeek = "h/sem."
        perDay = "h/jour"
        maxPerWeek = "max"
        bookAbsence = "Saisir une absence"
        day = "Jour"
        reason = "Motif"
        reasonRequired = "Motif *"
        reasonPlaceholder = "p.ex. service militaire, médecin"
        hoursFullDay = { "Heures (journée complète = $it)" }
        creditsHint = "Compte pour l'objectif hebdomadaire (absence payée)."
        fromOvertimeHint = "Déduit du solde d'heures supplémentaires – rien n'est crédité."
        enterHours = "Saisis les heures, p.ex. 8 ou 4."
        importProjects = "Importer des projets"
        importDescription = "Colle un projet par ligne : numéro, nom et client optionnel – séparés par tabulation, ';' ou ','. Les cellules copiées d'un tableur fonctionnent directement. Les numéros existants sont ignorés."
        projectList = "Liste de projets"
        importPlaceholder = "P-2026-0142;Rétrofit broche;Client SA\nP-2026-0150;Nouvelle IHM"
        noneRecognised = "Aucun projet reconnu pour l'instant."
        recognised = { n, list -> "$n projet(s) reconnu(s) : $list" }
        importN = { "Importer $it" }
        logActivity = "Saisir une activité"
        editActivity = "Modifier l'activité"
        sapProject = "Projet"
        category = "Tâche"
        selectCategory = "Choisir une tâche"
        taskOptional = "Tâche (optionnel)"
        noSpecificTask = "Général"
        whatDidYouDo = "Qu'as-tu fait ?"
        startLabel = "Début"
        endLabel = "Fin"
        pickStart = "Choisir le début"
        pickEnd = "Choisir la fin"
        stillRunning = "Cette activité est encore en cours."
        endAfterStart = "La fin doit être après le début."
        duration = { "Durée : $it" }
        noProjectOption = "Projet inconnu / pas encore dans l'app"
        unproductiveSuffix = "(improductif)"
        addProjectOption = "+ Nouveau projet…"
        unassignedProject = "Projet pas encore attribué"
        unassignedHint = "Modifie l'entrée pour attribuer un projet ou une tâche plus tard."
        addTaskOption = "+ Nouvelle tâche…"
        newTaskTitle = "Nouvelle tâche"
        noteHint = "Note – maintenant, pendant ou après le travail"
        moveTask = "Déplacer vers un autre projet"
        moveTaskTo = "Déplacer la tâche vers"
        noTasksInProject = "Pas encore de tâche"
        tasksCount = { "$it tâches" }
        todayOnThisTask = "aujourd'hui sur cette tâche"
        thisSession = "cette session"
        switchToThis = "Passer à cette activité"
        continueThis = "Reprendre cette activité"
        addNote = "Ajouter une note"
        editNote = "Modifier la note"
        untilNow = "Jusqu'à maintenant"
        durationChip = { "+$it" }
        quickDuration = "Durée"
        noActivityLogged = "Aucune activité saisie"
        assignGap = "Attribuer"
        previousActivityEnds = { name, time -> "Avant : $name (finit à $time)" }
        nextActivityStarts = { name, time -> "Après : $name (commence à $time)" }
        gapBetween = { "Laisse $it sans activité." }
        overlapBetween = { "Chevauche de $it." }
        moveNeighbourTo = { "Déplacer à $it" }
        leaveGap = "Laisser le trou, attribuer plus tard"
        keepOverlap = "Laisser tel quel"
        neighbourWouldBeEmpty = "En la déplaçant, il ne resterait aucun temps ; modifiez plutôt cette activité."
        addPeriod = "Ajouter une période pointée"
        editPeriod = "Modifier la période pointée"
        clockInLabel = "Pointer"
        clockOutLabel = "Dépointer"
        pickTime = "Choisir l'heure"
        clockOutReason = "Motif du dépointage"
        clockOutAfterIn = "Le dépointage doit être après le pointage."
        exportTitle = "Export des heures"
        report = "Rapport"
        fileFormat = "Format de fichier"
        roundQuarter = "Arrondir à 0.25 h (sinon exact, p.ex. 7.78)"
        preview = "Aperçu"
        copy = "Copier"
        copied = "Copié !"
        copiedToClipboard = "Copié dans le presse-papiers !"
        saveShare = "Enregistrer / partager CSV"
        saveFile = "Enregistrer"
        shareFile = "Partager (e-mail, chat, …)"
        moreRows = "... [plus de lignes]"
        taskFilterTodo = "À faire"
        taskFilterInProgress = "En cours"
        taskFilterDone = "Terminées"
        noTasksTitle = "Aucune tâche"
        noTasksSubtitle = "Ajoute les tâches assignées par le chef de projet, avec échéances et rappels."
        toggleComplete = "Basculer terminé"
        track = "Démarrer"
        bookedPlanned = { b, p -> "${b}h imputées / ${p}h planifiées" }
        newTask = "Nouvelle tâche"
        editTask = "Modifier la tâche"
        taskTitle = "Titre *"
        descriptionNotes = "Description & notes"
        priority = "Priorité"
        estHours = "Heures est."
        deadlineReminders = "Échéance & rappels automatiques"
        noDeadline = "Pas d'échéance"
        setDeadline = "Fixer l'échéance"
        saveTask = "Enregistrer la tâche"
        deadlineTime = "Heure de l'échéance"
        selectProject = "Choisir un projet"
        addTask = "Ajouter une tâche"
        priorityLow = "Basse"
        priorityMedium = "Moy."
        priorityHigh = "Haute"
        priorityUrgent = "Urgent"
        statusTodo = "À faire"
        statusInProgress = "En cours"
        statusReview = "Revue"
        statusDone = "Terminé"
        overdueDays = { "${it}j de retard" }
        overdueHours = { "${it}h de retard" }
        dueUnder2h = "Échéance < 2h !"
        dueInHours = { "Échéance dans ${it}h" }
        dueTomorrow = "Échéance demain"
        dueOn = { "Échéance $it" }
        absenceSick = "Maladie"; absenceHoliday = "Vacances"; absencePublicHoliday = "Jour férié (payé)"
        absenceCompensation = "Compensation (heures sup.)"; absenceEducation = "Formation"; absenceOther = "Autre motif"
        reasonLunch = "Midi"; reasonBreak = "Pause"; reasonOut = "Hors bureau"; reasonHome = "Fin de journée"
        periodDay = "Jour"; periodWeek = "Semaine"; periodMonth = "Mois"
        exportSummary = "Résumé par projet"; exportTimesheet = "Feuille journalière"; exportAttendance = "Présence & heures sup."
        exportSapWeek = "Feuille hebdomadaire (copier-coller)"
        sapWeekHint = "Séparé par tabulations, un bloc par semaine civile, lundi en premier : copiez-le et collez-le dans votre feuille hebdomadaire. Heures à deux décimales ; cellule vide = rien à saisir. Les lignes commençant par ! sont du temps sans projet – à saisir à la main ou à attribuer d'abord."
        sapSettingsTitle = "Saisie des heures"
        sapSettingsSubtitle = "Types d'activité et centre de coûts utilisés par l'export hebdomadaire copier-coller."
        sapProductiveType = "Type d'activité – travail projet"
        sapUnproductiveType = "Type d'activité – improductif"
        sapUnproductiveNumber = "Centre de coûts des heures improductives"
        undo = "Annuler"; activityDeleted = "Activité supprimée"; moreActions = "Plus d'actions"
        deleteProjectQuestion = { "Supprimer le projet $it ?" }
        deleteProjectWarning = "Ses tâches sont supprimées. Le temps déjà imputé est conservé mais perd son projet et devra être réattribué."
        deleteTaskQuestion = { "Supprimer la tâche « $it » ?" }
        deleteTaskWarning = "Le temps imputé sur cette tâche est conservé mais perd sa tâche."
        deletePeriodQuestion = "Supprimer cette période pointée ? Le temps pointé et les heures sup. du jour changent."
        deleteAbsenceQuestion = "Supprimer cette absence ? Ses heures ne sont plus créditées."
        targetLabel = { "Objectif $it" }; remainingToTarget = { "encore $it" }; targetReached = "Objectif atteint"
        eventClockedIn = "Pointé"; eventClockedOut = "Dépointé"
        noActivity = "Aucune activité en cours"; generalTaskTime = "Aucune tâche précise"
        clockInFirst = "Pointe d'abord, puis choisis sur quoi tu travailles."
        paidBreak = "Pauses payées"; paidBreakPerDay = "Pause payée par jour (min)"
        paidBreakHint = "Les pauses café / cigarette marquées « Pause » comptent comme temps de travail jusqu'à ce nombre de minutes par jour. 0 = non payées."
        paidBreakSummary = { "$it min de pause payée" }
        breakRulesTitle = "Pauses obligatoires"
        breakRulesHint = "Pause qu'une journée doit contenir à partir de ce temps pointé. Le repas et les pauses comptent ensemble."
        ruleAfterHours = "Plus de (h)"
        ruleBreakMinutes = "Pause (min)"
        addRule = "Ajouter une règle"
        deductMissingBreak = "Déduire automatiquement la pause manquante"
        deductMissingBreakHint = "Si la pause prise est plus courte que celle exigée, la différence est retirée du temps de travail compté : 9 h 30 pointées avec 30 min de pause pour 1 h exigée comptent 9 h."
        missingBreakDeducted = "− Pause manquante"
        breakRuleSummary = { hours, minutes -> "$minutes min > $hours h" }
        warnBreakDeducted = { "$it déduit." }
        formatCsv = "CSV (virgule)"; formatExcel = "Tableur (point-virgule)"
        backupTitle = "Sauvegarde & transfert"; backupSubtitle = "Enregistrer tout (projets, tâches, temps, absences, réglages) dans un seul fichier et le restaurer sur un autre appareil."
        backupShare = "Partager"; backupRestore = "Restaurer depuis un fichier"
        backupExported = "Sauvegarde créée"; backupRestored = "Sauvegarde restaurée"
        backupContents = { s -> "${s.projects} projets · ${s.tasks} tâches · ${s.timeEntries} activités · ${s.attendanceSessions} périodes pointées · ${s.dayRecords} absences" }
        restoreQuestion = { "Restaurer la sauvegarde du $it ?" }
        restoreWarning = "Toutes les données de cet appareil seront remplacées par la sauvegarde. Irréversible – en cas de doute, sauvegardez d'abord cet appareil."
        restoreConfirm = "Remplacer & restaurer"
        backupErrorNotBackup = "Ce fichier n'est pas une sauvegarde WorkTracker."
        backupErrorNewer = "La sauvegarde provient d'une version plus récente de l'app. Mettez l'app à jour, puis réessayez."
        backupErrorCorrupt = "Le fichier de sauvegarde est endommagé et ne peut pas être lu."
        backupErrorIo = "Le fichier n'a pas pu être lu ou écrit."
        autoBackupTitle = "Sauvegarde automatique"
        autoBackupSubtitle = "Conserve une copie de tout dans un dossier de votre choix – Google Drive, iCloud Drive ou un dossier de l'appareil – quelques secondes après chaque modification. Après une réinstallation, restaurez depuis ce dossier."
        autoBackupOff = "Désactivée – aucun dossier choisi"
        autoBackupFolder = { "Dossier : $it" }
        autoBackupLast = { "Dernière sauvegarde $it" }
        autoBackupPending = "Pas encore écrite"
        autoBackupFailed = "La dernière sauvegarde n'a pas pu être écrite. Vérifiez que le dossier existe toujours ou choisissez-le à nouveau."
        autoBackupNoFile = "Aucune sauvegarde dans ce dossier pour l'instant."
        chooseFolder = "Choisir un dossier"
        changeFolder = "Changer"
        turnOff = "Désactiver"
        backupNow = "Sauvegarder maintenant"
        restoreFromFolder = "Restaurer depuis le dossier"
        licensesTitle = "Licences open source"
        licensesSubtitle = "Les bibliothèques qui composent l'app et leurs conditions."
        licensesIntro = "WorkTracker est construit avec des logiciels open source. Les bibliothèques ci-dessous sont intégrées à l'app ; leurs auteurs en gardent les droits."
        licensesApacheNotice = "Sous licence Apache, version 2.0. L'utilisation de ces fichiers n'est permise que dans le respect de la licence ; une copie est disponible à :"
        importAppsTitle = "Importer depuis d'autres apps"
        importAppsSubtitle = "Récupérez votre historique. Pris en charge : l'export CSV de l'app iOS WORK (Export → CSV). Les jours déjà présents sont ignorés."
        importWorkButton = "Export WORK (CSV)"
        workImportTitle = "Importer l'export WORK"
        workImportSummary = { s, e, a, p, t -> "$s périodes de pointage · $e activités · $a absences" + if (p > 0 || t > 0) " · crée $p projets et $t tâches" else "" }
        workImportSkipped = { "$it lignes existent déjà et sont ignorées." }
        workImportNothing = "Rien de nouveau à importer."
        workImportConfirm = "Importer"
        workImported = { s, e -> "$s périodes de pointage et $e activités importées" }
        notWorkExport = "Ce fichier n'est pas un export WORK."
    }
}

/** Emoji used in the calendar for absences. */
fun AbsenceType.emoji(): String = when (this) {
    AbsenceType.SICK -> "🤒"
    AbsenceType.HOLIDAY -> "🏖️"
    AbsenceType.PUBLIC_HOLIDAY -> "🎉"
    AbsenceType.COMPENSATION -> "⏱️"
    AbsenceType.EDUCATION -> "🎓"
    AbsenceType.OTHER -> "📌"
}

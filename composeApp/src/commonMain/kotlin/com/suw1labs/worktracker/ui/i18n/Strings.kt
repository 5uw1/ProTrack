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
    FR("fr", "Français"),
    ZH("zh", "中文"),
    TH("th", "ไทย");

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
        Language.ZH -> ZH
        Language.TH -> TH
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

    val ZH: AppStrings = AppStrings(Language.ZH).apply {
        cancel = "取消"
        save = "保存"
        add = "添加"
        edit = "编辑"
        delete = "删除"
        close = "关闭"
        next = "下一步"
        back = "返回"
        ok = "确定"
        yes = "是"
        no = "否"
        monthsShort = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
        monthsLong = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
        weekdaysShort = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        weekdaysTwo = listOf("一", "二", "三", "四", "五", "六", "日")
        weekdaysLong = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        tabToday = "今天"
        tabProjects = "设置"
        tabTasks = "项目"
        tabReports = "报表"
        badgeWorking = "工作中"
        badgeClockedIn = "已上班打卡"
        clockedOut = "已下班打卡"
        clockedInSince = { "已打卡 · 从 $it 起" }
        clockedInToday = "今日已打卡时长"
        clockIn = "上班打卡"
        clockOut = "下班打卡"
        currentActivity = "当前活动"
        whatWorkingOn = "你正在做什么？"
        searchProjectsTasks = "搜索项目或任务…"
        focus = "关注"
        focusAll = "全部"
        focusOn = "加入关注"
        focusOff = "取消关注"
        focusHint = "为这段时间在做的项目加星标，「今天」页面就只显示这些项目。"
        noMatches = "没有匹配的项目或任务。"
        switchActivity = "切换活动"
        switchShort = "切换"
        start = "开始"
        stop = "停止"
        noteOptional = "备注（可选）"
        notePlaceholder = "例如：主轴 PLC 安全逻辑、HMI 画面…"
        autoClockInHint = "开始一项活动会自动为你打卡上班。"
        absence = "缺勤"
        editAbsence = "编辑缺勤"
        clockedIn = "已打卡"
        projectWork = "项目工作"
        unproductive = "非生产性"
        overtimeToday = "今日加班"
        activities = "活动"
        activitiesSubtitle = { "今日 $it 项 · 点击可修改项目或时间" }
        manualEntry = "手动记录"
        noActivitiesTitle = "还没有活动"
        noActivitiesSubtitle = "打卡后开始一项活动，或手动记录一条。"
        noProject = "无项目"
        since = { "自 $it" }
        running = "进行中"
        deadlineAlerts = { "截止提醒（$it）" }
        workOnIt = "现在去做"
        snooze = "推迟 24 小时"
        uncategorized = "未分类"
        credited = "已计入"
        fromOvertime = "从加班扣除"
        targetReachedTitle = "已达成今日目标 🎉"
        stillClockedInTitle = "还在打卡状态？"
        stillClockedInBody = { "你已经打卡超过 $it，是不是忘了下班打卡？" }
        clockOutAt = { "在 $it 下班打卡" }
        monthEndCheck = "月末检查"
        readyToBook = "可以登记到工时表了"
        thingsToFix = { if (it == 1) "登记前有 1 项需要处理" else "登记前有 $it 项需要处理" }
        checkForgotClockOut = { if (it == 1) "1 天没有下班打卡" else "$it 天没有下班打卡" }
        checkUnassigned = { "有 $it 的工时没有项目 – 请在日视图中分配" }
        checkRuleWarnings = { if (it == 1) "1 条工时规则警告" else "$it 条工时规则警告" }
        checkRoundedTotal = { rounded, exact -> "项目工时已取整：$rounded 小时（精确值 $exact 小时）" }
        checkOpenToday = "今天尚未结束 – 下班打卡后本月才算完整。"
        targetReachedBody = { "今天已打卡 $it，可以下班了吗？" }
        sapExport = "导出"
        target = "目标"
        absencesCredited = "缺勤计入"
        overtime = "加班"
        balanceToDate = "累计结余"
        hoursPerProject = "各项目工时"
        noProjectHours = "此期间没有项目工时。"
        total = "合计"
        workingTimeRules = "工时规则"
        absences = "缺勤"
        noneInPeriod = "此期间没有记录。"
        clockInPeriods = "打卡时段"
        notClockedInOnDay = "这一天没有打卡。"
        noActivitiesOnDay = "这一天没有活动。"
        breaks = "休息"
        previousMonth = "上个月"
        nextMonth = "下个月"
        previousWeek = "上一周"
        nextWeek = "下一周"
        stillClockedIn = "仍在打卡中"
        warnBreak = { worked, brk, required -> "工作 $worked 只休息了 $brk – 至少需要 $required。" }
        warnWeekMax = { week, worked, max -> "$week 那一周：工作 $worked – 法定上限为 $max 小时。" }
        warnForgotClockOut = { date -> "$date 忘了下班打卡 – 请修正该打卡时段。" }
        sapProjects = "项目"
        projectsSubtitle = "来自项目管理的项目编号和名称，另加内置的「非生产性」项目。"
        importBtn = "导入"
        importedResult = { added, skipped -> "已导入 $added 个项目" + if (skipped > 0) "，$skipped 个已存在。" else "。" }
        filterAll = "全部"
        filterActive = "进行中"
        filterOnHold = "暂停"
        filterCompleted = "已完成"
        noProjectsTitle = "还没有项目"
        noProjectsSubtitle = "点击 + 添加你要记录工时的项目。"
        booked = "已登记"
        tasks = "任务"
        done = "已完成"
        newProject = "新建项目"
        editProject = "编辑项目"
        deleteProject = "删除项目"
        projectNumber = "项目编号 *"
        projectNumberHint = "例如 P-2026-0142"
        projectName = "项目名称 *"
        customer = "客户 / 设备（可选）"
        plannedHours = "计划工时"
        status = "状态"
        colourTag = "颜色标签"
        addProject = "添加项目"
        workSchedule = "工作时间表"
        editSchedule = "编辑时间表"
        workCategories = "工作类别"
        categoriesSubtitle = "PLC、高级语言、会议… 非生产性类别会单独统计。"
        productive = "生产性"
        unproductiveLabel = "非生产性"
        newCategory = "新建工作类别"
        editCategory = "编辑类别"
        categoryName = "名称 *"
        categoryPlaceholder = "例如 PLC、会议、茶歇"
        productiveWork = "生产性项目"
        unproductiveTime = "非生产性项目"
        productiveHint = "工时计为项目工作，并进入工时表导出"
        unproductiveHint = "时间照常记录，但单独统计（会议、休息…）"
        colour = "颜色"
        languageTitle = "语言"
        languageSubtitle = "应用语言"
        statusActive = "进行中"
        statusOnHold = "暂停"
        statusCompleted = "已完成"
        hrsLogged = { "已记录 $it 小时" }
        budget = { "预算：$it 小时" }
        scheduleSubtitle = "每个工作日的工时 – 例如每周 3 天加进修、80 % 休一天、90 % 休半天。"
        hoursPerWeekAt100 = "100 % 时每周工时"
        legalMaxPerWeek = "每周法定上限"
        hoursPerDay = "每日目标工时"
        hoursPerDayHint = "0 = 休息日"
        schedulePreview = { weekly, percent, days -> "每周 $weekly 小时（$percent %）· $days 个工作日" }
        checkValues = "请检查填写的数值。"
        perWeek = "小时/周"
        perDay = "小时/天"
        maxPerWeek = "上限"
        bookAbsence = "登记缺勤"
        day = "日期"
        reason = "原因"
        reasonRequired = "原因 *"
        reasonPlaceholder = "例如：兵役、看医生"
        hoursFullDay = { "小时（整天 = $it）" }
        creditsHint = "计入每周目标（带薪缺勤）。"
        fromOvertimeHint = "从加班结余中扣除 – 不另行计入工时。"
        enterHours = "请输入小时数，例如 8 或 4。"
        importProjects = "导入项目"
        importDescription = "每行一个项目：编号、名称，客户可选 – 用制表符、';' 或 ',' 分隔。从表格软件复制的单元格可直接粘贴。已存在的项目编号会被跳过。"
        projectList = "项目清单"
        importPlaceholder = "P-2026-0142;主轴改造;客户公司\nP-2026-0150;新 HMI"
        noneRecognised = "尚未识别到项目。"
        recognised = { n, list -> "识别到 $n 个项目：$list" }
        importN = { "导入 $it" }
        logActivity = "记录活动"
        editActivity = "编辑活动"
        sapProject = "项目"
        category = "任务"
        selectCategory = "选择任务"
        taskOptional = "任务（可选）"
        noSpecificTask = "一般工作"
        whatDidYouDo = "你做了什么？"
        startLabel = "开始"
        endLabel = "结束"
        pickStart = "选择开始时间"
        pickEnd = "选择结束时间"
        stillRunning = "这项活动仍在进行中。"
        endAfterStart = "结束时间必须晚于开始时间。"
        duration = { "时长：$it" }
        noProjectOption = "暂无项目 / 不在应用中"
        unproductiveSuffix = "（非生产性）"
        addProjectOption = "+ 新建项目…"
        unassignedProject = "尚未分配项目"
        unassignedHint = "稍后编辑这条记录即可分配项目或任务。"
        addTaskOption = "+ 新建任务…"
        newTaskTitle = "新建任务"
        noteHint = "备注 – 现在、工作中或事后补都可以"
        moveTask = "移动到其他项目"
        moveTaskTo = "将任务移动到"
        noTasksInProject = "还没有任务"
        tasksCount = { "$it 个任务" }
        todayOnThisTask = "今日在此任务上"
        thisSession = "本次"
        switchToThis = "切换到这项活动"
        continueThis = "继续这项活动"
        addNote = "添加备注"
        editNote = "编辑备注"
        untilNow = "到现在"
        durationChip = { "+$it" }
        quickDuration = "时长"
        noActivityLogged = "没有记录活动"
        assignGap = "分配"
        previousActivityEnds = { name, time -> "之前：$name（$time 结束）" }
        nextActivityStarts = { name, time -> "之后：$name（$time 开始）" }
        gapBetween = { "会留下 $it 没有活动。" }
        overlapBetween = { "与其重叠 $it。" }
        moveNeighbourTo = { "将其移到 $it" }
        leaveGap = "留空，稍后分配"
        keepOverlap = "保持不变"
        neighbourWouldBeEmpty = "这样移动会让那项活动没有时长；请改为编辑那项活动。"
        addPeriod = "添加打卡时段"
        editPeriod = "编辑打卡时段"
        clockInLabel = "上班打卡"
        clockOutLabel = "下班打卡"
        pickTime = "选择时间"
        clockOutReason = "下班打卡原因"
        clockOutAfterIn = "下班打卡必须晚于上班打卡。"
        exportTitle = "工时导出"
        report = "报表"
        fileFormat = "文件格式"
        roundQuarter = "为登记取整到 0.25 小时（否则按精确值，例如 7.78）"
        preview = "预览"
        copy = "复制"
        copied = "已复制！"
        copiedToClipboard = "已复制到剪贴板！"
        saveShare = "保存 / 分享 CSV"
        saveFile = "保存文件"
        shareFile = "分享（邮件、聊天…）"
        moreRows = "…（还有更多行）"
        taskFilterTodo = "待办"
        taskFilterInProgress = "进行中"
        taskFilterDone = "已完成"
        noTasksTitle = "没有找到任务"
        noTasksSubtitle = "把项目经理分配的任务加进来，附上截止日期和提醒。"
        toggleComplete = "切换完成状态"
        track = "开始记录"
        bookedPlanned = { b, p -> "已记 ${b} 小时 / 计划 ${p} 小时" }
        newTask = "新建项目任务"
        editTask = "编辑任务"
        taskTitle = "任务标题 *"
        descriptionNotes = "说明与备注"
        priority = "优先级"
        estHours = "预计工时"
        deadlineReminders = "截止日期与自动提醒"
        noDeadline = "未设置截止日期"
        setDeadline = "设置截止日期"
        saveTask = "保存任务"
        deadlineTime = "截止时间"
        selectProject = "选择项目"
        addTask = "添加任务"
        priorityLow = "低"
        priorityMedium = "中"
        priorityHigh = "高"
        priorityUrgent = "紧急"
        statusTodo = "待办"
        statusInProgress = "进行中"
        statusReview = "待审核"
        statusDone = "已完成"
        overdueDays = { "逾期 ${it} 天" }
        overdueHours = { "逾期 ${it} 小时" }
        dueUnder2h = "不到 2 小时到期！"
        dueInHours = { "${it} 小时后到期" }
        dueTomorrow = "明天到期"
        dueOn = { "$it 到期" }
        absenceSick = "病假"; absenceHoliday = "年假（自己的假期）"; absencePublicHoliday = "法定节假日（公司支付）"
        absenceCompensation = "调休（从加班扣除）"; absenceEducation = "进修 / 培训"; absenceOther = "其他原因"
        reasonLunch = "午休"; reasonBreak = "休息"; reasonOut = "外出"; reasonHome = "下班回家"
        periodDay = "日"; periodWeek = "周"; periodMonth = "月"
        exportSummary = "各项目汇总"; exportTimesheet = "每日工时表"; exportAttendance = "出勤与加班"
        exportSapWeek = "周工时表（复制粘贴）"
        sapWeekHint = "以制表符分隔，每个自然周一段，从周一开始：复制后粘贴到你的周工时表。小时保留两位小数；空单元格表示无需登记。以 ! 开头的行是没有项目的工时 – 请手动登记或先分配项目。"
        sapSettingsTitle = "工时表登记"
        sapSettingsSubtitle = "周工时表复制粘贴导出所用的活动类型和成本对象。"
        sapProductiveType = "活动类型 – 项目工作"
        sapUnproductiveType = "活动类型 – 非生产性"
        sapUnproductiveNumber = "非生产性工时的成本对象"
        undo = "撤销"; activityDeleted = "活动已删除"; moreActions = "更多操作"
        deleteProjectQuestion = { "删除项目 $it？" }
        deleteProjectWarning = "其任务会一并删除。已登记在该项目上的工时会保留，但会失去项目，需要重新分配。"
        deleteTaskQuestion = { "删除任务「$it」？" }
        deleteTaskWarning = "登记在该任务上的工时会保留，但会失去任务。"
        deletePeriodQuestion = "删除这个打卡时段？当天的打卡时长和加班会随之改变。"
        deleteAbsenceQuestion = "删除这条缺勤？其工时将不再计入。"
        targetLabel = { "目标 $it" }; remainingToTarget = { "还差 $it" }; targetReached = "已达成目标"
        eventClockedIn = "上班打卡"; eventClockedOut = "下班打卡"
        noActivity = "没有进行中的活动"; generalTaskTime = "无特定任务"
        clockInFirst = "请先打卡，再选择你要做的工作。"
        paidBreak = "带薪休息"; paidBreakPerDay = "每天带薪休息（分钟）"
        paidBreakHint = "标记为「休息」的茶歇 / 抽烟时间，每天在此分钟数内计为工作时间。0 = 不带薪。"
        paidBreakSummary = { "$it 分钟带薪休息" }
        breakRulesTitle = "必需的休息"
        breakRulesHint = "打卡时间达到以下时长后，当天必须包含的休息时间。午休和休息合并计算。"
        ruleAfterHours = "超过（小时）"
        ruleBreakMinutes = "休息（分钟）"
        addRule = "添加规则"
        deductMissingBreak = "自动扣除未休的休息时间"
        deductMissingBreakHint = "如果休息时间少于规定，差额会从计入的工作时间中扣除：打卡 9 小时 30 分、休息 30 分钟而规定为 1 小时，则计为 9 小时。"
        missingBreakDeducted = "− 未休的休息"
        breakRuleSummary = { hours, minutes -> "$minutes 分钟 > $hours 小时" }
        warnBreakDeducted = { "已扣除 $it。" }
        formatCsv = "CSV（逗号）"; formatExcel = "表格（分号）"
        backupTitle = "备份与迁移"; backupSubtitle = "把所有内容（项目、任务、工时、缺勤、设置）保存为一个文件，并在另一台设备上恢复。"
        backupShare = "分享"; backupRestore = "从文件恢复"
        backupExported = "备份已创建"; backupRestored = "备份已恢复"
        backupContents = { s -> "${s.projects} 个项目 · ${s.tasks} 个任务 · ${s.timeEntries} 项活动 · ${s.attendanceSessions} 个打卡时段 · ${s.dayRecords} 条缺勤" }
        restoreQuestion = { "从 $it 恢复备份？" }
        restoreWarning = "本设备上的所有数据都会被备份内容替换，且无法撤销 – 如有疑虑，请先导出本设备的备份。"
        restoreConfirm = "替换并恢复"
        backupErrorNotBackup = "这个文件不是 WorkTracker 备份。"
        backupErrorNewer = "该备份由更新版本的应用创建。请先更新应用再试。"
        backupErrorCorrupt = "备份文件已损坏，无法读取。"
        backupErrorIo = "无法读取或写入该文件。"
        autoBackupTitle = "自动备份"
        autoBackupSubtitle = "每次变更几秒后，在你选择的文件夹里保留一份完整副本 – 云端硬盘、iCloud Drive 或本机文件夹。重装后可从该文件夹恢复。"
        autoBackupOff = "已关闭 – 未选择文件夹"
        autoBackupFolder = { "文件夹：$it" }
        autoBackupLast = { "上次备份 $it" }
        autoBackupPending = "尚未写入"
        autoBackupFailed = "上次备份未能写入。请确认文件夹仍然存在，或重新选择。"
        autoBackupNoFile = "该文件夹中还没有备份文件。"
        chooseFolder = "选择文件夹"
        changeFolder = "更改"
        turnOff = "关闭"
        backupNow = "立即备份"
        restoreFromFolder = "从文件夹恢复"
        licensesTitle = "开源许可"
        licensesSubtitle = "构建这款应用所用的库及其许可条款。"
        licensesIntro = "WorkTracker 基于开源软件构建。下列库包含在应用内，其著作权归各自作者所有。"
        licensesApacheNotice = "依据 Apache License 2.0 授权。除非符合该许可，否则不得使用这些文件；许可全文见："
        importAppsTitle = "从其他应用导入"
        importAppsSubtitle = "把过往记录带过来。支持 iOS 应用 WORK 的 CSV 导出（导出 → CSV）。已存在的日期会被跳过。"
        importWorkButton = "WORK 导出（CSV）"
        workImportTitle = "导入 WORK 导出文件"
        workImportSummary = { s, e, a, p, t -> "$s 个打卡时段 · $e 项活动 · $a 条缺勤" + if (p > 0 || t > 0) " · 将创建 $p 个项目和 $t 个任务" else "" }
        workImportSkipped = { "$it 行已存在，已跳过。" }
        workImportNothing = "没有可导入的新内容。"
        workImportConfirm = "导入"
        workImported = { s, e -> "已导入 $s 个打卡时段和 $e 项活动" }
        notWorkExport = "这个文件不是 WORK 的导出文件。"
    }

    val TH: AppStrings = AppStrings(Language.TH).apply {
        cancel = "ยกเลิก"
        save = "บันทึก"
        add = "เพิ่ม"
        edit = "แก้ไข"
        delete = "ลบ"
        close = "ปิด"
        next = "ถัดไป"
        back = "ย้อนกลับ"
        ok = "ตกลง"
        yes = "ใช่"
        no = "ไม่"
        monthsShort = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
        monthsLong = listOf("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม")
        weekdaysShort = listOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")
        weekdaysTwo = listOf("จ", "อ", "พ", "พฤ", "ศ", "ส", "อา")
        weekdaysLong = listOf("วันจันทร์", "วันอังคาร", "วันพุธ", "วันพฤหัสบดี", "วันศุกร์", "วันเสาร์", "วันอาทิตย์")
        tabToday = "วันนี้"
        tabProjects = "ตั้งค่า"
        tabTasks = "โปรเจกต์"
        tabReports = "รายงาน"
        badgeWorking = "กำลังทำงาน"
        badgeClockedIn = "เข้างานแล้ว"
        clockedOut = "ออกงานแล้ว"
        clockedInSince = { "เข้างานตั้งแต่ $it" }
        clockedInToday = "เวลาเข้างานวันนี้"
        clockIn = "เข้างาน"
        clockOut = "ออกงาน"
        currentActivity = "งานที่ทำอยู่"
        whatWorkingOn = "กำลังทำงานอะไรอยู่?"
        searchProjectsTasks = "ค้นหาโปรเจกต์หรืองาน…"
        focus = "โฟกัส"
        focusAll = "ทั้งหมด"
        focusOn = "เพิ่มเข้าโฟกัส"
        focusOff = "เอาออกจากโฟกัส"
        focusHint = "ติดดาวโปรเจกต์ที่ทำอยู่ช่วงนี้ แล้วหน้าวันนี้จะแสดงเฉพาะโปรเจกต์เหล่านั้น"
        noMatches = "ไม่พบโปรเจกต์หรืองานที่ตรงกัน"
        switchActivity = "สลับงาน"
        switchShort = "สลับ"
        start = "เริ่ม"
        stop = "หยุด"
        noteOptional = "บันทึกย่อ (ไม่บังคับ)"
        notePlaceholder = "เช่น PLC ความปลอดภัยของสปินเดิล, หน้าจอ HMI…"
        autoClockInHint = "เริ่มงานแล้วระบบจะบันทึกเวลาเข้างานให้อัตโนมัติ"
        absence = "การลา"
        editAbsence = "แก้ไขการลา"
        clockedIn = "เข้างาน"
        projectWork = "งานโปรเจกต์"
        unproductive = "ไม่ใช่งานโปรเจกต์"
        overtimeToday = "โอทีวันนี้"
        activities = "งานที่บันทึก"
        activitiesSubtitle = { "วันนี้ $it รายการ · แตะเพื่อแก้โปรเจกต์หรือเวลา" }
        manualEntry = "บันทึกเอง"
        noActivitiesTitle = "ยังไม่มีงานที่บันทึก"
        noActivitiesSubtitle = "เข้างานแล้วเริ่มงาน หรือบันทึกย้อนหลังเองก็ได้"
        noProject = "ไม่มีโปรเจกต์"
        since = { "ตั้งแต่ $it" }
        running = "กำลังทำ"
        deadlineAlerts = { "เตือนกำหนดส่ง ($it)" }
        workOnIt = "ทำเลย"
        snooze = "เลื่อน 24 ชม."
        uncategorized = "ยังไม่จัดหมวด"
        credited = "นับให้แล้ว"
        fromOvertime = "หักจากโอที"
        targetReachedTitle = "ถึงเป้าของวันแล้ว 🎉"
        stillClockedInTitle = "ยังเข้างานอยู่?"
        stillClockedInBody = { "คุณเข้างานมาเกิน $it แล้ว ลืมกดออกงานหรือเปล่า?" }
        clockOutAt = { "ออกงานเวลา $it" }
        monthEndCheck = "ตรวจก่อนปิดเดือน"
        readyToBook = "พร้อมลงในใบบันทึกเวลาแล้ว"
        thingsToFix = { if (it == 1) "มี 1 เรื่องต้องแก้ก่อนลงเวลา" else "มี $it เรื่องต้องแก้ก่อนลงเวลา" }
        checkForgotClockOut = { if (it == 1) "1 วันที่ไม่ได้กดออกงาน" else "$it วันที่ไม่ได้กดออกงาน" }
        checkUnassigned = { "มีเวลาทำงาน $it ที่ยังไม่มีโปรเจกต์ – กำหนดได้ในหน้ารายวัน" }
        checkRuleWarnings = { if (it == 1) "คำเตือนเรื่องเวลาทำงาน 1 รายการ" else "คำเตือนเรื่องเวลาทำงาน $it รายการ" }
        checkRoundedTotal = { rounded, exact -> "ชั่วโมงโปรเจกต์ปัดแล้ว: $rounded ชม. (จริง $exact ชม.)" }
        checkOpenToday = "วันนี้ยังไม่จบ – เดือนจะครบเมื่อกดออกงานแล้ว"
        targetReachedBody = { "วันนี้เข้างานมา $it แล้ว กลับบ้านได้หรือยัง?" }
        sapExport = "ส่งออก"
        target = "เป้าหมาย"
        absencesCredited = "การลาที่นับให้"
        overtime = "โอที"
        balanceToDate = "ยอดสะสมถึงวันนี้"
        hoursPerProject = "ชั่วโมงต่อโปรเจกต์"
        noProjectHours = "ช่วงนี้ไม่มีชั่วโมงโปรเจกต์"
        total = "รวม"
        workingTimeRules = "กฎเวลาทำงาน"
        absences = "การลา"
        noneInPeriod = "ไม่มีรายการในช่วงนี้"
        clockInPeriods = "ช่วงเวลาเข้างาน"
        notClockedInOnDay = "วันนี้ไม่ได้เข้างาน"
        noActivitiesOnDay = "วันนี้ไม่มีงานที่บันทึก"
        breaks = "เวลาพัก"
        previousMonth = "เดือนก่อน"
        nextMonth = "เดือนถัดไป"
        previousWeek = "สัปดาห์ก่อน"
        nextWeek = "สัปดาห์ถัดไป"
        stillClockedIn = "ยังเข้างานอยู่"
        warnBreak = { worked, brk, required -> "ทำงาน $worked แต่พักแค่ $brk – ต้องพักอย่างน้อย $required" }
        warnWeekMax = { week, worked, max -> "สัปดาห์ของ $week: ทำงาน $worked – กฎหมายกำหนดไม่เกิน $max ชม." }
        warnForgotClockOut = { date -> "ลืมกดออกงานวันที่ $date – แก้ช่วงเวลาเข้างานด้วย" }
        sapProjects = "โปรเจกต์"
        projectsSubtitle = "เลขและชื่อโปรเจกต์จากฝ่ายบริหารโครงการ พร้อมโปรเจกต์ «ไม่ใช่งานโปรเจกต์» ที่มีมาให้"
        importBtn = "นำเข้า"
        importedResult = { added, skipped -> "นำเข้า $added โปรเจกต์" + if (skipped > 0) " และมีอยู่แล้ว $skipped" else "" }
        filterAll = "ทั้งหมด"
        filterActive = "กำลังทำ"
        filterOnHold = "พักไว้"
        filterCompleted = "เสร็จแล้ว"
        noProjectsTitle = "ยังไม่มีโปรเจกต์"
        noProjectsSubtitle = "แตะ + เพื่อเพิ่มโปรเจกต์ที่ต้องลงชั่วโมง"
        booked = "ลงแล้ว"
        tasks = "งาน"
        done = "เสร็จ"
        newProject = "โปรเจกต์ใหม่"
        editProject = "แก้ไขโปรเจกต์"
        deleteProject = "ลบโปรเจกต์"
        projectNumber = "เลขโปรเจกต์ *"
        projectNumberHint = "เช่น P-2026-0142"
        projectName = "ชื่อโปรเจกต์ *"
        customer = "ลูกค้า / เครื่องจักร (ไม่บังคับ)"
        plannedHours = "ชั่วโมงที่วางแผน"
        status = "สถานะ"
        colourTag = "สีประจำโปรเจกต์"
        addProject = "เพิ่มโปรเจกต์"
        workSchedule = "ตารางเวลาทำงาน"
        editSchedule = "แก้ไขตาราง"
        workCategories = "หมวดงาน"
        categoriesSubtitle = "PLC, ภาษาระดับสูง, ประชุม… หมวดที่ไม่ใช่งานโปรเจกต์จะถูกแยกรายงาน"
        productive = "งานโปรเจกต์"
        unproductiveLabel = "ไม่ใช่งานโปรเจกต์"
        newCategory = "หมวดงานใหม่"
        editCategory = "แก้ไขหมวด"
        categoryName = "ชื่อ *"
        categoryPlaceholder = "เช่น PLC, ประชุม, พักกาแฟ"
        productiveWork = "โปรเจกต์ที่นับเป็นงาน"
        unproductiveTime = "โปรเจกต์ที่ไม่นับเป็นงาน"
        productiveHint = "ชั่วโมงนับเป็นงานโปรเจกต์และเข้าไปในไฟล์ส่งออกใบบันทึกเวลา"
        unproductiveHint = "บันทึกเวลาไว้แต่รายงานแยก (ประชุม, พัก, …)"
        colour = "สี"
        languageTitle = "ภาษา"
        languageSubtitle = "ภาษาของแอป"
        statusActive = "กำลังทำ"
        statusOnHold = "พักไว้"
        statusCompleted = "เสร็จแล้ว"
        hrsLogged = { "บันทึกแล้ว $it ชม." }
        budget = { "งบ: $it ชม." }
        scheduleSubtitle = "ชั่วโมงของแต่ละวันในสัปดาห์ – เช่น ทำ 3 วันบวกเรียน, 80 % หยุดหนึ่งวัน, 90 % หยุดครึ่งวัน"
        hoursPerWeekAt100 = "ชั่วโมง/สัปดาห์ ที่ 100 %"
        legalMaxPerWeek = "สูงสุดตามกฎหมาย ชม./สัปดาห์"
        hoursPerDay = "เป้าชั่วโมงต่อวัน"
        hoursPerDayHint = "0 = วันหยุด"
        schedulePreview = { weekly, percent, days -> "$weekly ชม./สัปดาห์ ($percent %) · ทำงาน $days วัน" }
        checkValues = "กรุณาตรวจสอบค่าที่กรอก"
        perWeek = "ชม./สัปดาห์"
        perDay = "ชม./วัน"
        maxPerWeek = "สูงสุด"
        bookAbsence = "บันทึกการลา"
        day = "วัน"
        reason = "เหตุผล"
        reasonRequired = "เหตุผล *"
        reasonPlaceholder = "เช่น เกณฑ์ทหาร, พบแพทย์"
        hoursFullDay = { "ชั่วโมง (เต็มวัน = $it)" }
        creditsHint = "นับรวมในเป้าของสัปดาห์ (ลาแบบได้รับค่าจ้าง)"
        fromOvertimeHint = "หักจากยอดโอทีที่สะสมไว้ – ไม่มีการนับชั่วโมงเพิ่ม"
        enterHours = "กรอกจำนวนชั่วโมง เช่น 8 หรือ 4"
        importProjects = "นำเข้าโปรเจกต์"
        importDescription = "วางบรรทัดละหนึ่งโปรเจกต์: เลข, ชื่อ และลูกค้า (ถ้ามี) – คั่นด้วยแท็บ, ';' หรือ ',' คัดลอกเซลล์จากโปรแกรมตารางคำนวณมาวางได้เลย เลขโปรเจกต์ที่มีอยู่แล้วจะถูกข้าม"
        projectList = "รายการโปรเจกต์"
        importPlaceholder = "P-2026-0142;ปรับปรุงสปินเดิล;บริษัทลูกค้า\nP-2026-0150;HMI ใหม่"
        noneRecognised = "ยังไม่พบโปรเจกต์"
        recognised = { n, list -> "พบ $n โปรเจกต์: $list" }
        importN = { "นำเข้า $it" }
        logActivity = "บันทึกงาน"
        editActivity = "แก้ไขงาน"
        sapProject = "โปรเจกต์"
        category = "งาน"
        selectCategory = "เลือกงาน"
        taskOptional = "งาน (ไม่บังคับ)"
        noSpecificTask = "งานทั่วไป"
        whatDidYouDo = "ทำอะไรไป?"
        startLabel = "เริ่ม"
        endLabel = "สิ้นสุด"
        pickStart = "เลือกเวลาเริ่ม"
        pickEnd = "เลือกเวลาสิ้นสุด"
        stillRunning = "งานนี้ยังทำอยู่"
        endAfterStart = "เวลาสิ้นสุดต้องหลังเวลาเริ่ม"
        duration = { "ระยะเวลา: $it" }
        noProjectOption = "ยังไม่มีโปรเจกต์ / ไม่ได้อยู่ในแอป"
        unproductiveSuffix = "(ไม่ใช่งานโปรเจกต์)"
        addProjectOption = "+ เพิ่มโปรเจกต์ใหม่…"
        unassignedProject = "ยังไม่ได้กำหนดโปรเจกต์"
        unassignedHint = "แก้ไขรายการนี้ภายหลังเพื่อกำหนดโปรเจกต์หรืองาน"
        addTaskOption = "+ เพิ่มงานใหม่…"
        newTaskTitle = "งานใหม่"
        noteHint = "บันทึกย่อ – เพิ่มตอนนี้ ระหว่างทำ หรือทีหลังก็ได้"
        moveTask = "ย้ายไปโปรเจกต์อื่น"
        moveTaskTo = "ย้ายงานไปที่"
        noTasksInProject = "ยังไม่มีงาน"
        tasksCount = { "$it งาน" }
        todayOnThisTask = "วันนี้กับงานนี้"
        thisSession = "ครั้งนี้"
        switchToThis = "สลับมาทำงานนี้"
        continueThis = "ทำงานนี้ต่อ"
        addNote = "เพิ่มบันทึกย่อ"
        editNote = "แก้ไขบันทึกย่อ"
        untilNow = "จนถึงตอนนี้"
        durationChip = { "+$it" }
        quickDuration = "ระยะเวลา"
        noActivityLogged = "ไม่มีงานที่บันทึก"
        assignGap = "กำหนด"
        previousActivityEnds = { name, time -> "ก่อนหน้า: $name (จบ $time)" }
        nextActivityStarts = { name, time -> "ถัดไป: $name (เริ่ม $time)" }
        gapBetween = { "จะเหลือช่วงว่าง $it ที่ไม่มีงาน" }
        overlapBetween = { "ทับกันอยู่ $it" }
        moveNeighbourTo = { "ย้ายไปที่ $it" }
        leaveGap = "เว้นว่างไว้ กำหนดทีหลัง"
        keepOverlap = "คงไว้แบบนี้"
        neighbourWouldBeEmpty = "ถ้าย้าย งานนั้นจะไม่เหลือเวลา แก้ที่งานนั้นแทนดีกว่า"
        addPeriod = "เพิ่มช่วงเวลาเข้างาน"
        editPeriod = "แก้ไขช่วงเวลาเข้างาน"
        clockInLabel = "เข้างาน"
        clockOutLabel = "ออกงาน"
        pickTime = "เลือกเวลา"
        clockOutReason = "เหตุผลที่ออกงาน"
        clockOutAfterIn = "เวลาออกงานต้องหลังเวลาเข้างาน"
        exportTitle = "ส่งออกชั่วโมงทำงาน"
        report = "รายงาน"
        fileFormat = "รูปแบบไฟล์"
        roundQuarter = "ปัดเป็น 0.25 ชม. สำหรับลงเวลา (ไม่งั้นใช้ค่าจริง เช่น 7.78)"
        preview = "ตัวอย่าง"
        copy = "คัดลอก"
        copied = "คัดลอกแล้ว!"
        copiedToClipboard = "คัดลอกไปยังคลิปบอร์ดแล้ว!"
        saveShare = "บันทึก / แชร์ CSV"
        saveFile = "บันทึกไฟล์"
        shareFile = "แชร์ (อีเมล, แชต, …)"
        moreRows = "… [ยังมีอีก]"
        taskFilterTodo = "ต้องทำ"
        taskFilterInProgress = "กำลังทำ"
        taskFilterDone = "เสร็จแล้ว"
        noTasksTitle = "ไม่พบงาน"
        noTasksSubtitle = "เพิ่มงานที่หัวหน้าโครงการมอบหมาย พร้อมกำหนดส่งและการเตือน"
        toggleComplete = "สลับสถานะเสร็จ"
        track = "จับเวลา"
        bookedPlanned = { b, p -> "ลงแล้ว ${b} ชม. / วางแผน ${p} ชม." }
        newTask = "งานใหม่ในโปรเจกต์"
        editTask = "แก้ไขงาน"
        taskTitle = "ชื่องาน *"
        descriptionNotes = "รายละเอียดและบันทึก"
        priority = "ความสำคัญ"
        estHours = "ชั่วโมงที่ประเมิน"
        deadlineReminders = "กำหนดส่งและการเตือนอัตโนมัติ"
        noDeadline = "ยังไม่ได้ตั้งกำหนดส่ง"
        setDeadline = "ตั้งกำหนดส่ง"
        saveTask = "บันทึกงาน"
        deadlineTime = "เวลากำหนดส่ง"
        selectProject = "เลือกโปรเจกต์"
        addTask = "เพิ่มงาน"
        priorityLow = "ต่ำ"
        priorityMedium = "กลาง"
        priorityHigh = "สูง"
        priorityUrgent = "ด่วน"
        statusTodo = "ต้องทำ"
        statusInProgress = "กำลังทำ"
        statusReview = "รอตรวจ"
        statusDone = "เสร็จแล้ว"
        overdueDays = { "เลยมา ${it} วัน" }
        overdueHours = { "เลยมา ${it} ชม." }
        dueUnder2h = "ครบกำหนดใน < 2 ชม.!"
        dueInHours = { "ครบกำหนดใน ${it} ชม." }
        dueTomorrow = "ครบกำหนดพรุ่งนี้"
        dueOn = { "ครบกำหนด $it" }
        absenceSick = "ลาป่วย"; absenceHoliday = "ลาพักร้อน"; absencePublicHoliday = "วันหยุดนักขัตฤกษ์ (บริษัทจ่าย)"
        absenceCompensation = "ชดเชย (หักจากโอที)"; absenceEducation = "อบรม / เรียน"; absenceOther = "เหตุผลอื่น"
        reasonLunch = "พักเที่ยง"; reasonBreak = "พัก"; reasonOut = "ออกนอกออฟฟิศ"; reasonHome = "กลับบ้าน"
        periodDay = "วัน"; periodWeek = "สัปดาห์"; periodMonth = "เดือน"
        exportSummary = "สรุปต่อโปรเจกต์"; exportTimesheet = "ใบบันทึกเวลารายวัน"; exportAttendance = "การเข้างานและโอที"
        exportSapWeek = "ใบรายสัปดาห์ (คัดลอก & วาง)"
        sapWeekHint = "คั่นด้วยแท็บ หนึ่งบล็อกต่อหนึ่งสัปดาห์ เริ่มวันจันทร์: คัดลอกแล้ววางลงใบบันทึกเวลารายสัปดาห์ของคุณ ชั่วโมงมีทศนิยมสองตำแหน่ง ช่องว่างหมายถึงไม่มีอะไรต้องลง บรรทัดที่ขึ้นต้นด้วย ! คือเวลาที่ยังไม่มีโปรเจกต์ – ลงเองหรือกำหนดโปรเจกต์ก่อน"
        sapSettingsTitle = "การลงใบบันทึกเวลา"
        sapSettingsSubtitle = "ประเภทกิจกรรมและรหัสศูนย์ต้นทุนที่ใช้ในไฟล์คัดลอก & วางรายสัปดาห์"
        sapProductiveType = "ประเภทกิจกรรม – งานโปรเจกต์"
        sapUnproductiveType = "ประเภทกิจกรรม – ไม่ใช่งานโปรเจกต์"
        sapUnproductiveNumber = "รหัสศูนย์ต้นทุนสำหรับเวลาที่ไม่ใช่งานโปรเจกต์"
        undo = "เลิกทำ"; activityDeleted = "ลบงานแล้ว"; moreActions = "ดูเพิ่มเติม"
        deleteProjectQuestion = { "ลบโปรเจกต์ $it?" }
        deleteProjectWarning = "งานในโปรเจกต์จะถูกลบด้วย เวลาที่ลงไว้แล้วยังอยู่แต่จะไม่มีโปรเจกต์ ต้องกำหนดใหม่"
        deleteTaskQuestion = { "ลบงาน «$it»?" }
        deleteTaskWarning = "เวลาที่ลงไว้กับงานนี้ยังอยู่ แต่จะไม่มีงานกำกับ"
        deletePeriodQuestion = "ลบช่วงเวลาเข้างานนี้? เวลาเข้างานและโอทีของวันนั้นจะเปลี่ยน"
        deleteAbsenceQuestion = "ลบการลานี้? ชั่วโมงของมันจะไม่ถูกนับอีก"
        targetLabel = { "เป้า $it" }; remainingToTarget = { "เหลืออีก $it" }; targetReached = "ถึงเป้าแล้ว"
        eventClockedIn = "เข้างาน"; eventClockedOut = "ออกงาน"
        noActivity = "ไม่มีงานที่กำลังทำ"; generalTaskTime = "ไม่ระบุงาน"
        clockInFirst = "กดเข้างานก่อน แล้วค่อยเลือกว่าจะทำอะไร"
        paidBreak = "พักแบบได้ค่าจ้าง"; paidBreakPerDay = "พักได้ค่าจ้างต่อวัน (นาที)"
        paidBreakHint = "เวลาพักกาแฟ/สูบบุหรี่ที่ติดป้าย «พัก» จะนับเป็นเวลาทำงานได้ไม่เกินจำนวนนาทีนี้ต่อวัน 0 = ไม่ได้ค่าจ้าง"
        paidBreakSummary = { "พักได้ค่าจ้าง $it นาที" }
        breakRulesTitle = "เวลาพักที่ต้องมี"
        breakRulesHint = "เวลาพักที่วันหนึ่งต้องมี เมื่อเข้างานถึงจำนวนชั่วโมงนี้ พักเที่ยงและพักย่อยนับรวมกัน"
        ruleAfterHours = "มากกว่า (ชม.)"
        ruleBreakMinutes = "พัก (นาที)"
        addRule = "เพิ่มกฎ"
        deductMissingBreak = "หักเวลาพักที่ไม่ได้พักโดยอัตโนมัติ"
        deductMissingBreakHint = "ถ้าพักน้อยกว่าที่กำหนด ส่วนต่างจะถูกหักออกจากเวลาทำงานที่นับ: เข้างาน 9 ชม. 30 น. พัก 30 นาที แต่ต้องพัก 1 ชม. จะนับเป็น 9 ชม."
        missingBreakDeducted = "− เวลาพักที่ขาด"
        breakRuleSummary = { hours, minutes -> "$minutes นาที > $hours ชม." }
        warnBreakDeducted = { "หักแล้ว $it" }
        formatCsv = "CSV (จุลภาค)"; formatExcel = "ตารางคำนวณ (อัฒภาค)"
        backupTitle = "สำรองและย้ายข้อมูล"; backupSubtitle = "บันทึกทุกอย่าง (โปรเจกต์ งาน เวลา การลา การตั้งค่า) เป็นไฟล์เดียว แล้วกู้คืนบนเครื่องอื่นได้"
        backupShare = "แชร์"; backupRestore = "กู้คืนจากไฟล์"
        backupExported = "สร้างไฟล์สำรองแล้ว"; backupRestored = "กู้คืนข้อมูลแล้ว"
        backupContents = { s -> "${s.projects} โปรเจกต์ · ${s.tasks} งาน · ${s.timeEntries} รายการงาน · ${s.attendanceSessions} ช่วงเข้างาน · ${s.dayRecords} การลา" }
        restoreQuestion = { "กู้คืนข้อมูลจาก $it?" }
        restoreWarning = "ข้อมูลทั้งหมดบนเครื่องนี้จะถูกแทนที่ด้วยไฟล์สำรอง และย้อนกลับไม่ได้ – ถ้าไม่แน่ใจ ให้สำรองข้อมูลเครื่องนี้ไว้ก่อน"
        restoreConfirm = "แทนที่และกู้คืน"
        backupErrorNotBackup = "ไฟล์นี้ไม่ใช่ไฟล์สำรองของ WorkTracker"
        backupErrorNewer = "ไฟล์สำรองถูกสร้างจากแอปเวอร์ชันใหม่กว่า อัปเดตแอปแล้วลองอีกครั้ง"
        backupErrorCorrupt = "ไฟล์สำรองเสียหาย อ่านไม่ได้"
        backupErrorIo = "อ่านหรือเขียนไฟล์ไม่ได้"
        autoBackupTitle = "สำรองอัตโนมัติ"
        autoBackupSubtitle = "เก็บสำเนาทุกอย่างไว้ในโฟลเดอร์ที่เลือก – คลาวด์ไดรฟ์, iCloud Drive หรือโฟลเดอร์ในเครื่อง – หลังการเปลี่ยนแปลงไม่กี่วินาที ติดตั้งใหม่แล้วกู้คืนจากโฟลเดอร์นั้นได้"
        autoBackupOff = "ปิดอยู่ – ยังไม่ได้เลือกโฟลเดอร์"
        autoBackupFolder = { "โฟลเดอร์: $it" }
        autoBackupLast = { "สำรองล่าสุด $it" }
        autoBackupPending = "ยังไม่ได้เขียนไฟล์"
        autoBackupFailed = "เขียนไฟล์สำรองครั้งล่าสุดไม่สำเร็จ ตรวจว่าโฟลเดอร์ยังอยู่ หรือเลือกใหม่อีกครั้ง"
        autoBackupNoFile = "ยังไม่มีไฟล์สำรองในโฟลเดอร์นั้น"
        chooseFolder = "เลือกโฟลเดอร์"
        changeFolder = "เปลี่ยน"
        turnOff = "ปิด"
        backupNow = "สำรองเดี๋ยวนี้"
        restoreFromFolder = "กู้คืนจากโฟลเดอร์"
        licensesTitle = "ไลเซนส์โอเพนซอร์ส"
        licensesSubtitle = "ไลบรารีที่ใช้สร้างแอปนี้ และเงื่อนไขของแต่ละตัว"
        licensesIntro = "WorkTracker สร้างขึ้นด้วยซอฟต์แวร์โอเพนซอร์ส ไลบรารีด้านล่างรวมอยู่ในแอป ลิขสิทธิ์ยังเป็นของผู้เขียนแต่ละราย"
        licensesApacheNotice = "อยู่ภายใต้ Apache License เวอร์ชัน 2.0 การใช้ไฟล์เหล่านี้ต้องเป็นไปตามไลเซนส์ ดูฉบับเต็มได้ที่:"
        importAppsTitle = "นำเข้าจากแอปอื่น"
        importAppsSubtitle = "ย้ายประวัติเดิมเข้ามา รองรับไฟล์ CSV ที่ส่งออกจากแอป WORK บน iOS (Export → CSV) วันที่มีข้อมูลอยู่แล้วจะถูกข้าม"
        importWorkButton = "ไฟล์ส่งออกจาก WORK (CSV)"
        workImportTitle = "นำเข้าไฟล์จาก WORK"
        workImportSummary = { s, e, a, p, t -> "$s ช่วงเข้างาน · $e รายการงาน · $a การลา" + if (p > 0 || t > 0) " · จะสร้าง $p โปรเจกต์ และ $t งาน" else "" }
        workImportSkipped = { "$it แถวมีอยู่แล้วและถูกข้าม" }
        workImportNothing = "ไม่มีข้อมูลใหม่ให้นำเข้า"
        workImportConfirm = "นำเข้า"
        workImported = { s, e -> "นำเข้า $s ช่วงเข้างาน และ $e รายการงาน" }
        notWorkExport = "ไฟล์นี้ไม่ใช่ไฟล์ส่งออกจาก WORK"
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

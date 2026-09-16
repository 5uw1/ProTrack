package com.suw1labs.worktracker.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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
class AppStrings(
    val language: Language,
    // common
    val cancel: String, val save: String, val add: String, val edit: String, val delete: String, val close: String,
    val next: String, val back: String, val ok: String, val yes: String, val no: String,
    val monthsShort: List<String>, val monthsLong: List<String>, val weekdaysShort: List<String>, val weekdaysTwo: List<String>,
    val weekdaysLong: List<String>,
    // tabs / app bar
    val tabToday: String, val tabProjects: String, val tabTasks: String, val tabReports: String,
    val badgeWorking: String, val badgeClockedIn: String,
    // today
    val clockedOut: String, val clockedInSince: (String) -> String, val clockedInToday: String,
    val clockIn: String, val clockOut: String, val currentActivity: String, val whatWorkingOn: String,
    val switchActivity: String, val switchShort: String, val start: String, val stop: String,
    val noteOptional: String, val notePlaceholder: String, val autoClockInHint: String,
    val absence: String, val editAbsence: String, val clockedIn: String, val projectWork: String,
    val unproductive: String, val overtimeToday: String, val activities: String,
    val activitiesSubtitle: (Int) -> String, val manualEntry: String, val noActivitiesTitle: String,
    val noActivitiesSubtitle: String, val noProject: String, val since: (String) -> String, val running: String,
    val deadlineAlerts: (Int) -> String, val workOnIt: String, val snooze: String, val uncategorized: String,
    val credited: String, val fromOvertime: String,
    val targetReachedTitle: String, val targetReachedBody: (String) -> String,
    // reports
    val sapExport: String, val target: String, val absencesCredited: String, val overtime: String,
    val balanceToDate: String, val hoursPerProject: String, val noProjectHours: String, val total: String,
    val workingTimeRules: String, val absences: String, val noneInPeriod: String, val clockInPeriods: String,
    val notClockedInOnDay: String, val noActivitiesOnDay: String, val breaks: String,
    val previousMonth: String, val nextMonth: String, val previousWeek: String, val nextWeek: String,
    val stillClockedIn: String,
    val warnBreak: (worked: String, brk: String, required: String) -> String,
    val warnWeekMax: (weekStart: String, worked: String, max: String) -> String,
    val warnForgotClockOut: (date: String) -> String,
    // projects & setup
    val sapProjects: String, val projectsSubtitle: String, val importBtn: String,
    val importedResult: (Int, Int) -> String, val filterAll: String, val filterActive: String,
    val filterOnHold: String, val filterCompleted: String, val noProjectsTitle: String,
    val noProjectsSubtitle: String, val booked: String, val tasks: String, val done: String,
    val newProject: String, val editProject: String, val projectNumber: String, val projectNumberHint: String,
    val projectName: String, val customer: String, val plannedHours: String, val status: String,
    val colourTag: String, val addProject: String, val workSchedule: String, val editSchedule: String,
    val workCategories: String, val categoriesSubtitle: String, val productive: String, val unproductiveLabel: String,
    val newCategory: String, val editCategory: String, val categoryName: String, val categoryPlaceholder: String,
    val productiveWork: String, val unproductiveTime: String, val productiveHint: String, val unproductiveHint: String,
    val colour: String, val languageTitle: String, val languageSubtitle: String,
    val statusActive: String, val statusOnHold: String, val statusCompleted: String,
    val hrsLogged: (String) -> String, val budget: (String) -> String,
    // schedule dialog
    val scheduleSubtitle: String, val hoursPerWeekAt100: String, val legalMaxPerWeek: String,
    val hoursPerDay: String, val hoursPerDayHint: String, val schedulePreview: (weekly: String, percent: String, days: Int) -> String,
    val checkValues: String, val perWeek: String, val perDay: String, val maxPerWeek: String,
    // absence dialog
    val bookAbsence: String, val day: String, val reason: String, val reasonRequired: String,
    val reasonPlaceholder: String, val hoursFullDay: (String) -> String, val creditsHint: String,
    val fromOvertimeHint: String, val enterHours: String,
    // import dialog
    val importProjects: String, val importDescription: String, val projectList: String,
    val importPlaceholder: String, val noneRecognised: String, val recognised: (Int, String) -> String,
    val importN: (Int) -> String,
    // entry dialog
    val logActivity: String, val editActivity: String, val sapProject: String, val category: String,
    val selectCategory: String, val taskOptional: String, val noSpecificTask: String, val whatDidYouDo: String,
    val startLabel: String, val endLabel: String, val pickStart: String, val pickEnd: String,
    val stillRunning: String, val endAfterStart: String, val duration: (String) -> String, val noProjectOption: String,
    val unproductiveSuffix: String, val addProjectOption: String, val unassignedProject: String, val unassignedHint: String,
    val addTaskOption: String, val newTaskTitle: String, val noteHint: String,
    val moveTask: String, val moveTaskTo: String, val noTasksInProject: String, val tasksCount: (Int) -> String,
    val todayOnThisTask: String, val thisSession: String,
    // session dialog
    val addPeriod: String, val editPeriod: String, val clockInLabel: String, val clockOutLabel: String,
    val pickTime: String, val clockOutReason: String, val clockOutAfterIn: String,
    // export dialog
    val exportTitle: String, val report: String, val fileFormat: String, val roundQuarter: String,
    val preview: String, val copy: String, val copied: String, val copiedToClipboard: String, val saveShare: String,
    val saveFile: String, val shareFile: String,
    val moreRows: String,
    // tasks
    val taskFilterTodo: String, val taskFilterInProgress: String, val taskFilterDone: String,
    val noTasksTitle: String, val noTasksSubtitle: String, val toggleComplete: String, val track: String,
    val bookedPlanned: (String, String) -> String, val newTask: String, val editTask: String,
    val taskTitle: String, val descriptionNotes: String, val priority: String, val estHours: String,
    val deadlineReminders: String, val noDeadline: String, val setDeadline: String, val saveTask: String,
    val deadlineTime: String, val selectProject: String, val addTask: String,
    val priorityLow: String, val priorityMedium: String, val priorityHigh: String, val priorityUrgent: String,
    val statusTodo: String, val statusInProgress: String, val statusReview: String, val statusDone: String,
    val overdueDays: (Int) -> String, val overdueHours: (Int) -> String, val dueUnder2h: String,
    val dueInHours: (Int) -> String, val dueTomorrow: String, val dueOn: (String) -> String,
) {
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
        SapExportType.MONTHLY_SUMMARY -> exportSummary
        SapExportType.DAILY_TIMESHEET -> exportTimesheet
        SapExportType.ATTENDANCE -> exportAttendance
    }
    var exportSummary = ""; var exportTimesheet = ""; var exportAttendance = ""

    fun formatLabel(format: ExportFormat): String = when (format) {
        ExportFormat.CSV -> formatCsv
        ExportFormat.EXCEL_CSV -> formatExcel
    }
    var formatCsv = ""; var formatExcel = ""

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
        )
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

    val EN: AppStrings = AppStrings(
        language = Language.EN,
        cancel = "Cancel", save = "Save", add = "Add", edit = "Edit", delete = "Delete", close = "Close",
        next = "Next", back = "Back", ok = "OK", yes = "Yes", no = "No",
        monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        monthsLong = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"),
        weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
        weekdaysTwo = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"),
        weekdaysLong = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
        tabToday = "Today", tabProjects = "Settings", tabTasks = "Projects", tabReports = "Reports",
        badgeWorking = "WORKING", badgeClockedIn = "CLOCKED IN",
        clockedOut = "CLOCKED OUT", clockedInSince = { "CLOCKED IN since $it" }, clockedInToday = "clocked in today",
        clockIn = "CLOCK IN", clockOut = "CLOCK OUT", currentActivity = "CURRENT ACTIVITY", whatWorkingOn = "WHAT ARE YOU WORKING ON?",
        switchActivity = "SWITCH ACTIVITY", switchShort = "Switch", start = "Start", stop = "Stop",
        noteOptional = "Note (optional)", notePlaceholder = "e.g. Spindle PLC safety logic, HMI screen…",
        autoClockInHint = "Starting an activity clocks you in automatically.",
        absence = "Absence", editAbsence = "Edit absence", clockedIn = "Clocked in", projectWork = "Project work",
        unproductive = "Unproductive", overtimeToday = "Overtime today", activities = "Activities",
        activitiesSubtitle = { "$it today · edit to adjust project or times" }, manualEntry = "Manual entry",
        noActivitiesTitle = "No activities yet", noActivitiesSubtitle = "Clock in and start an activity, or log one manually.",
        noProject = "No project", since = { "since $it" }, running = "running",
        deadlineAlerts = { "Deadline alerts ($it)" }, workOnIt = "Work on it", snooze = "Snooze +24h", uncategorized = "Uncategorized",
        credited = "credited", fromOvertime = "from overtime",
        targetReachedTitle = "Daily target reached 🎉", targetReachedBody = { "You have clocked in $it today. Time to go home?" },
        sapExport = "Export", target = "Target", absencesCredited = "Absences credited", overtime = "Overtime",
        balanceToDate = "Balance to date", hoursPerProject = "Hours per project", noProjectHours = "No project hours in this period.",
        total = "Total", workingTimeRules = "Working-time rules", absences = "Absences", noneInPeriod = "None in this period.",
        clockInPeriods = "Clock-in periods", notClockedInOnDay = "Not clocked in on this day.", noActivitiesOnDay = "No activities on this day.",
        breaks = "Breaks", previousMonth = "Previous month", nextMonth = "Next month", previousWeek = "Previous week", nextWeek = "Next week",
        stillClockedIn = "still clocked in",
        warnBreak = { worked, brk, required -> "Worked $worked with only $brk break – at least $required required." },
        warnWeekMax = { week, worked, max -> "Week of $week: $worked worked – legal maximum is $max h." },
        warnForgotClockOut = { date -> "Forgot to clock out on $date – fix the clock-in period." },
        sapProjects = "Projects", projectsSubtitle = "Project numbers and names from project management, plus the built-in Unproductive project.",
        importBtn = "Import", importedResult = { added, skipped -> "Imported $added project(s)" + if (skipped > 0) ", $skipped already existed." else "." },
        filterAll = "All", filterActive = "Active", filterOnHold = "On hold", filterCompleted = "Completed",
        noProjectsTitle = "No projects yet", noProjectsSubtitle = "Tap + to add the projects you book hours on.",
        booked = "BOOKED", tasks = "TASKS", done = "done", newProject = "New project", editProject = "Edit project",
        projectNumber = "Project number *", projectNumberHint = "e.g. P-2026-0142", projectName = "Project name *",
        customer = "Customer / machine (optional)", plannedHours = "Planned hours", status = "Status", colourTag = "Colour tag",
        addProject = "Add project", workSchedule = "Work schedule", editSchedule = "Edit schedule",
        workCategories = "Work categories", categoriesSubtitle = "PLC, High Level Language, Meeting… Unproductive ones are reported separately.",
        productive = "Productive", unproductiveLabel = "Unproductive", newCategory = "New work category", editCategory = "Edit category",
        categoryName = "Name *", categoryPlaceholder = "e.g. PLC, Meeting, Coffee break", productiveWork = "Productive project",
        unproductiveTime = "Unproductive project", productiveHint = "Hours count as project work and are exported for SAP",
        unproductiveHint = "Time is recorded but reported separately (meetings, breaks, …)", colour = "Colour",
        languageTitle = "Language", languageSubtitle = "App language",
        statusActive = "Active", statusOnHold = "On hold", statusCompleted = "Completed",
        hrsLogged = { "$it hrs logged" }, budget = { "Budget: $it hrs" },
        scheduleSubtitle = "Hours per weekday – e.g. 3 days + education, 80 % with a day off, 90 % with a half day.",
        hoursPerWeekAt100 = "Hours / week at 100 %", legalMaxPerWeek = "Legal max h / week", hoursPerDay = "Target hours per day",
        hoursPerDayHint = "0 = day off", schedulePreview = { weekly, percent, days -> "$weekly h per week ($percent %) · $days working days" },
        checkValues = "Please check the values.", perWeek = "h/week", perDay = "h/day", maxPerWeek = "max",
        bookAbsence = "Book absence", day = "Day", reason = "Reason", reasonRequired = "Reason *",
        reasonPlaceholder = "e.g. Military service, doctor", hoursFullDay = { "Hours (full day = $it)" },
        creditsHint = "Counts towards the weekly target (paid absence).", fromOvertimeHint = "Taken from your overtime balance – no hours are credited.",
        enterHours = "Enter the hours, e.g. 8 or 4.",
        importProjects = "Import projects",
        importDescription = "Paste one project per line: number, name and optionally customer – separated by tab, ';' or ','. Copying cells from Excel works directly. Existing project numbers are skipped.",
        projectList = "Project list", importPlaceholder = "P-2026-0142;Spindle retrofit;Customer AG\nP-2026-0150;New HMI",
        noneRecognised = "No projects recognised yet.", recognised = { n, list -> "$n project(s) recognised: $list" }, importN = { "Import $it" },
        logActivity = "Log activity", editActivity = "Edit activity", sapProject = "Project", category = "Task",
        selectCategory = "Select task", taskOptional = "Task (optional)", noSpecificTask = "General (no specific task)",
        whatDidYouDo = "What did you do?", startLabel = "Start", endLabel = "End", pickStart = "Pick start", pickEnd = "Pick end",
        stillRunning = "This activity is still running.", endAfterStart = "End must be after start.", duration = { "Duration: $it" },
        noProjectOption = "No project yet / not in app", unproductiveSuffix = "(unproductive)",
        addProjectOption = "+ Add new project…", unassignedProject = "Project not assigned yet", unassignedHint = "Edit the entry to assign a project or task later.",
        addTaskOption = "+ Add new task…", newTaskTitle = "New task", noteHint = "Note – add it now, while working or afterwards",
        moveTask = "Move to another project", moveTaskTo = "Move task to", noTasksInProject = "No tasks yet", tasksCount = { "$it tasks" },
        todayOnThisTask = "today on this task", thisSession = "this session",
        addPeriod = "Add clock-in period", editPeriod = "Edit clock-in period", clockInLabel = "Clock in", clockOutLabel = "Clock out",
        pickTime = "Pick time", clockOutReason = "Clock-out reason", clockOutAfterIn = "Clock out must be after clock in.",
        exportTitle = "Hours export", report = "Report", fileFormat = "File format", roundQuarter = "Round to 0.25 h for SAP (otherwise exact, e.g. 7.78)",
        preview = "Preview", copy = "Copy", copied = "Copied!", copiedToClipboard = "Copied to clipboard!", saveShare = "Save / share CSV", saveFile = "Save file", shareFile = "Share (e-mail, Teams, …)",
        moreRows = "... [more rows]",
        taskFilterTodo = "To-Do", taskFilterInProgress = "In progress", taskFilterDone = "Done",
        noTasksTitle = "No tasks found", noTasksSubtitle = "Add the tasks your project manager assigned, with deadlines and reminders.",
        toggleComplete = "Toggle complete", track = "Track", bookedPlanned = { b, p -> "${b}h booked / ${p}h planned" },
        newTask = "New project task", editTask = "Edit task", taskTitle = "Task title *", descriptionNotes = "Description & notes",
        priority = "Priority", estHours = "Est. hours", deadlineReminders = "Deadline & automated reminders", noDeadline = "No deadline set",
        setDeadline = "Set deadline", saveTask = "Save task", deadlineTime = "Deadline time", selectProject = "Select project", addTask = "Add task",
        priorityLow = "Low", priorityMedium = "Med", priorityHigh = "High", priorityUrgent = "Urgent",
        statusTodo = "Todo", statusInProgress = "In progress", statusReview = "Review", statusDone = "Done",
        overdueDays = { "${it}d overdue" }, overdueHours = { "${it}h overdue" }, dueUnder2h = "Due in < 2h!",
        dueInHours = { "Due in ${it}h" }, dueTomorrow = "Due tomorrow", dueOn = { "Due $it" },
    ).apply {
        absenceSick = "Sick"; absenceHoliday = "Holiday (own vacation)"; absencePublicHoliday = "Public holiday (paid by company)"
        absenceCompensation = "Compensation (from overtime)"; absenceEducation = "Education / training"; absenceOther = "Other reason"
        reasonLunch = "Lunch"; reasonBreak = "Break"; reasonOut = "Out of office"; reasonHome = "Go home"
        periodDay = "Day"; periodWeek = "Week"; periodMonth = "Month"
        exportSummary = "Summary per project"; exportTimesheet = "Daily timesheet"; exportAttendance = "Attendance & overtime"
        formatCsv = "CSV (comma)"; formatExcel = "Excel (semicolon)"
    }

    val DE: AppStrings = AppStrings(
        language = Language.DE,
        cancel = "Abbrechen", save = "Speichern", add = "Hinzufügen", edit = "Bearbeiten", delete = "Löschen", close = "Schliessen",
        next = "Weiter", back = "Zurück", ok = "OK", yes = "Ja", no = "Nein",
        monthsShort = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"),
        monthsLong = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"),
        weekdaysShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
        weekdaysTwo = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
        weekdaysLong = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"),
        tabToday = "Heute", tabProjects = "Einstellungen", tabTasks = "Projekte", tabReports = "Berichte",
        badgeWorking = "ARBEITET", badgeClockedIn = "EINGESTEMPELT",
        clockedOut = "AUSGESTEMPELT", clockedInSince = { "EINGESTEMPELT seit $it" }, clockedInToday = "heute eingestempelt",
        clockIn = "EINSTEMPELN", clockOut = "AUSSTEMPELN", currentActivity = "AKTUELLE TÄTIGKEIT", whatWorkingOn = "WORAN ARBEITEST DU?",
        switchActivity = "TÄTIGKEIT WECHSELN", switchShort = "Wechseln", start = "Start", stop = "Stopp",
        noteOptional = "Notiz (optional)", notePlaceholder = "z.B. SPS Sicherheitslogik Spindel, HMI-Bild…",
        autoClockInHint = "Beim Starten einer Tätigkeit wird automatisch eingestempelt.",
        absence = "Abwesenheit", editAbsence = "Abwesenheit bearbeiten", clockedIn = "Eingestempelt", projectWork = "Projektarbeit",
        unproductive = "Unproduktiv", overtimeToday = "Überzeit heute", activities = "Tätigkeiten",
        activitiesSubtitle = { "$it heute · bearbeiten, um Projekt oder Zeiten anzupassen" }, manualEntry = "Manueller Eintrag",
        noActivitiesTitle = "Noch keine Tätigkeiten", noActivitiesSubtitle = "Einstempeln und Tätigkeit starten oder manuell erfassen.",
        noProject = "Kein Projekt", since = { "seit $it" }, running = "läuft",
        deadlineAlerts = { "Termin-Warnungen ($it)" }, workOnIt = "Daran arbeiten", snooze = "+24h verschieben", uncategorized = "Nicht zugeordnet",
        credited = "gutgeschrieben", fromOvertime = "aus Überzeit",
        targetReachedTitle = "Tagessoll erreicht 🎉", targetReachedBody = { "Du bist heute $it eingestempelt. Zeit für den Feierabend?" },
        sapExport = "Export", target = "Soll", absencesCredited = "Abwesenheiten gutgeschrieben", overtime = "Überzeit",
        balanceToDate = "Saldo bis heute", hoursPerProject = "Stunden pro Projekt", noProjectHours = "Keine Projektstunden in diesem Zeitraum.",
        total = "Total", workingTimeRules = "Arbeitszeitregeln", absences = "Abwesenheiten", noneInPeriod = "Keine in diesem Zeitraum.",
        clockInPeriods = "Stempelzeiten", notClockedInOnDay = "An diesem Tag nicht eingestempelt.", noActivitiesOnDay = "Keine Tätigkeiten an diesem Tag.",
        breaks = "Pausen", previousMonth = "Vorheriger Monat", nextMonth = "Nächster Monat", previousWeek = "Vorherige Woche", nextWeek = "Nächste Woche",
        stillClockedIn = "noch eingestempelt",
        warnBreak = { worked, brk, required -> "$worked gearbeitet mit nur $brk Pause – mindestens $required erforderlich." },
        warnWeekMax = { week, worked, max -> "Woche vom $week: $worked gearbeitet – gesetzliches Maximum ist $max h." },
        warnForgotClockOut = { date -> "Am $date nicht ausgestempelt – Stempelzeit korrigieren." },
        sapProjects = "Projekte", projectsSubtitle = "Projektnummern und Namen der Projektleitung sowie das eingebaute Projekt Unproduktiv.",
        importBtn = "Import", importedResult = { added, skipped -> "$added Projekt(e) importiert" + if (skipped > 0) ", $skipped bereits vorhanden." else "." },
        filterAll = "Alle", filterActive = "Aktiv", filterOnHold = "Pausiert", filterCompleted = "Fertig",
        noProjectsTitle = "Noch keine Projekte", noProjectsSubtitle = "Mit + die Projekte erfassen, auf die du Stunden buchst.",
        booked = "GEBUCHT", tasks = "AUFGABEN", done = "erledigt", newProject = "Neues Projekt", editProject = "Projekt bearbeiten",
        projectNumber = "Projektnummer *", projectNumberHint = "z.B. P-2026-0142", projectName = "Projektname *",
        customer = "Kunde / Maschine (optional)", plannedHours = "Geplante Stunden", status = "Status", colourTag = "Farbe",
        addProject = "Projekt hinzufügen", workSchedule = "Arbeitszeitmodell", editSchedule = "Arbeitszeitmodell bearbeiten",
        workCategories = "Tätigkeitskategorien", categoriesSubtitle = "SPS, Hochsprache, Meeting… Unproduktive werden separat ausgewiesen.",
        productive = "Produktiv", unproductiveLabel = "Unproduktiv", newCategory = "Neue Kategorie", editCategory = "Kategorie bearbeiten",
        categoryName = "Name *", categoryPlaceholder = "z.B. SPS, Meeting, Kaffeepause", productiveWork = "Produktives Projekt",
        unproductiveTime = "Unproduktives Projekt", productiveHint = "Stunden zählen als Projektarbeit und werden für SAP exportiert",
        unproductiveHint = "Zeit wird erfasst, aber separat ausgewiesen (Meetings, Pausen, …)", colour = "Farbe",
        languageTitle = "Sprache", languageSubtitle = "App-Sprache",
        statusActive = "Aktiv", statusOnHold = "Pausiert", statusCompleted = "Fertig",
        hrsLogged = { "$it Std. gebucht" }, budget = { "Budget: $it Std." },
        scheduleSubtitle = "Stunden pro Wochentag – z.B. 3 Tage + Ausbildung, 80 % mit freiem Tag, 90 % mit halbem Tag.",
        hoursPerWeekAt100 = "Stunden / Woche bei 100 %", legalMaxPerWeek = "Gesetzl. Max. h / Woche", hoursPerDay = "Sollstunden pro Tag",
        hoursPerDayHint = "0 = frei", schedulePreview = { weekly, percent, days -> "$weekly h pro Woche ($percent %) · $days Arbeitstage" },
        checkValues = "Bitte Werte prüfen.", perWeek = "h/Woche", perDay = "h/Tag", maxPerWeek = "max.",
        bookAbsence = "Abwesenheit buchen", day = "Tag", reason = "Grund", reasonRequired = "Grund *",
        reasonPlaceholder = "z.B. Militär, Arzt", hoursFullDay = { "Stunden (ganzer Tag = $it)" },
        creditsHint = "Zählt zum Wochensoll (bezahlte Abwesenheit).", fromOvertimeHint = "Wird vom Überzeitsaldo abgezogen – keine Gutschrift.",
        enterHours = "Stunden eingeben, z.B. 8 oder 4.",
        importProjects = "Projekte importieren",
        importDescription = "Ein Projekt pro Zeile einfügen: Nummer, Name und optional Kunde – getrennt durch Tab, ';' oder ','. Aus Excel kopierte Zellen funktionieren direkt. Vorhandene Projektnummern werden übersprungen.",
        projectList = "Projektliste", importPlaceholder = "P-2026-0142;Spindel Retrofit;Kunde AG\nP-2026-0150;Neues HMI",
        noneRecognised = "Noch keine Projekte erkannt.", recognised = { n, list -> "$n Projekt(e) erkannt: $list" }, importN = { "$it importieren" },
        logActivity = "Tätigkeit erfassen", editActivity = "Tätigkeit bearbeiten", sapProject = "Projekt", category = "Aufgabe",
        selectCategory = "Aufgabe wählen", taskOptional = "Aufgabe (optional)", noSpecificTask = "Allgemein (keine bestimmte Aufgabe)",
        whatDidYouDo = "Was hast du gemacht?", startLabel = "Start", endLabel = "Ende", pickStart = "Start wählen", pickEnd = "Ende wählen",
        stillRunning = "Diese Tätigkeit läuft noch.", endAfterStart = "Ende muss nach Start liegen.", duration = { "Dauer: $it" },
        noProjectOption = "Noch kein Projekt / nicht in der App", unproductiveSuffix = "(unproduktiv)",
        addProjectOption = "+ Neues Projekt…", unassignedProject = "Noch keinem Projekt zugeordnet", unassignedHint = "Eintrag bearbeiten, um später Projekt oder Aufgabe zuzuordnen.",
        addTaskOption = "+ Neue Aufgabe…", newTaskTitle = "Neue Aufgabe", noteHint = "Notiz – jetzt, während der Arbeit oder danach",
        moveTask = "In anderes Projekt verschieben", moveTaskTo = "Aufgabe verschieben nach", noTasksInProject = "Noch keine Aufgaben", tasksCount = { "$it Aufgaben" },
        todayOnThisTask = "heute an dieser Aufgabe", thisSession = "diese Sitzung",
        addPeriod = "Stempelzeit hinzufügen", editPeriod = "Stempelzeit bearbeiten", clockInLabel = "Einstempeln", clockOutLabel = "Ausstempeln",
        pickTime = "Zeit wählen", clockOutReason = "Grund fürs Ausstempeln", clockOutAfterIn = "Ausstempeln muss nach Einstempeln liegen.",
        exportTitle = "Stundenexport", report = "Bericht", fileFormat = "Dateiformat", roundQuarter = "Auf 0.25 h runden (sonst exakt, z.B. 7.78)",
        preview = "Vorschau", copy = "Kopieren", copied = "Kopiert!", copiedToClipboard = "In die Zwischenablage kopiert!", saveShare = "CSV speichern / teilen", saveFile = "Datei speichern", shareFile = "Teilen (E-Mail, Teams, …)",
        moreRows = "... [weitere Zeilen]",
        taskFilterTodo = "Offen", taskFilterInProgress = "In Arbeit", taskFilterDone = "Erledigt",
        noTasksTitle = "Keine Aufgaben", noTasksSubtitle = "Aufgaben der Projektleitung mit Terminen und Erinnerungen erfassen.",
        toggleComplete = "Erledigt umschalten", track = "Starten", bookedPlanned = { b, p -> "${b}h gebucht / ${p}h geplant" },
        newTask = "Neue Aufgabe", editTask = "Aufgabe bearbeiten", taskTitle = "Titel *", descriptionNotes = "Beschreibung & Notizen",
        priority = "Priorität", estHours = "Gesch. Std.", deadlineReminders = "Termin & automatische Erinnerung", noDeadline = "Kein Termin",
        setDeadline = "Termin setzen", saveTask = "Aufgabe speichern", deadlineTime = "Terminzeit", selectProject = "Projekt wählen", addTask = "Aufgabe hinzufügen",
        priorityLow = "Tief", priorityMedium = "Mittel", priorityHigh = "Hoch", priorityUrgent = "Dringend",
        statusTodo = "Offen", statusInProgress = "In Arbeit", statusReview = "Review", statusDone = "Erledigt",
        overdueDays = { "${it}T überfällig" }, overdueHours = { "${it}h überfällig" }, dueUnder2h = "Fällig in < 2h!",
        dueInHours = { "Fällig in ${it}h" }, dueTomorrow = "Fällig morgen", dueOn = { "Fällig $it" },
    ).apply {
        absenceSick = "Krank"; absenceHoliday = "Ferien"; absencePublicHoliday = "Feiertag (bezahlt)"
        absenceCompensation = "Kompensation (aus Überzeit)"; absenceEducation = "Ausbildung / Schulung"; absenceOther = "Anderer Grund"
        reasonLunch = "Mittag"; reasonBreak = "Pause"; reasonOut = "Ausser Haus"; reasonHome = "Feierabend"
        periodDay = "Tag"; periodWeek = "Woche"; periodMonth = "Monat"
        exportSummary = "Summe pro Projekt"; exportTimesheet = "Tagesrapport"; exportAttendance = "Anwesenheit & Überzeit"
        formatCsv = "CSV (Komma)"; formatExcel = "Excel (Semikolon)"
    }

    val FR: AppStrings = AppStrings(
        language = Language.FR,
        cancel = "Annuler", save = "Enregistrer", add = "Ajouter", edit = "Modifier", delete = "Supprimer", close = "Fermer",
        next = "Suivant", back = "Retour", ok = "OK", yes = "Oui", no = "Non",
        monthsShort = listOf("janv", "févr", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc"),
        monthsLong = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
        weekdaysShort = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim"),
        weekdaysTwo = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"),
        weekdaysLong = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"),
        tabToday = "Aujourd'hui", tabProjects = "Réglages", tabTasks = "Projets", tabReports = "Rapports",
        badgeWorking = "EN COURS", badgeClockedIn = "POINTÉ",
        clockedOut = "DÉPOINTÉ", clockedInSince = { "POINTÉ depuis $it" }, clockedInToday = "pointé aujourd'hui",
        clockIn = "POINTER", clockOut = "DÉPOINTER", currentActivity = "ACTIVITÉ EN COURS", whatWorkingOn = "SUR QUOI TRAVAILLES-TU ?",
        switchActivity = "CHANGER D'ACTIVITÉ", switchShort = "Changer", start = "Démarrer", stop = "Arrêter",
        noteOptional = "Note (optionnel)", notePlaceholder = "p.ex. logique de sécurité API broche, écran IHM…",
        autoClockInHint = "Démarrer une activité pointe automatiquement.",
        absence = "Absence", editAbsence = "Modifier l'absence", clockedIn = "Pointé", projectWork = "Travail projet",
        unproductive = "Improductif", overtimeToday = "Heures sup. aujourd'hui", activities = "Activités",
        activitiesSubtitle = { "$it aujourd'hui · modifier pour ajuster projet ou heures" }, manualEntry = "Saisie manuelle",
        noActivitiesTitle = "Pas encore d'activité", noActivitiesSubtitle = "Pointe et démarre une activité, ou saisis-la manuellement.",
        noProject = "Sans projet", since = { "depuis $it" }, running = "en cours",
        deadlineAlerts = { "Alertes d'échéance ($it)" }, workOnIt = "Y travailler", snooze = "Reporter +24h", uncategorized = "Non catégorisé",
        credited = "crédité", fromOvertime = "des heures sup.",
        targetReachedTitle = "Objectif du jour atteint 🎉", targetReachedBody = { "Tu as pointé $it aujourd'hui. L'heure de rentrer ?" },
        sapExport = "Export", target = "Objectif", absencesCredited = "Absences créditées", overtime = "Heures supplémentaires",
        balanceToDate = "Solde à ce jour", hoursPerProject = "Heures par projet", noProjectHours = "Aucune heure projet sur cette période.",
        total = "Total", workingTimeRules = "Règles du temps de travail", absences = "Absences", noneInPeriod = "Aucune sur cette période.",
        clockInPeriods = "Périodes pointées", notClockedInOnDay = "Pas pointé ce jour-là.", noActivitiesOnDay = "Aucune activité ce jour-là.",
        breaks = "Pauses", previousMonth = "Mois précédent", nextMonth = "Mois suivant", previousWeek = "Semaine précédente", nextWeek = "Semaine suivante",
        stillClockedIn = "encore pointé",
        warnBreak = { worked, brk, required -> "$worked travaillées avec seulement $brk de pause – au moins $required requis." },
        warnWeekMax = { week, worked, max -> "Semaine du $week : $worked travaillées – maximum légal $max h." },
        warnForgotClockOut = { date -> "Oubli de dépointer le $date – corriger la période pointée." },
        sapProjects = "Projets", projectsSubtitle = "Numéros et noms de projets de la direction de projet, plus le projet intégré Improductif.",
        importBtn = "Importer", importedResult = { added, skipped -> "$added projet(s) importé(s)" + if (skipped > 0) ", $skipped déjà existant(s)." else "." },
        filterAll = "Tous", filterActive = "Actifs", filterOnHold = "En pause", filterCompleted = "Terminés",
        noProjectsTitle = "Pas encore de projet", noProjectsSubtitle = "Touche + pour ajouter les projets sur lesquels tu imputes des heures.",
        booked = "IMPUTÉ", tasks = "TÂCHES", done = "terminées", newProject = "Nouveau projet", editProject = "Modifier le projet",
        projectNumber = "Numéro de projet *", projectNumberHint = "p.ex. P-2026-0142", projectName = "Nom du projet *",
        customer = "Client / machine (optionnel)", plannedHours = "Heures planifiées", status = "Statut", colourTag = "Couleur",
        addProject = "Ajouter un projet", workSchedule = "Horaire de travail", editSchedule = "Modifier l'horaire",
        workCategories = "Catégories de travail", categoriesSubtitle = "API, langage évolué, réunion… Les improductives sont rapportées séparément.",
        productive = "Productif", unproductiveLabel = "Improductif", newCategory = "Nouvelle catégorie", editCategory = "Modifier la catégorie",
        categoryName = "Nom *", categoryPlaceholder = "p.ex. API, Réunion, Pause café", productiveWork = "Projet productif",
        unproductiveTime = "Projet improductif", productiveHint = "Les heures comptent comme travail projet et sont exportées pour SAP",
        unproductiveHint = "Le temps est enregistré mais rapporté séparément (réunions, pauses, …)", colour = "Couleur",
        languageTitle = "Langue", languageSubtitle = "Langue de l'application",
        statusActive = "Actif", statusOnHold = "En pause", statusCompleted = "Terminé",
        hrsLogged = { "$it h imputées" }, budget = { "Budget : $it h" },
        scheduleSubtitle = "Heures par jour de semaine – p.ex. 3 jours + formation, 80 % avec un jour libre, 90 % avec une demi-journée.",
        hoursPerWeekAt100 = "Heures / semaine à 100 %", legalMaxPerWeek = "Max légal h / semaine", hoursPerDay = "Heures cibles par jour",
        hoursPerDayHint = "0 = congé", schedulePreview = { weekly, percent, days -> "$weekly h par semaine ($percent %) · $days jours de travail" },
        checkValues = "Veuillez vérifier les valeurs.", perWeek = "h/sem.", perDay = "h/jour", maxPerWeek = "max",
        bookAbsence = "Saisir une absence", day = "Jour", reason = "Motif", reasonRequired = "Motif *",
        reasonPlaceholder = "p.ex. service militaire, médecin", hoursFullDay = { "Heures (journée complète = $it)" },
        creditsHint = "Compte pour l'objectif hebdomadaire (absence payée).", fromOvertimeHint = "Déduit du solde d'heures supplémentaires – rien n'est crédité.",
        enterHours = "Saisis les heures, p.ex. 8 ou 4.",
        importProjects = "Importer des projets",
        importDescription = "Colle un projet par ligne : numéro, nom et client optionnel – séparés par tabulation, ';' ou ','. Les cellules copiées d'Excel fonctionnent directement. Les numéros existants sont ignorés.",
        projectList = "Liste de projets", importPlaceholder = "P-2026-0142;Rétrofit broche;Client SA\nP-2026-0150;Nouvelle IHM",
        noneRecognised = "Aucun projet reconnu pour l'instant.", recognised = { n, list -> "$n projet(s) reconnu(s) : $list" }, importN = { "Importer $it" },
        logActivity = "Saisir une activité", editActivity = "Modifier l'activité", sapProject = "Projet", category = "Tâche",
        selectCategory = "Choisir une tâche", taskOptional = "Tâche (optionnel)", noSpecificTask = "Général (aucune tâche précise)",
        whatDidYouDo = "Qu'as-tu fait ?", startLabel = "Début", endLabel = "Fin", pickStart = "Choisir le début", pickEnd = "Choisir la fin",
        stillRunning = "Cette activité est encore en cours.", endAfterStart = "La fin doit être après le début.", duration = { "Durée : $it" },
        noProjectOption = "Projet inconnu / pas encore dans l'app", unproductiveSuffix = "(improductif)",
        addProjectOption = "+ Nouveau projet…", unassignedProject = "Projet pas encore attribué", unassignedHint = "Modifie l'entrée pour attribuer un projet ou une tâche plus tard.",
        addTaskOption = "+ Nouvelle tâche…", newTaskTitle = "Nouvelle tâche", noteHint = "Note – maintenant, pendant ou après le travail",
        moveTask = "Déplacer vers un autre projet", moveTaskTo = "Déplacer la tâche vers", noTasksInProject = "Pas encore de tâche", tasksCount = { "$it tâches" },
        todayOnThisTask = "aujourd'hui sur cette tâche", thisSession = "cette session",
        addPeriod = "Ajouter une période pointée", editPeriod = "Modifier la période pointée", clockInLabel = "Pointer", clockOutLabel = "Dépointer",
        pickTime = "Choisir l'heure", clockOutReason = "Motif du dépointage", clockOutAfterIn = "Le dépointage doit être après le pointage.",
        exportTitle = "Export des heures", report = "Rapport", fileFormat = "Format de fichier", roundQuarter = "Arrondir à 0.25 h (sinon exact, p.ex. 7.78)",
        preview = "Aperçu", copy = "Copier", copied = "Copié !", copiedToClipboard = "Copié dans le presse-papiers !", saveShare = "Enregistrer / partager CSV", saveFile = "Enregistrer le fichier", shareFile = "Partager (e-mail, Teams, …)",
        moreRows = "... [plus de lignes]",
        taskFilterTodo = "À faire", taskFilterInProgress = "En cours", taskFilterDone = "Terminées",
        noTasksTitle = "Aucune tâche", noTasksSubtitle = "Ajoute les tâches assignées par le chef de projet, avec échéances et rappels.",
        toggleComplete = "Basculer terminé", track = "Démarrer", bookedPlanned = { b, p -> "${b}h imputées / ${p}h planifiées" },
        newTask = "Nouvelle tâche", editTask = "Modifier la tâche", taskTitle = "Titre *", descriptionNotes = "Description & notes",
        priority = "Priorité", estHours = "Heures est.", deadlineReminders = "Échéance & rappels automatiques", noDeadline = "Pas d'échéance",
        setDeadline = "Fixer l'échéance", saveTask = "Enregistrer la tâche", deadlineTime = "Heure de l'échéance", selectProject = "Choisir un projet", addTask = "Ajouter une tâche",
        priorityLow = "Basse", priorityMedium = "Moy.", priorityHigh = "Haute", priorityUrgent = "Urgent",
        statusTodo = "À faire", statusInProgress = "En cours", statusReview = "Revue", statusDone = "Terminé",
        overdueDays = { "${it}j de retard" }, overdueHours = { "${it}h de retard" }, dueUnder2h = "Échéance < 2h !",
        dueInHours = { "Échéance dans ${it}h" }, dueTomorrow = "Échéance demain", dueOn = { "Échéance $it" },
    ).apply {
        absenceSick = "Maladie"; absenceHoliday = "Vacances"; absencePublicHoliday = "Jour férié (payé)"
        absenceCompensation = "Compensation (heures sup.)"; absenceEducation = "Formation"; absenceOther = "Autre motif"
        reasonLunch = "Midi"; reasonBreak = "Pause"; reasonOut = "Hors bureau"; reasonHome = "Fin de journée"
        periodDay = "Jour"; periodWeek = "Semaine"; periodMonth = "Mois"
        exportSummary = "Résumé par projet"; exportTimesheet = "Feuille journalière"; exportAttendance = "Présence & heures sup."
        formatCsv = "CSV (virgule)"; formatExcel = "Excel (point-virgule)"
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

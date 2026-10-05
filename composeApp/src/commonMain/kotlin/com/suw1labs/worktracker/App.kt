package com.suw1labs.worktracker

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.SnackbarHostState
import com.suw1labs.worktracker.ui.shell.LocalAppShell
import com.suw1labs.worktracker.ui.shell.ShellState
import com.suw1labs.worktracker.ui.shell.ShellTab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suw1labs.worktracker.ui.i18n.Language
import com.suw1labs.worktracker.ui.i18n.LocalStrings
import com.suw1labs.worktracker.ui.i18n.Translations
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.platform.NotificationPermissionEffect
import com.suw1labs.worktracker.ui.components.LocalModalPresence
import com.suw1labs.worktracker.ui.components.ModalPresence
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnScroll
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.screens.ProjectsScreen
import com.suw1labs.worktracker.ui.screens.ReportsScreen
import com.suw1labs.worktracker.ui.screens.TasksScreen
import com.suw1labs.worktracker.ui.screens.TodayScreen
import com.suw1labs.worktracker.ui.timesheet.TimesheetScreen
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats

enum class TrackerDestination(
    val icon: ImageVector,
    /** SF Symbol for the native iOS tab bar; the Material icon above is used everywhere else. */
    val systemImage: String,
    val tag: String,
) {
    TODAY(Icons.Default.Timer, "timer", "nav_today"),
    /** Only on wide windows (desktop, tablets): the week on one screen, edited with mouse and keyboard. */
    TIMESHEET(Icons.Default.CalendarViewWeek, "calendar", "nav_timesheet"),
    TASKS(Icons.Default.Folder, "folder.fill", "nav_projects"),
    REPORTS(Icons.Default.Insights, "chart.line.uptrend.xyaxis", "nav_reports"),
    PROJECTS(Icons.Default.Settings, "gearshape.fill", "nav_settings");

    @Composable
    fun title(): String = when (this) {
        TODAY -> strings.tabToday
        TIMESHEET -> strings.tabTimesheet
        PROJECTS -> strings.tabProjects
        TASKS -> strings.tabTasks
        REPORTS -> strings.tabReports
    }

    companion object {
        /** "today", "timesheet", "tasks", "reports" or "settings" (as the tabs read on screen), for launch options. */
        fun fromLaunchName(name: String): TrackerDestination? = when (name.trim().lowercase()) {
            "today" -> TODAY
            "timesheet" -> TIMESHEET
            "tasks", "projects" -> TASKS
            "reports" -> REPORTS
            "settings" -> PROJECTS
            else -> null
        }
    }
}

/**
 * Root composable shared by Android, iOS and desktop.
 */
@Composable
fun App(container: AppContainer, launchOptions: AppLaunchOptions = AppLaunchOptions.NONE) {
    MyApplicationTheme {
        val viewModel: TrackerViewModel = viewModel { container.createViewModel() }
        val settings by viewModel.settings.collectAsState()
        val translation = remember(settings.language) { Translations.forLanguage(Language.fromCode(settings.language)) }
        CompositionLocalProvider(LocalStrings provides translation) {
            MainAppContent(viewModel = viewModel, initialDestination = launchOptions.initialTab ?: TrackerDestination.TODAY, askNotificationPermission = !launchOptions.demo)
        }
    }
}

@Composable
fun MainAppContent(viewModel: TrackerViewModel, initialDestination: TrackerDestination = TrackerDestination.TODAY, askNotificationPermission: Boolean = true) {
    var currentDestination by remember { mutableStateOf(initialDestination) }
    val now by viewModel.now.collectAsState()
    val urgentTasks by viewModel.urgentTasks.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    val runningEntry by viewModel.runningEntry.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Ask for notification permission where needed, then surface urgent deadlines.
    // Not in demo mode: the system permission dialog would sit on top of every store screenshot.
    if (askNotificationPermission) NotificationPermissionEffect { viewModel.checkUpcomingDeadlines() }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalModalPresence provides remember { ModalPresence() },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().dismissKeyboardOnScroll().dismissKeyboardOnTap()
        ) {
            val wide = this.maxWidth >= 720.dp
            // The timesheet needs the room of a wide window; a narrow one falls back to Today.
            val destination = if (!wide && currentDestination == TrackerDestination.TIMESHEET) TrackerDestination.TODAY else currentDestination
            val state = ShellState(
                // Today shows the current weekday and date, e.g. "Tuesday, 16 Sep 2026".
                title = if (destination == TrackerDestination.TODAY) DateFormats.weekdayLongDate(now) else destination.title(),
                status = openSession?.let { if (runningEntry != null) strings.badgeWorking else strings.badgeClockedIn },
                alertCount = urgentTasks.size,
                onAlertClick = { currentDestination = TrackerDestination.TASKS },
                tabs = TrackerDestination.entries.filter { wide || it != TrackerDestination.TIMESHEET }.map { tab ->
                    ShellTab(
                        label = tab.title(),
                        icon = tab.icon,
                        systemImage = tab.systemImage,
                        selected = destination == tab,
                        testTag = tab.tag,
                        badgeCount = if (tab == TrackerDestination.TASKS) urgentTasks.size else 0,
                        onClick = { currentDestination = tab },
                    )
                },
                snackbarHostState = snackbarHostState,
                fullWidth = destination == TrackerDestination.TIMESHEET,
            )

            // Which chrome is drawn around the screens is the shell's business, not this file's.
            LocalAppShell.current.Chrome(state = state, wide = wide) { modifier ->
                DestinationContent(
                    destination = destination,
                    viewModel = viewModel,
                    onNavigateToTimer = { currentDestination = TrackerDestination.TODAY },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun DestinationContent(
    destination: TrackerDestination,
    viewModel: TrackerViewModel,
    onNavigateToTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (destination) {
        TrackerDestination.TODAY -> {
            TodayScreen(viewModel = viewModel, modifier = modifier)
        }
        TrackerDestination.TIMESHEET -> {
            TimesheetScreen(viewModel = viewModel, modifier = modifier)
        }
        TrackerDestination.PROJECTS -> {
            ProjectsScreen(viewModel = viewModel, modifier = modifier)
        }
        TrackerDestination.TASKS -> {
            TasksScreen(
                viewModel = viewModel,
                onTrackTask = { task ->
                    viewModel.startActivityFromTask(task)
                    onNavigateToTimer()
                },
                modifier = modifier
            )
        }
        TrackerDestination.REPORTS -> {
            ReportsScreen(viewModel = viewModel, modifier = modifier)
        }
    }
}

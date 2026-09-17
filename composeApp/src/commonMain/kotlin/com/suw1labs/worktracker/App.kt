package com.suw1labs.worktracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suw1labs.worktracker.ui.i18n.Language
import com.suw1labs.worktracker.ui.i18n.LocalStrings
import com.suw1labs.worktracker.ui.i18n.Translations
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.platform.NotificationPermissionEffect
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnScroll
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.screens.ProjectsScreen
import com.suw1labs.worktracker.ui.screens.ReportsScreen
import com.suw1labs.worktracker.ui.screens.TasksScreen
import com.suw1labs.worktracker.ui.screens.TodayScreen
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats

enum class TrackerDestination(
    val icon: ImageVector,
    val tag: String,
) {
    TODAY(Icons.Default.Timer, "nav_today"),
    TASKS(Icons.Default.Folder, "nav_projects"),
    REPORTS(Icons.Default.Insights, "nav_reports"),
    PROJECTS(Icons.Default.Settings, "nav_settings");

    @Composable
    fun title(): String = when (this) {
        TODAY -> strings.tabToday
        PROJECTS -> strings.tabProjects
        TASKS -> strings.tabTasks
        REPORTS -> strings.tabReports
    }

    companion object {
        /** "today", "tasks", "reports" or "settings" (as the tabs read on screen), for launch options. */
        fun fromLaunchName(name: String): TrackerDestination? = when (name.trim().lowercase()) {
            "today" -> TODAY
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

@OptIn(ExperimentalMaterial3Api::class)
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

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().dismissKeyboardOnScroll().dismissKeyboardOnTap()
    ) {
        val isWideScreen = this.maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet & Desktop Responsive Layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("desktop_navigation_rail"),
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    // App mark: the rail is too narrow for the full name.
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "WorkTracker",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    TrackerDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = {
                                if ((destination == TrackerDestination.TASKS) && urgentTasks.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = RoseUrgent) {
                                                Text(urgentTasks.size.toString())
                                            }
                                        },
                                    ) {
                                        Icon(destination.icon, contentDescription = destination.title())
                                    }
                                } else {
                                    Icon(destination.icon, contentDescription = destination.title())
                                }
                            },
                            label = { Text(destination.title()) },
                            modifier = Modifier.testTag(destination.tag),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = if (currentDestination == TrackerDestination.TODAY) DateFormats.weekdayLongDate(now) else currentDestination.title(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background
                                ),
                                scrollBehavior = scrollBehavior
                            )
                        }
                    ) { innerPadding ->
                        // Cards stay readable on a wide window: content is capped and centred.
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
                            DestinationContent(
                                destination = currentDestination,
                                viewModel = viewModel,
                                onNavigateToTimer = { currentDestination = TrackerDestination.TODAY },
                                modifier = Modifier.widthIn(max = 720.dp).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        } else {
            // Handheld Mobile Phone Layout with Bottom Navigation Bar.
            // The compact title bar slides away while scrolling down and comes back on scroll up.
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    // Today shows the current weekday and date, e.g. "Tuesday, 16 Sep 2026".
                                    text = if (currentDestination == TrackerDestination.TODAY) DateFormats.weekdayLongDate(now) else currentDestination.title(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (openSession != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (runningEntry != null) strings.badgeWorking else strings.badgeClockedIn,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (urgentTasks.isNotEmpty()) {
                                IconButton(
                                    onClick = { currentDestination = TrackerDestination.TASKS },
                                    modifier = Modifier.testTag("top_deadline_alerts_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = RoseUrgent) {
                                                Text(urgentTasks.size.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = strings.deadlineAlerts(urgentTasks.size),
                                            tint = RoseUrgent
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background
                        ),
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("mobile_bottom_nav_bar")
                    ) {
                        TrackerDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    if ((destination == TrackerDestination.TASKS) && urgentTasks.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = RoseUrgent) {
                                                    Text(urgentTasks.size.toString())
                                                }
                                            }
                                        ) {
                                            Icon(destination.icon, contentDescription = destination.title())
                                        }
                                    } else {
                                        Icon(destination.icon, contentDescription = destination.title())
                                    }
                                },
                                label = { Text(destination.title()) },
                                modifier = Modifier.testTag(destination.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                DestinationContent(
                    destination = currentDestination,
                    viewModel = viewModel,
                    onNavigateToTimer = { currentDestination = TrackerDestination.TODAY },
                    modifier = Modifier.padding(innerPadding)
                )
            }
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

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnScroll
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.screens.ProjectsScreen
import com.suw1labs.worktracker.ui.screens.ReportsScreen
import com.suw1labs.worktracker.ui.screens.TasksScreen
import com.suw1labs.worktracker.ui.screens.TodayScreen
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel

enum class TrackerDestination(
    val icon: ImageVector,
    val tag: String,
) {
    TODAY(Icons.Default.Timer, "nav_today"),
    PROJECTS(Icons.Default.Folder, "nav_projects"),
    TASKS(Icons.AutoMirrored.Filled.Assignment, "nav_tasks"),
    REPORTS(Icons.Default.Insights, "nav_reports");

    @Composable
    fun title(): String = when (this) {
        TODAY -> strings.tabToday
        PROJECTS -> strings.tabProjects
        TASKS -> strings.tabTasks
        REPORTS -> strings.tabReports
    }
}

/**
 * Root composable shared by Android, iOS and desktop.
 */
@Composable
fun App(container: AppContainer) {
    MyApplicationTheme {
        val viewModel: TrackerViewModel = viewModel { container.createViewModel() }
        val settings by viewModel.settings.collectAsState()
        val translation = remember(settings.language) { Translations.forLanguage(Language.fromCode(settings.language)) }
        CompositionLocalProvider(LocalStrings provides translation) {
            MainAppContent(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: TrackerViewModel) {
    var currentDestination by remember { mutableStateOf(TrackerDestination.TODAY) }
    val urgentTasks by viewModel.urgentTasks.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    val runningEntry by viewModel.runningEntry.collectAsState()

    // Ask for notification permission where needed, then surface urgent deadlines.
    NotificationPermissionEffect { viewModel.checkUpcomingDeadlines() }

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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "WorkTracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = currentDestination.title(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
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
                        DestinationContent(
                            destination = currentDestination,
                            viewModel = viewModel,
                            onNavigateToTimer = { currentDestination = TrackerDestination.TODAY },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
            // Handheld Mobile Phone Layout with Bottom Navigation Bar.
            // The compact title bar slides away while scrolling down and comes back on scroll up.
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentDestination.title(),
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

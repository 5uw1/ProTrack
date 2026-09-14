package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.repository.TimeTrackerRepository
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoseUrgent
import com.example.ui.viewmodel.TrackerViewModel
import com.example.ui.viewmodel.TrackerViewModelFactory

enum class TrackerDestination(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    TIMER("Timer", Icons.Default.Timer, "nav_timer"),
    PROJECTS("Projects", Icons.Default.Folder, "nav_projects"),
    TASKS("Tasks", Icons.Default.Assignment, "nav_tasks"),
    REPORTS("Reports", Icons.Default.Insights, "nav_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = TimeTrackerRepository(
            projectDao = db.projectDao(),
            taskDao = db.workTaskDao(),
            timeEntryDao = db.timeEntryDao()
        )
        val factory = TrackerViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: TrackerViewModel = viewModel(factory = factory)
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: TrackerViewModel) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(TrackerDestination.TIMER) }
    val urgentTasks by viewModel.urgentTasks.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    // Request Notification Permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.checkUpcomingDeadlines(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.checkUpcomingDeadlines(context)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet & Desktop Responsive Layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("desktop_navigation_rail")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "WorkTracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TrackerDestination.values().forEach { destination ->
                        NavigationRailItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            icon = {
                                if (destination == TrackerDestination.TASKS && urgentTasks.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = RoseUrgent) {
                                                Text("${urgentTasks.size}")
                                            }
                                        }
                                    ) {
                                        Icon(destination.icon, contentDescription = destination.title)
                                    }
                                } else {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            },
                            label = { Text(destination.title) },
                            modifier = Modifier.testTag(destination.tag)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = currentDestination.title,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ) { innerPadding ->
                        DestinationContent(
                            destination = currentDestination,
                            viewModel = viewModel,
                            onNavigateToTimer = { currentDestination = TrackerDestination.TIMER },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } else {
            // Handheld Mobile Phone Layout with Bottom Navigation Bar
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WorkTracker",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (timerState.isRunning) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (timerState.isPaused) "PAUSED" else "TRACKING",
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
                                                Text("${urgentTasks.size}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "Deadlines",
                                            tint = RoseUrgent
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("mobile_bottom_nav_bar")
                    ) {
                        TrackerDestination.values().forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    if (destination == TrackerDestination.TASKS && urgentTasks.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = RoseUrgent) {
                                                    Text("${urgentTasks.size}")
                                                }
                                            }
                                        ) {
                                            Icon(destination.icon, contentDescription = destination.title)
                                        }
                                    } else {
                                        Icon(destination.icon, contentDescription = destination.title)
                                    }
                                },
                                label = { Text(destination.title) },
                                modifier = Modifier.testTag(destination.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                DestinationContent(
                    destination = currentDestination,
                    viewModel = viewModel,
                    onNavigateToTimer = { currentDestination = TrackerDestination.TIMER },
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
        TrackerDestination.TIMER -> {
            TimerScreen(viewModel = viewModel, modifier = modifier)
        }
        TrackerDestination.PROJECTS -> {
            ProjectsScreen(viewModel = viewModel, modifier = modifier)
        }
        TrackerDestination.TASKS -> {
            TasksScreen(
                viewModel = viewModel,
                onTrackTask = { task ->
                    viewModel.quickStartFromTask(task)
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

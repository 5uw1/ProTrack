package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.export.ExportManager
import com.example.data.model.Project
import com.example.data.model.TimeEntryWithDetails
import com.example.data.model.WorkTaskWithProject
import com.example.ui.components.DeadlineUrgencyBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseUrgent
import com.example.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val timeEntries by viewModel.allEntries.collectAsState()
    val urgentTasks by viewModel.urgentTasks.collectAsState()

    var showManualEntryDialog by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var descriptionInput by remember { mutableStateOf("") }
    var isBillable by remember { mutableStateOf(true) }
    var tagsInput by remember { mutableStateOf("") }
    var isProjectMenuOpen by remember { mutableStateOf(false) }
    var isTaskMenuOpen by remember { mutableStateOf(false) }

    // Synchronize default project selection if needed
    if (selectedProjectId == null && activeProjects.isNotEmpty()) {
        selectedProjectId = activeProjects.first().id
    }

    val availableTasksForProject = remember(selectedProjectId, allTasks) {
        if (selectedProjectId == null) emptyList()
        else allTasks.filter { it.projectId == selectedProjectId && it.status != "DONE" }
    }

    val currentActiveProject = remember(timerState.selectedProjectId, activeProjects) {
        activeProjects.find { it.id == timerState.selectedProjectId }
    }

    val currentActiveTask = remember(timerState.selectedTaskId, allTasks) {
        allTasks.find { it.id == timerState.selectedTaskId }
    }

    // Pulsing effect when running
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (timerState.isRunning && !timerState.isPaused) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // --- URGENT DEADLINES BANNER ---
        if (urgentTasks.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RoseUrgent.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_deadlines_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = RoseUrgent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Automated Deadline Alerts (${urgentTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoseUrgent
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val topUrgent = urgentTasks.first()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topUrgent.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${topUrgent.projectName} • ${topUrgent.client}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DeadlineUrgencyBadge(deadlineTimestamp = topUrgent.deadlineTimestamp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.quickStartFromTask(topUrgent) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseUrgent),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Now", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.snoozeTask(context, topUrgent.id, 24) },
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Snooze +24h", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- ACTIVE WORK TIMER CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_timer_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Running State Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusBg = when {
                            timerState.isRunning && !timerState.isPaused -> EmeraldGreen
                            timerState.isRunning && timerState.isPaused -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(statusBg)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                timerState.isRunning && !timerState.isPaused -> "TIMER RUNNING"
                                timerState.isRunning && timerState.isPaused -> "PAUSED"
                                else -> "READY TO TRACK"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = statusBg
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clock Dial Numbers
                    val displayTime = ExportManager.formatDurationHms(timerState.elapsedSeconds)
                    Text(
                        text = displayTime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("timer_display_text")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!timerState.isRunning) {
                        // Project Selector
                        ExposedDropdownMenuBox(
                            expanded = isProjectMenuOpen,
                            onExpandedChange = { isProjectMenuOpen = !isProjectMenuOpen }
                        ) {
                            val activeProj = activeProjects.find { it.id == selectedProjectId }
                            TextField(
                                value = activeProj?.let { "${it.name} (${it.client})" } ?: "Select Project",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Project") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProjectMenuOpen) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isProjectMenuOpen,
                                onDismissRequest = { isProjectMenuOpen = false }
                            ) {
                                activeProjects.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} - ${p.client}") },
                                        onClick = {
                                            selectedProjectId = p.id
                                            selectedTaskId = null
                                            isProjectMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Task Selector (Optional)
                        if (availableTasksForProject.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = isTaskMenuOpen,
                                onExpandedChange = { isTaskMenuOpen = !isTaskMenuOpen }
                            ) {
                                val currentTask = availableTasksForProject.find { it.id == selectedTaskId }
                                TextField(
                                    value = currentTask?.title ?: "General (No specific task)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Task (Optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTaskMenuOpen) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isTaskMenuOpen,
                                    onDismissRequest = { isTaskMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("General (No specific task)") },
                                        onClick = {
                                            selectedTaskId = null
                                            isTaskMenuOpen = false
                                        }
                                    )
                                    availableTasksForProject.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t.title) },
                                            onClick = {
                                                selectedTaskId = t.id
                                                descriptionInput = t.title
                                                isTaskMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text("What are you working on?") },
                            placeholder = { Text("e.g. Design review, API debugging...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timer_description_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = if (isBillable) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBillable) "Billable Session" else "Non-billable",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = isBillable,
                                onCheckedChange = { isBillable = it },
                                modifier = Modifier.testTag("billable_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                selectedProjectId?.let { pId ->
                                    viewModel.startTimer(
                                        projectId = pId,
                                        taskId = selectedTaskId,
                                        description = descriptionInput,
                                        isBillable = isBillable,
                                        tags = tagsInput
                                    )
                                }
                            },
                            enabled = selectedProjectId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_timer_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START TRACKING", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        // Current timer details
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = currentActiveProject?.name ?: "Project",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentActiveTask != null) {
                                    Text(
                                        text = "Task: ${currentActiveTask.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (timerState.description.isNotBlank()) {
                                    Text(
                                        text = timerState.description,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls: Pause/Resume, Stop, Discard
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (timerState.isPaused) {
                                Button(
                                    onClick = { viewModel.resumeTimer() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("resume_timer_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.pauseTimer() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pause_timer_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            }

                            Button(
                                onClick = { viewModel.stopTimer() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseUrgent),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("stop_timer_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop & Save")
                            }

                            IconButton(
                                onClick = { viewModel.discardTimer() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Discard", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // --- RECENT SESSIONS SECTION HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Work Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${timeEntries.size} logged sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showManualEntryDialog = true },
                    modifier = Modifier.testTag("manual_time_entry_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Entry", fontSize = 12.sp)
                }
            }
        }

        // Time Entry List
        if (timeEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Work,
                    title = "No time entries yet",
                    subtitle = "Start the timer above or log a manual entry to track work."
                )
            }
        } else {
            items(timeEntries.take(15), key = { it.id }) { entry ->
                TimeEntryRowCard(
                    entry = entry,
                    onContinue = { viewModel.quickStartFromEntry(entry) },
                    onDelete = { viewModel.deleteTimeEntry(entry.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Manual Time Entry Dialog
    if (showManualEntryDialog) {
        ManualTimeEntryDialog(
            projects = activeProjects,
            tasks = allTasks,
            onDismiss = { showManualEntryDialog = false },
            onSave = { pId, tId, desc, start, end, billable, tags ->
                viewModel.addManualTimeEntry(pId, tId, desc, start, end, billable, tags)
                showManualEntryDialog = false
            }
        )
    }
}

@Composable
fun TimeEntryRowCard(
    entry: TimeEntryWithDetails,
    onContinue: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(entry.startTime))
    val durationStr = ExportManager.formatDurationHms(entry.durationSeconds)
    val projColor = try {
        Color(android.graphics.Color.parseColor(entry.projectColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_entry_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(projColor)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.projectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.description.ifBlank { entry.taskTitle ?: "General work" },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (entry.isBillable) {
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Billable ($${String.format(Locale.US, "%.0f", entry.hourlyRate)}/h)",
                                fontSize = 10.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationStr,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row {
                    IconButton(onClick = onContinue, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTimeEntryDialog(
    projects: List<Project>,
    tasks: List<WorkTaskWithProject>,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, String, Long, Long, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var isBillable by remember { mutableStateOf(true) }
    var hoursInput by remember { mutableStateOf("1.5") }
    var isProjectOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Log Time Manually",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Project
                ExposedDropdownMenuBox(
                    expanded = isProjectOpen,
                    onExpandedChange = { isProjectOpen = !isProjectOpen }
                ) {
                    val pName = projects.find { it.id == selectedProjectId }?.name ?: "Select Project"
                    TextField(
                        value = pName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProjectOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isProjectOpen,
                        onDismissRequest = { isProjectOpen = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedProjectId = p.id
                                    isProjectOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Duration (Hours)") },
                    placeholder = { Text("e.g. 2.5") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Billable", fontWeight = FontWeight.Medium)
                    Switch(checked = isBillable, onCheckedChange = { isBillable = it })
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val hrs = hoursInput.toDoubleOrNull() ?: 1.0
                            val end = System.currentTimeMillis()
                            val start = end - (hrs * 3600 * 1000L).toLong()
                            onSave(selectedProjectId, selectedTaskId, description, start, end, isBillable, "")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Entry")
                    }
                }
            }
        }
    }
}

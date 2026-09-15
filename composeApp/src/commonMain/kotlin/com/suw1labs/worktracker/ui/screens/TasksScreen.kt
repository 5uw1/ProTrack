package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.components.DateTimePickerDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.DeadlineUrgencyBadge
import com.suw1labs.worktracker.ui.components.EmptyStateCard
import com.suw1labs.worktracker.ui.components.displayLabel
import com.suw1labs.worktracker.ui.components.PriorityBadge
import com.suw1labs.worktracker.ui.components.StatusBadge
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.formatFixed
import com.suw1labs.worktracker.util.projectColor
import com.suw1labs.worktracker.ui.i18n.strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TrackerViewModel,
    onTrackTask: (WorkTaskWithProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = strings
    val allTasks by viewModel.allTasks.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()

    var statusFilter by remember { mutableStateOf("ALL") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<WorkTaskWithProject?>(null) }

    val filteredTasks = remember(allTasks, statusFilter) {
        when (statusFilter) {
            "TODO" -> allTasks.filter { it.status == "TODO" }
            "IN_PROGRESS" -> allTasks.filter { it.status == "IN_PROGRESS" }
            "DONE" -> allTasks.filter { it.status == "DONE" }
            else -> allTasks
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Filter status chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    listOf("ALL" to t.filterAll, "TODO" to t.taskFilterTodo, "IN_PROGRESS" to t.taskFilterInProgress, "DONE" to t.taskFilterDone).forEach { (code, label) ->
                        FilterChip(
                            selected = statusFilter == code,
                            onClick = { statusFilter = code },
                            label = { Text(label) },
                            modifier = Modifier.testTag("task_filter_$code")
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Task,
                        title = t.noTasksTitle,
                        subtitle = t.noTasksSubtitle
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleDone = {
                            val newStatus = if (task.status == "DONE") "TODO" else "DONE"
                            viewModel.updateTaskStatus(task.id, newStatus)
                        },
                        onTrack = { onTrackTask(task) },
                        onEdit = { editingTask = task },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_task_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = t.addTask)
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        TaskFormDialog(
            task = null,
            projects = activeProjects,
            onDismiss = { showAddTaskDialog = false },
            onSave = { pId, title, desc, priority, estHours, deadline, leadHours ->
                viewModel.addTask(pId, title, desc, priority, estHours, deadline, leadHours)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog
    editingTask?.let { t ->
        TaskFormDialog(
            task = t,
            projects = activeProjects,
            onDismiss = { editingTask = null },
            onSave = { pId, title, desc, priority, estHours, deadline, leadHours ->
                viewModel.updateTask(
                    WorkTask(
                        id = t.id,
                        projectId = pId,
                        title = title,
                        description = desc,
                        priority = priority,
                        status = t.status,
                        estimatedHours = estHours,
                        deadlineTimestamp = deadline,
                        reminderLeadHours = leadHours,
                        reminderEnabled = deadline != null,
                        createdAt = t.createdAt
                    )
                )
                editingTask = null
            }
        )
    }
}

@Composable
fun TaskCard(
    task: WorkTaskWithProject,
    onToggleDone: () -> Unit,
    onTrack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val t = strings
    val isDone = task.status == "DONE"
    val projColor = projectColor(task.projectColor)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Checkbox, Title, Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggleDone,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = t.toggleComplete,
                        tint = if (isDone) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                PriorityBadge(priority = task.priority)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Project badge & deadline info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(projColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${task.projectCode} · ${task.projectName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (task.deadlineTimestamp != null && !isDone) {
                    DeadlineUrgencyBadge(deadlineTimestamp = task.deadlineTimestamp)
                } else if (isDone) {
                    StatusBadge(status = "DONE")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val loggedHours = task.loggedSeconds / 3600.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = t.bookedPlanned(loggedHours.formatFixed(1), task.estimatedHours.toString()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isDone) {
                        Button(
                            onClick = onTrack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("track_task_${task.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t.track, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = t.edit, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = t.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    task: WorkTaskWithProject?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Double, Long?, Int) -> Unit
) {
    val t = strings
    var selectedProjectId by remember { mutableStateOf(task?.projectId ?: (projects.firstOrNull()?.id ?: 0L)) }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var estimatedHoursStr by remember { mutableStateOf(task?.estimatedHours?.toString() ?: "4.0") }
    var deadlineTimestamp by remember { mutableStateOf<Long?>(task?.deadlineTimestamp) }
    var reminderLeadHours by remember { mutableStateOf(task?.reminderLeadHours ?: 24) }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("task_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (task == null) t.newTask else t.editTask,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Project Selector
                LabeledDropdown(
                    label = t.sapProject + " *",
                    selectedText = projects.find { it.id == selectedProjectId }?.displayLabel() ?: t.selectProject,
                    options = projects,
                    optionText = { it.displayLabel() },
                    onSelect = { selectedProjectId = it.id },
                    testTag = "task_project_dropdown"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t.taskTitle) },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(t.descriptionNotes) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Priority
                    LabeledDropdown(
                        label = t.priority,
                        selectedText = t.priorityName(priority),
                        options = listOf("LOW", "MEDIUM", "HIGH", "URGENT"),
                        optionText = { t.priorityName(it) },
                        onSelect = { priority = it },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = estimatedHoursStr,
                        onValueChange = { estimatedHoursStr = it },
                        label = { Text(t.estHours) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Deadline Picker Row
                Text(
                    text = t.deadlineReminders,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val deadlineDateText = deadlineTimestamp?.let { DateFormats.dateTime(it) } ?: t.noDeadline

                    Text(
                        text = deadlineDateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (deadlineTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showDeadlinePicker = true },
                        modifier = Modifier.testTag("set_deadline_button")
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t.setDeadline, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(t.cancel)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && selectedProjectId > 0) {
                                val est = estimatedHoursStr.toDoubleOrNull() ?: 0.0
                                onSave(selectedProjectId, title, description, priority, est, deadlineTimestamp, reminderLeadHours)
                            }
                        },
                        enabled = title.isNotBlank() && selectedProjectId > 0,
                        modifier = Modifier.weight(1f).testTag("save_task_button")
                    ) {
                        Text(t.saveTask)
                    }
                }
            }
        }
    }

    if (showDeadlinePicker) {
        DateTimePickerDialog(
            initialMillis = deadlineTimestamp,
            title = t.deadlineTime,
            confirmLabel = t.setDeadline,
            onDismiss = { showDeadlinePicker = false },
            onConfirm = { millis ->
                deadlineTimestamp = millis
                showDeadlinePicker = false
            }
        )
    }
}

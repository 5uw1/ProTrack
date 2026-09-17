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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.suw1labs.worktracker.ui.components.ConfirmDeleteDialog
import com.suw1labs.worktracker.ui.components.DateTimePickerDialog
import com.suw1labs.worktracker.ui.components.FormDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.DeadlineUrgencyBadge
import com.suw1labs.worktracker.ui.components.EmptyStateCard
import com.suw1labs.worktracker.ui.components.displayLabel
import com.suw1labs.worktracker.ui.components.PriorityBadge
import com.suw1labs.worktracker.ui.theme.AmberWarning
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.formatFixed
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.projectColor
import com.suw1labs.worktracker.ui.i18n.strings

@Composable
fun TasksScreen(
    viewModel: TrackerViewModel,
    onTrackTask: (WorkTaskWithProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = strings
    val allTasks by viewModel.allTasks.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    var statusFilter by remember { mutableStateOf("ALL") }
    var addTaskProjectId by remember { mutableStateOf<Long?>(null) }
    var editingTask by remember { mutableStateOf<WorkTaskWithProject?>(null) }
    var movingTask by remember { mutableStateOf<WorkTaskWithProject?>(null) }
    var deletingTask by remember { mutableStateOf<WorkTaskWithProject?>(null) }

    val filteredTasks = remember(allTasks, statusFilter) {
        when (statusFilter) {
            "TODO" -> allTasks.filter { it.status == "TODO" }
            "IN_PROGRESS" -> allTasks.filter { it.status == "IN_PROGRESS" }
            "DONE" -> allTasks.filter { it.status == "DONE" }
            else -> allTasks
        }
    }
    val tasksByProject = remember(filteredTasks) { filteredTasks.groupBy { it.projectId } }
    // Productive projects first, then the unproductive one(s); completed projects last.
    val orderedProjects = remember(allProjects) {
        allProjects.sortedWith(compareBy({ !it.isProductive }, { it.status == "COMPLETED" }, { it.code }))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

        if (orderedProjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Task,
                    title = t.noProjectsTitle,
                    subtitle = t.noProjectsSubtitle
                )
            }
        }

        items(orderedProjects, key = { it.id }) { project ->
            ProjectTasksCard(
                project = project,
                tasks = tasksByProject[project.id].orEmpty(),
                onAddTask = { addTaskProjectId = project.id },
                onToggleDone = { task ->
                    viewModel.updateTaskStatus(task.id, if (task.status == "DONE") "TODO" else "DONE")
                },
                onTrack = onTrackTask,
                onMove = { movingTask = it },
                onEdit = { editingTask = it },
                onDelete = { deletingTask = it }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    deletingTask?.let { task ->
        ConfirmDeleteDialog(
            title = t.deleteTaskQuestion(task.title),
            message = t.deleteTaskWarning,
            onConfirm = {
                viewModel.deleteTask(task.id)
                deletingTask = null
            },
            onDismiss = { deletingTask = null }
        )
    }

    addTaskProjectId?.let { projectId ->
        TaskFormDialog(
            task = null,
            projects = allProjects,
            initialProjectId = projectId,
            onDismiss = { addTaskProjectId = null },
            onSave = { pId, title, desc, priority, estHours, deadline, leadHours ->
                viewModel.addTask(pId, title, desc, priority, estHours, deadline, leadHours)
                addTaskProjectId = null
            }
        )
    }

    editingTask?.let { task ->
        TaskFormDialog(
            task = task,
            projects = allProjects,
            onDismiss = { editingTask = null },
            onSave = { pId, title, desc, priority, estHours, deadline, leadHours ->
                if (pId != task.projectId) viewModel.moveTask(task.id, pId)
                viewModel.updateTask(
                    WorkTask(
                        id = task.id,
                        projectId = pId,
                        title = title,
                        description = desc,
                        priority = priority,
                        status = task.status,
                        estimatedHours = estHours,
                        deadlineTimestamp = deadline,
                        reminderLeadHours = leadHours,
                        reminderEnabled = deadline != null,
                        createdAt = task.createdAt
                    )
                )
                editingTask = null
            }
        )
    }

    movingTask?.let { task ->
        MoveTaskDialog(
            task = task,
            projects = allProjects.filter { it.id != task.projectId },
            onDismiss = { movingTask = null },
            onMove = { projectId ->
                viewModel.moveTask(task.id, projectId)
                movingTask = null
            }
        )
    }
}

/** One project with its tasks. */
@Composable
private fun ProjectTasksCard(
    project: Project,
    tasks: List<WorkTaskWithProject>,
    onAddTask: () -> Unit,
    onToggleDone: (WorkTaskWithProject) -> Unit,
    onTrack: (WorkTaskWithProject) -> Unit,
    onMove: (WorkTaskWithProject) -> Unit,
    onEdit: (WorkTaskWithProject) -> Unit,
    onDelete: (WorkTaskWithProject) -> Unit
) {
    val t = strings
    val projColor = projectColor(project.colorHex)
    val sorted = remember(tasks) { tasks.sortedWith(compareBy({ it.status == "DONE" }, { it.deadlineTimestamp ?: Long.MAX_VALUE })) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("project_tasks_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(30.dp).clip(RoundedCornerShape(2.dp)).background(projColor))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (project.isProductive) "${project.code} · ${project.name}" else project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (project.isProductive) MaterialTheme.colorScheme.onSurface else AmberWarning
                    )
                    Text(
                        text = (if (project.client.isNotBlank()) "${project.client} · " else "") + t.tasksCount(tasks.size),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onAddTask, modifier = Modifier.testTag("add_task_${project.id}")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t.add, fontSize = 12.sp)
                }
            }

            if (sorted.isEmpty()) {
                Text(t.noTasksInProject, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 14.dp, top = 6.dp))
            }
            sorted.forEachIndexed { index, task ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                TaskRow(
                    task = task,
                    onToggleDone = { onToggleDone(task) },
                    onTrack = { onTrack(task) },
                    onMove = { onMove(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { onDelete(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: WorkTaskWithProject,
    onToggleDone: () -> Unit,
    onTrack: () -> Unit,
    onMove: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val t = strings
    val isDone = task.status == "DONE"
    val loggedHours = task.loggedSeconds / 3600.0

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("task_card_${task.id}"), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleDone, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = t.toggleComplete,
                tint = if (isDone) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.priority != "LOW") PriorityBadge(priority = task.priority)
                if (task.deadlineTimestamp != null && !isDone) DeadlineUrgencyBadge(deadlineTimestamp = task.deadlineTimestamp)
                if (task.loggedSeconds > 0 || task.estimatedHours > 0) {
                    Text(
                        text = t.bookedPlanned(loggedHours.formatFixed(1), TimeFormat.sapHours(task.estimatedHours)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (!isDone) {
            IconButton(onClick = onTrack, modifier = Modifier.size(36.dp).testTag("track_task_${task.id}")) {
                Icon(Icons.Default.PlayArrow, contentDescription = t.track, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        // Rare actions live in an overflow menu so the row stays readable on a phone.
        Box {
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp).testTag("task_menu_${task.id}")) {
                Icon(Icons.Default.MoreVert, contentDescription = t.moreActions, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(t.edit) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { menuOpen = false; onEdit() },
                    modifier = Modifier.testTag("edit_task_${task.id}")
                )
                DropdownMenuItem(
                    text = { Text(t.moveTask) },
                    leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                    onClick = { menuOpen = false; onMove() },
                    modifier = Modifier.testTag("move_task_${task.id}")
                )
                DropdownMenuItem(
                    text = { Text(t.delete, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() },
                    modifier = Modifier.testTag("delete_task_${task.id}")
                )
            }
        }
    }
}

/** Pick the project a task (and its logged time) moves to. */
@Composable
private fun MoveTaskDialog(
    task: WorkTaskWithProject,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit
) {
    val t = strings
    var targetId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    FormDialog(
        title = t.moveTask,
        subtitle = task.title,
        onDismiss = onDismiss,
        onSave = { targetId?.let(onMove) },
        saveEnabled = targetId != null,
        saveTestTag = "confirm_move_button",
        modifier = Modifier.testTag("move_task_dialog")
    ) {
        LabeledDropdown(
            label = t.moveTaskTo,
            selectedText = projects.find { it.id == targetId }?.displayLabel() ?: t.selectProject,
            options = projects,
            optionText = { it.displayLabel() },
            onSelect = { targetId = it.id },
            testTag = "move_target_dropdown"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    task: WorkTaskWithProject?,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Double, Long?, Int) -> Unit,
    initialProjectId: Long? = null
) {
    val t = strings
    var selectedProjectId by remember { mutableStateOf(task?.projectId ?: initialProjectId ?: (projects.firstOrNull()?.id ?: 0L)) }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var estimatedHoursStr by remember { mutableStateOf(TimeFormat.sapHours(task?.estimatedHours ?: 4.0)) }
    var deadlineTimestamp by remember { mutableStateOf<Long?>(task?.deadlineTimestamp) }
    var reminderLeadHours by remember { mutableStateOf(task?.reminderLeadHours ?: 24) }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && selectedProjectId > 0
    FormDialog(
        title = if (task == null) t.newTask else t.editTask,
        onDismiss = onDismiss,
        onSave = {
            val est = estimatedHoursStr.replace(',', '.').toDoubleOrNull() ?: 0.0
            onSave(selectedProjectId, title.trim(), description.trim(), priority, est, deadlineTimestamp, reminderLeadHours)
        },
        saveEnabled = canSave,
        saveLabel = t.saveTask,
        saveTestTag = "save_task_button",
        modifier = Modifier.testTag("task_form_dialog")
    ) {
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
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("task_title_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(t.descriptionNotes) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Deadline
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
                color = if (deadlineTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (deadlineTimestamp != null) {
                IconButton(onClick = { deadlineTimestamp = null }, modifier = Modifier.size(36.dp).testTag("clear_deadline_button")) {
                    Icon(Icons.Default.Close, contentDescription = t.noDeadline, modifier = Modifier.size(18.dp))
                }
            }
            OutlinedButton(
                onClick = { showDeadlinePicker = true },
                modifier = Modifier.testTag("set_deadline_button")
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(t.setDeadline, fontSize = 12.sp)
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

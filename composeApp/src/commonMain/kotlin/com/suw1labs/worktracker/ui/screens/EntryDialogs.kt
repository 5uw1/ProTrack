package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkCategory
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.components.CategoryDropdown
import com.suw1labs.worktracker.ui.components.ColorPaletteSelector
import com.suw1labs.worktracker.ui.components.DateTimePickerDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.ProjectDropdown
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.ui.i18n.strings

/** Button that shows a date/time and opens the picker. */
@Composable
private fun DateTimeField(
    label: String,
    millis: Long?,
    placeholder: String,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(millis?.let { DateFormats.dateTime(it) } ?: placeholder, fontSize = 13.sp)
        }
    }
    if (showPicker) {
        DateTimePickerDialog(
            initialMillis = millis,
            title = label,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPick(it)
                showPicker = false
            }
        )
    }
}

/**
 * Add or edit an activity entry. For a running entry the end time stays open.
 */
@Composable
fun EntryFormDialog(
    entry: TimeEntryWithDetails?,
    projects: List<Project>,
    categories: List<WorkCategory>,
    tasks: List<WorkTaskWithProject>,
    onDismiss: () -> Unit,
    onSave: (projectId: Long?, categoryId: Long?, taskId: Long?, description: String, start: Long, end: Long?) -> Unit,
    initialDayStart: Long? = null
) {
    val t = strings
    val isRunning = entry?.isRunning == true
    val defaultEnd = initialDayStart?.let { it + 17 * 3600_000L } ?: currentTimeMillis()
    var projectId by remember { mutableStateOf(entry?.projectId) }
    var categoryId by remember { mutableStateOf(entry?.categoryId ?: categories.firstOrNull()?.id) }
    var taskId by remember { mutableStateOf(entry?.taskId) }
    var description by remember { mutableStateOf(entry?.description ?: "") }
    var start by remember { mutableStateOf(entry?.startTime ?: (defaultEnd - 3600_000L)) }
    var end by remember { mutableStateOf<Long?>(if (isRunning) null else (entry?.endTime ?: defaultEnd)) }

    val projectTasks = remember(projectId, tasks) { tasks.filter { it.projectId == projectId && it.status != "DONE" } }
    val endValue = end
    val invalidRange = !isRunning && endValue != null && endValue <= start

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("entry_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (entry == null) t.logActivity else t.editActivity,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                ProjectDropdown(projects = projects, selectedProjectId = projectId, onSelect = {
                    projectId = it
                    taskId = null
                }, testTag = "entry_project_dropdown")
                Spacer(modifier = Modifier.height(10.dp))

                CategoryDropdown(categories = categories, selectedCategoryId = categoryId, onSelect = { categoryId = it })

                if (projectTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val options: List<WorkTaskWithProject?> = listOf<WorkTaskWithProject?>(null) + projectTasks
                    LabeledDropdown(
                        label = t.taskOptional,
                        selectedText = projectTasks.find { it.id == taskId }?.title ?: t.noSpecificTask,
                        options = options,
                        optionText = { it?.title ?: t.noSpecificTask },
                        onSelect = { selected ->
                            taskId = selected?.id
                            if (selected != null && description.isBlank()) description = selected.title
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(t.whatDidYouDo) },
                    modifier = Modifier.fillMaxWidth().testTag("entry_description_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                DateTimeField(label = t.startLabel, millis = start, placeholder = t.pickStart, onPick = { start = it })
                if (!isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DateTimeField(label = t.endLabel, millis = end, placeholder = t.pickEnd, onPick = { end = it })
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(t.stillRunning, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (invalidRange) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(t.endAfterStart, fontSize = 12.sp, color = RoseUrgent)
                } else if (!isRunning && endValue != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        t.duration(TimeFormat.hoursMinutes((endValue - start) / 1000)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { onSave(projectId, categoryId, taskId, description, start, end) },
                        enabled = !invalidRange && (isRunning || end != null),
                        modifier = Modifier.weight(1f).testTag("save_entry_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

/**
 * Add or edit an attendance (clock-in) session.
 */
@Composable
fun SessionFormDialog(
    session: AttendanceSession?,
    onDismiss: () -> Unit,
    onSave: (clockIn: Long, clockOut: Long?, reason: ClockOutReason?) -> Unit,
    initialDayStart: Long? = null
) {
    val t = strings
    val isOpen = session?.isOpen == true
    val defaultIn = initialDayStart?.let { it + 8 * 3600_000L } ?: (currentTimeMillis() - 8 * 3600_000L)
    val defaultOut = initialDayStart?.let { it + 17 * 3600_000L } ?: currentTimeMillis()
    var clockIn by remember { mutableStateOf(session?.clockIn ?: defaultIn) }
    var clockOut by remember { mutableStateOf<Long?>(if (isOpen) null else (session?.clockOut ?: defaultOut)) }
    var reason by remember { mutableStateOf(ClockOutReason.fromName(session?.clockOutReason) ?: ClockOutReason.END_OF_DAY) }

    val outValue = clockOut
    val invalidRange = !isOpen && outValue != null && outValue <= clockIn

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("session_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (session == null) t.addPeriod else t.editPeriod,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                DateTimeField(label = t.clockInLabel, millis = clockIn, placeholder = t.pickTime, onPick = { clockIn = it })
                Spacer(modifier = Modifier.height(10.dp))
                if (isOpen) {
                    Text(t.stillClockedIn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    DateTimeField(label = t.clockOutLabel, millis = clockOut, placeholder = t.pickTime, onPick = { clockOut = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    LabeledDropdown(
                        label = t.clockOutReason,
                        selectedText = t.reasonLabel(reason),
                        options = ClockOutReason.entries,
                        optionText = { t.reasonLabel(it) },
                        onSelect = { reason = it }
                    )
                }
                if (invalidRange) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(t.clockOutAfterIn, fontSize = 12.sp, color = RoseUrgent)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { onSave(clockIn, clockOut, if (isOpen) null else reason) },
                        enabled = !invalidRange && (isOpen || clockOut != null),
                        modifier = Modifier.weight(1f).testTag("save_session_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

/** Add or edit a work category. */
@Composable
fun CategoryFormDialog(
    category: WorkCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, isProductive: Boolean, colorHex: String) -> Unit
) {
    val t = strings
    var name by remember { mutableStateOf(category?.name ?: "") }
    var isProductive by remember { mutableStateOf(category?.isProductive ?: true) }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#3B82F6") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("category_form_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (category == null) t.newCategory else t.editCategory,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(t.categoryName) },
                    placeholder = { Text(t.categoryPlaceholder) },
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isProductive) t.productiveWork else t.unproductiveTime, fontWeight = FontWeight.Medium)
                        Text(
                            if (isProductive) t.productiveHint else t.unproductiveHint,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isProductive, onCheckedChange = { isProductive = it })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(t.colour, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ColorPaletteSelector(selectedColorHex = colorHex, onColorSelected = { colorHex = it })
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name, isProductive, colorHex) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_category_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

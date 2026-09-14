package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Project
import com.example.data.model.ProjectSummary
import com.example.ui.components.ColorPaletteSelector
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HoursProgressBar
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.TrackerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val projectSummaries by viewModel.projectSummaries.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectSummary?>(null) }

    val filteredProjects = remember(projectSummaries, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> projectSummaries.filter { it.status == "ACTIVE" }
            "COMPLETED" -> projectSummaries.filter { it.status == "COMPLETED" }
            "ON_HOLD" -> projectSummaries.filter { it.status == "ON_HOLD" }
            else -> projectSummaries
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("ALL" to "All", "ACTIVE" to "Active", "ON_HOLD" to "On Hold", "COMPLETED" to "Completed").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedFilter == code,
                            onClick = { selectedFilter = code },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_chip_$code")
                        )
                    }
                }
            }

            if (filteredProjects.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Folder,
                        title = "No projects found",
                        subtitle = "Tap the + button below to create your first work project."
                    )
                }
            } else {
                items(filteredProjects, key = { it.id }) { summary ->
                    ProjectCard(
                        summary = summary,
                        onEdit = { editingProject = summary },
                        onDelete = { viewModel.deleteProject(summary.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating Action Button to Add Project
        FloatingActionButton(
            onClick = { showAddProjectDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_project_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Project")
        }
    }

    // Add Project Dialog
    if (showAddProjectDialog) {
        ProjectFormDialog(
            project = null,
            onDismiss = { showAddProjectDialog = false },
            onSave = { name, client, colorHex, hourlyRate, budgetHours, _ ->
                viewModel.addProject(name, client, colorHex, hourlyRate, budgetHours)
                showAddProjectDialog = false
            }
        )
    }

    // Edit Project Dialog
    editingProject?.let { summary ->
        ProjectFormDialog(
            project = Project(
                id = summary.id,
                name = summary.name,
                client = summary.client,
                colorHex = summary.colorHex,
                hourlyRate = summary.hourlyRate,
                budgetHours = summary.budgetHours,
                status = summary.status
            ),
            onDismiss = { editingProject = null },
            onSave = { name, client, colorHex, hourlyRate, budgetHours, status ->
                viewModel.updateProject(
                    Project(
                        id = summary.id,
                        name = name,
                        client = client,
                        colorHex = colorHex,
                        hourlyRate = hourlyRate,
                        budgetHours = budgetHours,
                        status = status
                    )
                )
                editingProject = null
            }
        )
    }
}

@Composable
fun ProjectCard(
    summary: ProjectSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val projColor = try {
        Color(android.graphics.Color.parseColor(summary.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val loggedHours = summary.totalSeconds / 3600.0
    val billableHours = summary.billableSeconds / 3600.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${summary.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(projColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(status = summary.status)
            }

            if (summary.client.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = summary.client,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Budget Progress
            HoursProgressBar(
                loggedHours = loggedHours,
                budgetHours = summary.budgetHours,
                accentColor = projColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "BILLABLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.0f", summary.totalEarnings)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "RATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.0f", summary.hourlyRate)}/h",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text(
                        text = "TASKS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${summary.completedTasks}/${summary.totalTasks} Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var client by remember { mutableStateOf(project?.client ?: "") }
    var colorHex by remember { mutableStateOf(project?.colorHex ?: "#3B82F6") }
    var hourlyRateStr by remember { mutableStateOf(project?.hourlyRate?.toString() ?: "75.0") }
    var budgetHoursStr by remember { mutableStateOf(project?.budgetHours?.toString() ?: "30.0") }
    var status by remember { mutableStateOf(project?.status ?: "ACTIVE") }
    var isStatusMenuOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("project_form_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (project == null) "New Work Project" else "Edit Project",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client / Organization") },
                    modifier = Modifier.fillMaxWidth().testTag("project_client_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hourlyRateStr,
                        onValueChange = { hourlyRateStr = it },
                        label = { Text("Hourly Rate ($)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = budgetHoursStr,
                        onValueChange = { budgetHoursStr = it },
                        label = { Text("Budget (Hours)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status dropdown
                ExposedDropdownMenuBox(
                    expanded = isStatusMenuOpen,
                    onExpandedChange = { isStatusMenuOpen = !isStatusMenuOpen }
                ) {
                    TextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusMenuOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isStatusMenuOpen,
                        onDismissRequest = { isStatusMenuOpen = false }
                    ) {
                        listOf("ACTIVE", "ON_HOLD", "COMPLETED").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    status = s
                                    isStatusMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Project Color Tag",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ColorPaletteSelector(
                    selectedColorHex = colorHex,
                    onColorSelected = { colorHex = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val rate = hourlyRateStr.toDoubleOrNull() ?: 0.0
                                val budget = budgetHoursStr.toDoubleOrNull() ?: 0.0
                                onSave(name, client, colorHex, rate, budget, status)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_project_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

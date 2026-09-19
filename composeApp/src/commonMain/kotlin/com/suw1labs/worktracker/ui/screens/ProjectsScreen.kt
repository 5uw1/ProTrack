package com.suw1labs.worktracker.ui.screens

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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.ProjectSummary
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.suw1labs.worktracker.ui.components.ColorPaletteSelector
import com.suw1labs.worktracker.ui.components.ConfirmDeleteDialog
import com.suw1labs.worktracker.ui.components.FormDialog
import com.suw1labs.worktracker.ui.components.EmptyStateCard
import com.suw1labs.worktracker.ui.components.LocalSnackbarHostState
import com.suw1labs.worktracker.ui.components.HoursProgressBar
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.StatusBadge
import com.suw1labs.worktracker.ui.components.LocalScreenInsets
import com.suw1labs.worktracker.ui.i18n.Language
import com.suw1labs.worktracker.ui.theme.AmberWarning
import com.suw1labs.worktracker.ui.theme.EmeraldGreen
import com.suw1labs.worktracker.ui.viewmodel.BackupMessage
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.platform.ExportAction
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.projectColor
import com.suw1labs.worktracker.ui.i18n.strings

/** Settings tab: work schedule, language, backups. Projects themselves live on the Projects tab (TasksScreen). */
@Composable
fun ProjectsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val t = strings
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showSapDialog by remember { mutableStateOf(false) }
    val settings by viewModel.settings.collectAsState()
    val backupBusy by viewModel.backupBusy.collectAsState()
    val pendingRestore by viewModel.pendingRestore.collectAsState()
    val pendingWorkImport by viewModel.pendingWorkImport.collectAsState()
    val autoBackupFolder by viewModel.autoBackupFolder.collectAsState()
    val autoBackupState by viewModel.autoBackupState.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()
    val snackbarHost = LocalSnackbarHostState.current

    // Report the outcome of a backup action once, then forget it.
    LaunchedEffect(backupMessage) {
        val message = backupMessage ?: return@LaunchedEffect
        val text = when (message) {
            is BackupMessage.Exported -> t.backupExported
            is BackupMessage.Restored -> "${t.backupRestored} · ${t.backupContents(message.summary)}"
            is BackupMessage.Failed -> t.backupError(message.error)
            BackupMessage.NoAutoBackup -> t.autoBackupNoFile
            BackupMessage.NotWorkExport -> t.notWorkExport
            is BackupMessage.WorkImported -> t.workImported(message.plan.sessions.size, message.plan.entries.size)
        }
        viewModel.clearBackupMessage()
        snackbarHost.showSnackbar(text)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = LocalScreenInsets.current,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // --- Work schedule ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("work_schedule_card")
                ) {
                    Row(modifier = Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.workSchedule, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${TimeFormat.sapHours(settings.workloadPercent)} % · ${TimeFormat.sapHours(settings.weeklyTargetHours)} ${t.perWeek} · " +
                                    settings.weekdayHoursList.mapIndexedNotNull { i, h -> if (h > 0) "${t.weekdaysTwo[i]} ${TimeFormat.sapHours(h)}" else null }.joinToString(" ") +
                                    " · ${t.maxPerWeek} ${TimeFormat.sapHours(settings.maxWeeklyHours)} ${t.perWeek}" +
                                    (if (settings.paidBreakMinutes > 0) " · ${t.paidBreakSummary(settings.paidBreakMinutes.toString())}" else "") +
                                    settings.breakRuleList.sortedBy { it.afterSeconds }.joinToString("") { " · ${t.breakRuleSummary(TimeFormat.sapHours(it.afterSeconds / 3600.0), (it.breakSeconds / 60).toString())}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showScheduleDialog = true }, modifier = Modifier.testTag("edit_schedule_button")) {
                            Icon(Icons.Default.Edit, contentDescription = t.editSchedule)
                        }
                    }
                }
            }

            // --- SAP booking (activity types for the weekly paste export) ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("sap_settings_card")
                ) {
                    Row(modifier = Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.sapSettingsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${settings.sapProductiveType} · ${settings.sapUnproductiveType} ${settings.sapUnproductiveNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showSapDialog = true }, modifier = Modifier.testTag("edit_sap_button")) {
                            Icon(Icons.Default.Edit, contentDescription = t.edit)
                        }
                    }
                }
            }

            // --- Language ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("language_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(t.languageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Exactly one language is active: a segmented button group, not filter chips.
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            Language.entries.forEachIndexed { index, lang ->
                                SegmentedButton(
                                    selected = settings.language == lang.code,
                                    onClick = { viewModel.setLanguage(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Language.entries.size),
                                    label = { Text(lang.displayName) },
                                    modifier = Modifier.testTag("language_${lang.code}")
                                )
                            }
                        }
                    }
                }
            }

            // --- Backup & transfer ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("backup_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.backupTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(t.backupSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { viewModel.exportBackup(ExportAction.SAVE) },
                                enabled = !backupBusy,
                                modifier = Modifier.weight(1f).testTag("backup_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.saveFile, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportBackup(ExportAction.SHARE) },
                                enabled = !backupBusy,
                                modifier = Modifier.weight(1f).testTag("backup_share_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t.backupShare, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.pickBackupToRestore() },
                            enabled = !backupBusy,
                            modifier = Modifier.fillMaxWidth().testTag("backup_restore_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t.backupRestore, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- Import from other apps (WORK for iOS) ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("import_apps_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.importAppsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(t.importAppsSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.pickWorkExport() },
                            enabled = !backupBusy,
                            modifier = Modifier.fillMaxWidth().testTag("import_work_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t.importWorkButton, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- Automatic backup into a chosen folder (Drive, iCloud, local) ---
            if (viewModel.autoBackupAvailable) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().testTag("auto_backup_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.autoBackupTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(t.autoBackupSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val folder = autoBackupFolder
                            if (folder == null) {
                                Text(t.autoBackupOff, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("auto_backup_status"))
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.chooseAutoBackupFolder() },
                                    enabled = !backupBusy,
                                    modifier = Modifier.fillMaxWidth().testTag("auto_backup_choose_button")
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(t.chooseFolder, fontSize = 12.sp)
                                }
                            } else {
                                Text(t.autoBackupFolder(folder.displayName), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("auto_backup_status"))
                                val status = when {
                                    autoBackupState.lastError -> t.autoBackupFailed
                                    autoBackupState.lastWrittenAt != null -> t.autoBackupLast(DateFormats.dateTime(autoBackupState.lastWrittenAt!!))
                                    else -> t.autoBackupPending
                                }
                                Text(
                                    status,
                                    fontSize = 12.sp,
                                    color = if (autoBackupState.lastError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { viewModel.runAutoBackupNow() }, enabled = !backupBusy && !autoBackupState.running, modifier = Modifier.weight(1f).testTag("auto_backup_now_button")) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(t.backupNow, fontSize = 12.sp, maxLines = 1)
                                    }
                                    OutlinedButton(onClick = { viewModel.restoreFromAutoBackup() }, enabled = !backupBusy, modifier = Modifier.weight(1f).testTag("auto_backup_restore_button")) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(t.restoreFromFolder, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.chooseAutoBackupFolder() }, enabled = !backupBusy, modifier = Modifier.weight(1f).testTag("auto_backup_change_button")) {
                                        Text(t.changeFolder, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { viewModel.disableAutoBackup() }, modifier = Modifier.weight(1f).testTag("auto_backup_off_button")) {
                                        Text(t.turnOff, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSapDialog) {
        SapSettingsDialog(
            settings = settings,
            onDismiss = { showSapDialog = false },
            onSave = { productive, unproductive, number ->
                viewModel.saveSapSettings(productive, unproductive, number)
                showSapDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        WorkScheduleDialog(
            settings = settings,
            onDismiss = { showScheduleDialog = false },
            onSave = {
                viewModel.saveSettings(it)
                showScheduleDialog = false
            }
        )
    }

    pendingWorkImport?.let { plan ->
        val skipped = plan.skippedEntries + plan.skippedSessions
        ConfirmDeleteDialog(
            title = t.workImportTitle,
            message = (if (plan.isEmpty) t.workImportNothing else t.workImportSummary(plan.sessions.size, plan.entries.size, plan.dayRecords.size, plan.newProjects.size, plan.newTasks.size)) +
                if (skipped > 0) "\n\n" + t.workImportSkipped(skipped) else "",
            confirmLabel = t.workImportConfirm,
            onConfirm = { if (plan.isEmpty) viewModel.cancelWorkImport() else viewModel.confirmWorkImport() },
            onDismiss = { viewModel.cancelWorkImport() }
        )
    }

    pendingRestore?.let { backup ->
        ConfirmDeleteDialog(
            title = t.restoreQuestion(DateFormats.dateTime(backup.exportedAt)),
            message = "${t.backupContents(backup.summary)}\n\n${t.restoreWarning}",
            confirmLabel = t.restoreConfirm,
            onConfirm = { viewModel.confirmRestore() },
            onDismiss = { viewModel.cancelRestore() }
        )
    }


}

@Composable
fun ProjectFormDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, client: String, colorHex: String, budgetHours: Double, status: String, isProductive: Boolean) -> Unit
) {
    val t = strings
    var code by remember { mutableStateOf(project?.code ?: "") }
    var isProductive by remember { mutableStateOf(project?.isProductive ?: true) }
    var name by remember { mutableStateOf(project?.name ?: "") }
    var client by remember { mutableStateOf(project?.client ?: "") }
    var colorHex by remember { mutableStateOf(project?.colorHex ?: "#3B82F6") }
    var budgetHoursStr by remember { mutableStateOf(project?.budgetHours?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var status by remember { mutableStateOf(project?.status ?: "ACTIVE") }

    val valid = code.isNotBlank() && name.isNotBlank()

    FormDialog(
        title = if (project == null) t.newProject else t.editProject,
        onDismiss = onDismiss,
        onSave = { onSave(code, name, client, colorHex, budgetHoursStr.replace(',', '.').toDoubleOrNull() ?: 0.0, status, isProductive) },
        saveEnabled = valid,
        saveTestTag = "save_project_button",
        modifier = Modifier.testTag("project_form_dialog")
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(t.projectNumber) },
            placeholder = { Text(t.projectNumberHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("project_code_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(t.projectName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("project_name_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = client,
            onValueChange = { client = it },
            label = { Text(t.customer) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("project_client_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = budgetHoursStr,
                onValueChange = { budgetHoursStr = it },
                label = { Text(t.plannedHours) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
            LabeledDropdown(
                label = t.status,
                selectedText = t.projectStatus(status),
                options = listOf("ACTIVE", "ON_HOLD", "COMPLETED"),
                optionText = { t.projectStatus(it) },
                onSelect = { status = it },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isProductive) t.productiveWork else t.unproductiveTime, fontWeight = FontWeight.Medium)
                Text(if (isProductive) t.productiveHint else t.unproductiveHint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isProductive, onCheckedChange = { isProductive = it }, modifier = Modifier.testTag("project_productive_switch"))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(t.colourTag, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        ColorPaletteSelector(selectedColorHex = colorHex, onColorSelected = { colorHex = it })
    }
}

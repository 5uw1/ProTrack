package com.suw1labs.worktracker.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.suw1labs.worktracker.data.import.ProjectImporter
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.ui.components.LabeledDropdown
import com.suw1labs.worktracker.ui.components.dismissKeyboardOnTap
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.ui.theme.RoseUrgent
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.startOfDayMillis
import com.suw1labs.worktracker.util.toLocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Calendar-day picker button (local day start in epoch millis). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DayPickerField(
    label: String,
    dayStart: Long,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = strings
    var show by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth().testTag("day_picker_button")) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(DateFormats.fullDate(dayStart), fontSize = 13.sp)
        }
    }
    if (show) {
        val initialUtc = remember(dayStart) { dayStart.toLocalDate().atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.selectedDateMillis ?: initialUtc
                    val date = Instant.fromEpochMilliseconds(selected).toLocalDateTime(TimeZone.UTC).date
                    onPick(date.startOfDayMillis())
                    show = false
                }) { Text(t.ok) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(t.cancel) } }
        ) { DatePicker(state = state) }
    }
}

/**
 * Book an absence (sick, holiday, public holiday, compensation, education, other) on a day.
 */
@Composable
fun DayRecordDialog(
    record: DayRecord?,
    initialDayStart: Long,
    defaultHours: Double,
    onDismiss: () -> Unit,
    onSave: (dayStart: Long, type: AbsenceType, hours: Double, label: String, note: String) -> Unit
) {
    val t = strings
    var dayStart by remember { mutableStateOf(record?.dayStart ?: initialDayStart) }
    var type by remember { mutableStateOf(record?.absenceType ?: AbsenceType.HOLIDAY) }
    var hoursText by remember { mutableStateOf(TimeFormat.sapHours(record?.hours ?: defaultHours)) }
    var label by remember { mutableStateOf(record?.label ?: "") }
    var note by remember { mutableStateOf(record?.note ?: "") }

    val hours = hoursText.replace(',', '.').toDoubleOrNull()
    val valid = hours != null && hours > 0 && (type != AbsenceType.OTHER || label.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("day_record_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (record == null) t.bookAbsence else t.editAbsence,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                DayPickerField(label = t.day, dayStart = dayStart, onPick = { dayStart = it })
                Spacer(modifier = Modifier.height(10.dp))
                LabeledDropdown(
                    label = t.reason,
                    selectedText = t.absenceLabel(type),
                    options = AbsenceType.entries,
                    optionText = { t.absenceLabel(it) },
                    onSelect = { type = it },
                    testTag = "absence_type_dropdown"
                )
                if (type == AbsenceType.OTHER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(t.reasonRequired) },
                        placeholder = { Text(t.reasonPlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("absence_label_input")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text(t.hoursFullDay(TimeFormat.sapHours(defaultHours))) },
                    modifier = Modifier.fillMaxWidth().testTag("absence_hours_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (type.creditsHours) t.creditsHint else t.fromOvertimeHint,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(t.noteOptional) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (hours == null || hours <= 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(t.enterHours, fontSize = 12.sp, color = RoseUrgent)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { if (valid) onSave(dayStart, type, hours ?: 0.0, label, note) },
                        enabled = valid,
                        modifier = Modifier.weight(1f).testTag("save_day_record_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

/** Contract hours, target hours per weekday and the legal weekly maximum. */
@Composable
fun WorkScheduleDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    val t = strings
    var fullTime by remember { mutableStateOf(TimeFormat.sapHours(settings.fullTimeWeeklyHours)) }
    var maxWeekly by remember { mutableStateOf(TimeFormat.sapHours(settings.maxWeeklyHours)) }
    var dayHours by remember { mutableStateOf(settings.weekdayHoursList.map { TimeFormat.sapHours(it) }) }

    val fullTimeValue = fullTime.replace(',', '.').toDoubleOrNull()
    val maxValue = maxWeekly.replace(',', '.').toDoubleOrNull()
    val dayValues = dayHours.map { it.replace(',', '.').trim().let { v -> if (v.isEmpty()) 0.0 else v.toDoubleOrNull() } }
    val valid = fullTimeValue != null && fullTimeValue > 0 && maxValue != null && maxValue > 0 &&
        dayValues.all { it != null && it >= 0 } && dayValues.any { (it ?: 0.0) > 0 }

    val preview = if (valid) {
        val s = AppSettings(
            fullTimeWeeklyHours = fullTimeValue!!,
            weekdayHours = AppSettings.weekdayHoursString(dayValues.map { it ?: 0.0 }),
            maxWeeklyHours = maxValue!!
        )
        t.schedulePreview(TimeFormat.sapHours(s.weeklyTargetHours), TimeFormat.sapHours(s.workloadPercent), s.workDaysPerWeek)
    } else t.checkValues

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("work_schedule_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(t.workSchedule, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(t.scheduleSubtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = fullTime, onValueChange = { fullTime = it }, singleLine = true, label = { Text(t.hoursPerWeekAt100) }, modifier = Modifier.fillMaxWidth().testTag("weekly_hours_input"))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = maxWeekly, onValueChange = { maxWeekly = it }, singleLine = true, label = { Text(t.legalMaxPerWeek) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Text(t.hoursPerDay, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(t.hoursPerDayHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    (0 until 7).forEach { index ->
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t.weekdaysTwo[index], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CompactNumberField(
                                value = dayHours[index],
                                onValueChange = { v -> dayHours = dayHours.toMutableList().also { it[index] = v } },
                                modifier = Modifier.fillMaxWidth().testTag("day_hours_${index + 1}")
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(preview, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (valid) MaterialTheme.colorScheme.primary else RoseUrgent)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = {
                            if (valid) onSave(
                                settings.copy(
                                    fullTimeWeeklyHours = fullTimeValue!!,
                                    weekdayHours = AppSettings.weekdayHoursString(dayValues.map { it ?: 0.0 }),
                                    maxWeeklyHours = maxValue!!
                                )
                            )
                        },
                        enabled = valid,
                        modifier = Modifier.weight(1f).testTag("save_schedule_button")
                    ) { Text(t.save) }
                }
            }
        }
    }
}

/** Small bordered number field that shows short values (e.g. "8" or "4.5") without clipping. */
@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        modifier = modifier,
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp)
            ) { inner() }
        }
    )
}

/** Paste a project list (from Excel, SAP or a CSV file) and import the new project numbers. */
@Composable
fun ImportProjectsDialog(
    onPreview: (String) -> List<ProjectImporter.ParsedProject>,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val t = strings
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { onPreview(text) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("import_projects_dialog").dismissKeyboardOnTap()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(t.importProjects, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(t.importDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(t.projectList) },
                    placeholder = { Text(t.importPlaceholder) },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).testTag("import_text_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (parsed.isEmpty()) t.noneRecognised else t.recognised(parsed.size, parsed.take(3).joinToString(", ") { it.code } + if (parsed.size > 3) ", …" else ""),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t.cancel) }
                    Button(
                        onClick = { onImport(text) },
                        enabled = parsed.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("import_projects_button")
                    ) { Text(t.importN(parsed.size)) }
                }
            }
        }
    }
}

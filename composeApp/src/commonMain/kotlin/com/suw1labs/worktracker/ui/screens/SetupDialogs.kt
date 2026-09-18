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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.import.ProjectImporter
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.BreakRule
import com.suw1labs.worktracker.data.model.DayRecord
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.suw1labs.worktracker.ui.components.FormDialog
import com.suw1labs.worktracker.ui.components.LabeledDropdown
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

    FormDialog(
        title = if (record == null) t.bookAbsence else t.editAbsence,
        onDismiss = onDismiss,
        onSave = { if (valid) onSave(dayStart, type, hours ?: 0.0, label, note) },
        saveEnabled = valid,
        saveTestTag = "save_day_record_button",
        modifier = Modifier.testTag("day_record_dialog")
    ) {
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("absence_label_input")
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = hoursText,
            onValueChange = { hoursText = it },
            label = { Text(t.hoursFullDay(TimeFormat.sapHours(defaultHours))) },
            singleLine = true,
            isError = hours == null || hours <= 0,
            supportingText = { Text(if (hours == null || hours <= 0) t.enterHours else if (type.creditsHours) t.creditsHint else t.fromOvertimeHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("absence_hours_input")
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(t.noteOptional) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
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
    var paidBreak by remember { mutableStateOf(settings.paidBreakMinutes.toString()) }
    val paidBreakValue = paidBreak.trim().let { if (it.isEmpty()) 0 else it.toIntOrNull() }
    // Break rules as editable text pairs (hours worked, minutes of break).
    var rules by remember { mutableStateOf(settings.breakRuleList.sortedBy { it.afterSeconds }.map { TimeFormat.sapHours(it.afterSeconds / 3600.0) to (it.breakSeconds / 60).toString() }) }
    var deductMissing by remember { mutableStateOf(settings.deductMissingBreak) }
    val ruleValues = rules.map { (h, m) -> h.replace(',', '.').trim().toDoubleOrNull() to m.trim().toIntOrNull() }
    val rulesValid = ruleValues.all { (h, m) -> h != null && h >= 0 && m != null && m > 0 }

    val fullTimeValue = fullTime.replace(',', '.').toDoubleOrNull()
    val maxValue = maxWeekly.replace(',', '.').toDoubleOrNull()
    val dayValues = dayHours.map { it.replace(',', '.').trim().let { v -> if (v.isEmpty()) 0.0 else v.toDoubleOrNull() } }
    val valid = fullTimeValue != null && fullTimeValue > 0 && maxValue != null && maxValue > 0 &&
        dayValues.all { it != null && it >= 0 } && dayValues.any { (it ?: 0.0) > 0 } &&
        paidBreakValue != null && paidBreakValue >= 0 && rulesValid

    val preview = if (valid) {
        val s = AppSettings(
            fullTimeWeeklyHours = fullTimeValue!!,
            weekdayHours = AppSettings.weekdayHoursString(dayValues.map { it ?: 0.0 }),
            maxWeeklyHours = maxValue!!
        )
        t.schedulePreview(TimeFormat.sapHours(s.weeklyTargetHours), TimeFormat.sapHours(s.workloadPercent), s.workDaysPerWeek)
    } else t.checkValues

    FormDialog(
        title = t.workSchedule,
        subtitle = t.scheduleSubtitle,
        onDismiss = onDismiss,
        onSave = {
            if (valid) onSave(
                settings.copy(
                    fullTimeWeeklyHours = fullTimeValue!!,
                    weekdayHours = AppSettings.weekdayHoursString(dayValues.map { it ?: 0.0 }),
                    maxWeeklyHours = maxValue!!,
                    paidBreakMinutes = paidBreakValue ?: 0,
                    breakRules = AppSettings.breakRulesString(ruleValues.map { (h, m) -> BreakRule((h!! * 3600).toLong(), m!! * 60L) }),
                    deductMissingBreak = deductMissing
                )
            )
        },
        saveEnabled = valid,
        saveTestTag = "save_schedule_button",
        modifier = Modifier.testTag("work_schedule_dialog")
    ) {
        val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        OutlinedTextField(value = fullTime, onValueChange = { fullTime = it }, singleLine = true, keyboardOptions = decimal, label = { Text(t.hoursPerWeekAt100) }, modifier = Modifier.fillMaxWidth().testTag("weekly_hours_input"))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = maxWeekly, onValueChange = { maxWeekly = it }, singleLine = true, keyboardOptions = decimal, label = { Text(t.legalMaxPerWeek) }, modifier = Modifier.fillMaxWidth())
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
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = paidBreak,
            onValueChange = { paidBreak = it },
            singleLine = true,
            isError = paidBreakValue == null,
            label = { Text(t.paidBreakPerDay) },
            supportingText = { Text(t.paidBreakHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("paid_break_input")
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(t.breakRulesTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(t.breakRulesHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        rules.forEachIndexed { index, (hours, minutes) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { v -> rules = rules.toMutableList().also { it[index] = v to it[index].second } },
                    singleLine = true,
                    label = { Text(t.ruleAfterHours) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).testTag("break_rule_hours_$index")
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { v -> rules = rules.toMutableList().also { it[index] = it[index].first to v } },
                    singleLine = true,
                    label = { Text(t.ruleBreakMinutes) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f).testTag("break_rule_minutes_$index")
                )
                IconButton(onClick = { rules = rules.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(32.dp).testTag("break_rule_remove_$index")) {
                    Icon(Icons.Default.Close, contentDescription = t.delete, modifier = Modifier.size(18.dp))
                }
            }
        }
        TextButton(onClick = { rules = rules + ("" to "") }, modifier = Modifier.testTag("break_rule_add")) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(t.addRule, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.deductMissingBreak, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(t.deductMissingBreakHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = deductMissing, onCheckedChange = { deductMissing = it }, modifier = Modifier.testTag("deduct_missing_break_switch"))
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

    FormDialog(
        title = t.importProjects,
        subtitle = t.importDescription,
        onDismiss = onDismiss,
        onSave = { onImport(text) },
        saveEnabled = parsed.isNotEmpty(),
        saveLabel = t.importN(parsed.size),
        saveTestTag = "import_projects_button",
        modifier = Modifier.testTag("import_projects_dialog")
    ) {
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
            color = if (parsed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
    }
}

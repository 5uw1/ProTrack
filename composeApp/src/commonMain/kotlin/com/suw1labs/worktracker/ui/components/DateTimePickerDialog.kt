package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.toEpochMillis
import com.suw1labs.worktracker.util.toLocalDateTime
import com.suw1labs.worktracker.ui.i18n.strings
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Two-step date & time picker (calendar date, then 24h time) built from Material 3 pickers so it
 * works identically on Android, iOS and desktop.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateTimePickerDialog(
    initialMillis: Long?,
    title: String,
    confirmLabel: String = "OK",
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val t = strings
    val initialLocal = remember { (initialMillis ?: currentTimeMillis()).toLocalDateTime() }

    // Material's DatePicker works with UTC-midnight epoch millis for the selected calendar day.
    val initialUtcDayMillis = remember {
        initialLocal.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtcDayMillis)
    val timeState = rememberTimePickerState(
        initialHour = initialLocal.hour,
        initialMinute = initialLocal.minute,
        is24Hour = true
    )

    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (pickedDate == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = dateState.selectedDateMillis ?: initialUtcDayMillis
                        pickedDate = Instant.fromEpochMilliseconds(selected)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                    }
                ) { Text(t.next) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(t.cancel) }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pickedDate ?: return@TextButton
                        val dateTime = LocalDateTime(date, LocalTime(timeState.hour, timeState.minute))
                        onConfirm(dateTime.toEpochMillis())
                    }
                ) { Text(confirmLabel) }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) { Text(t.back) }
            }
        )
    }
}

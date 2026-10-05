package com.suw1labs.worktracker.ui.timesheet

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.ui.i18n.strings
import com.suw1labs.worktracker.util.projectColor

private val FieldShape = RoundedCornerShape(8.dp)

/**
 * A small text field for a time of day, made for the keyboard: the text is selected on focus so a
 * new time simply overwrites it, Enter or leaving the field saves, Esc puts the old value back,
 * Alt+↑/↓ moves it by 15 minutes and plain ↑/↓ go to the field above or below (as in a spreadsheet).
 *
 * [onCommit] gets the typed minutes after midnight and returns false when the time does not fit
 * (end before start); the field then shows the error until a valid time is typed or it is left.
 */
@Composable
fun TimeField(
    minutes: Int?,
    onCommit: (Int) -> Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    testTag: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val shown = minutes?.let { ClockTime.format(it) } ?: placeholder
    var value by remember { mutableStateOf(TextFieldValue(shown)) }
    var focused by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // A saved change from elsewhere (drag, the other field) shows up unless the user is typing here.
    LaunchedEffect(shown) { if (!focused) value = TextFieldValue(shown) }

    fun commit(text: String): Boolean {
        if (text == shown) { error = false; return true }
        val parsed = ClockTime.parse(text)
        val ok = parsed != null && onCommit(parsed)
        error = !ok
        if (ok) value = TextFieldValue(ClockTime.format(parsed), TextRange(0, 5))
        return ok
    }

    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = { value = it; error = false },
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) colors.onSurface else colors.onSurfaceVariant),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit(value.text) }),
        modifier = modifier
            .width(64.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { state ->
                if (state.isFocused && !focused) value = value.copy(selection = TextRange(0, value.text.length))
                if (!state.isFocused && focused && !commit(value.text)) {
                    // Leaving a field with a time that does not fit puts the saved one back.
                    value = TextFieldValue(shown); error = false
                }
                focused = state.isFocused
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Enter || event.key == Key.NumPadEnter -> { commit(value.text); true }
                    event.key == Key.Escape -> {
                        val changed = value.text != shown
                        value = TextFieldValue(shown, TextRange(0, shown.length)); error = false
                        changed
                    }
                    event.isAltPressed && (event.key == Key.DirectionUp || event.key == Key.DirectionDown) -> {
                        val base = ClockTime.parse(value.text) ?: minutes
                        if (base != null) {
                            val step = if (event.key == Key.DirectionUp) -TimesheetEdits.NUDGE_MINUTES else TimesheetEdits.NUDGE_MINUTES
                            commit(ClockTime.format((base + step).coerceIn(0, ClockTime.DAY_MINUTES)))
                        }
                        true
                    }
                    !event.isShiftPressed && event.key == Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                    !event.isShiftPressed && event.key == Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                    else -> false
                }
            }
            .let { if (testTag != null) it.testTag(testTag) else it },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .clip(FieldShape)
                    .border(
                        width = if (focused || error) 2.dp else 1.dp,
                        color = when {
                            error -> colors.error
                            focused -> colors.primary
                            else -> colors.outlineVariant
                        },
                        shape = FieldShape
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) { inner() }
        }
    )
}

/** A one-line note field: Enter or leaving it saves, Esc puts the saved text back. */
@Composable
fun NoteField(
    text: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    testTag: String? = null,
) {
    var value by remember { mutableStateOf(text) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(text) { if (!focused) value = text }
    val colors = MaterialTheme.colorScheme
    fun commit() { if (value.trim() != text) onCommit(value.trim()) }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier
            .onFocusChanged { state ->
                if (!state.isFocused && focused) commit()
                focused = state.isFocused
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> { commit(); true }
                    Key.Escape -> { value = text; true }
                    else -> false
                }
            }
            .let { if (testTag != null) it.testTag(testTag) else it },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .clip(FieldShape)
                    .border(if (focused) 2.dp else 1.dp, if (focused) colors.primary else colors.outlineVariant, FieldShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 1)
                }
                inner()
            }
        }
    )
}

/** A dense pick-one button for table rows: Tab reaches it, Enter or a click opens the list. */
@Composable
fun <T> CompactPicker(
    text: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingColor: Color? = null,
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .border(1.dp, colors.outlineVariant, FieldShape)
                .clickable { expanded = true }
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                .let { if (testTag != null) it.testTag(testTag) else it }
        ) {
            if (leadingColor != null) {
                Box(Modifier.size(10.dp).clip(CircleShape).border(5.dp, leadingColor, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.6f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionText(option)) }, onClick = { expanded = false; onSelect(option) })
            }
        }
    }
}

/** Projects to offer for an entry: the active ones, plus the entry's own if it is no longer active. */
fun projectChoices(active: List<Project>, all: List<Project>, currentId: Long?): List<Project> {
    val current = currentId?.let { id -> all.firstOrNull { it.id == id } }
    return if (current == null || active.any { it.id == current.id }) active else active + current
}

/** Open tasks of [projectId], plus the entry's own task if it is already done. */
fun taskChoices(tasks: List<WorkTaskWithProject>, projectId: Long?, currentTaskId: Long?): List<WorkTaskWithProject> =
    tasks.filter { it.projectId == projectId && (it.status != "DONE" || it.id == currentTaskId) }

@Composable
fun ProjectPicker(projects: List<Project>, selectedId: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier, testTag: String? = null) {
    val selected = projects.firstOrNull { it.id == selectedId }
    val none = strings.noProjectOption
    CompactPicker(
        text = selected?.let { "${it.code} · ${it.name}" } ?: none,
        options = listOf<Project?>(null) + projects,
        optionText = { it?.let { p -> "${p.code} · ${p.name}" } ?: none },
        onSelect = { onSelect(it?.id) },
        leadingColor = selected?.let { projectColor(it.colorHex) },
        modifier = modifier,
        testTag = testTag
    )
}

@Composable
fun TaskPicker(tasks: List<WorkTaskWithProject>, selectedId: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, testTag: String? = null) {
    val general = strings.noSpecificTask
    if (!enabled) {
        Spacer(modifier.height(1.dp))
        return
    }
    CompactPicker(
        text = tasks.firstOrNull { it.id == selectedId }?.title ?: general,
        options = listOf<WorkTaskWithProject?>(null) + tasks,
        optionText = { it?.title ?: general },
        onSelect = { onSelect(it?.id) },
        modifier = modifier,
        testTag = testTag
    )
}

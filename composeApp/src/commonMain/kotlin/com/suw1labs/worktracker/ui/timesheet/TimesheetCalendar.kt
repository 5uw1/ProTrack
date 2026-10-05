package com.suw1labs.worktracker.ui.timesheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.projectColor
import kotlin.math.abs

private val HourHeight = 52.dp
private val GutterWidth = 52.dp
/** Width of the clock-in strip at the left of each day; activities are drawn to its right. */
private val AttendanceStrip = 8.dp
private val BlockShape = RoundedCornerShape(6.dp)

private enum class DragMode { MOVE, RESIZE_START, RESIZE_END, CREATE }

/** What a drag shows before it is saved: the activity (or a new one) at its new place. */
private data class DragPreview(val entryId: Long?, val day: Int, val startMin: Int, val endMin: Int)

/**
 * The week as a calendar: a column per day, an hour grid, every activity a block.
 *
 * Mouse: click selects, dragging a block moves it (also to another day), dragging its top or bottom
 * edge changes start or end, dragging on an empty spot logs a new activity, a double click logs one
 * hour. Keyboard (once the calendar has focus): arrows select, Alt+↑/↓ moves by 15 minutes,
 * Shift+↑/↓ changes the end, Alt+←/→ moves to another day, N logs the next activity, Delete deletes,
 * Enter jumps to the start field of the inspector, Esc clears the selection.
 */
@Composable
fun WeekCalendar(
    days: List<TimesheetDay>,
    /** The days with every edit so far, also ones made since the last recomposition (key repeat). */
    latestDays: () -> List<TimesheetDay>,
    now: Long,
    selection: TimesheetSelection?,
    onSelect: (TimesheetSelection?) -> Unit,
    actions: TimesheetActions,
    onEditSelected: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    var preview by remember { mutableStateOf<DragPreview?>(null) }
    val allEntries = remember(days) { days.flatMap { it.entries } }
    val currentLatestDays by rememberUpdatedState(latestDays)
    val currentSelection by rememberUpdatedState(selection)
    val currentNow by rememberUpdatedState(now)

    // Start the week scrolled to the first activity (07:00 on an empty week), not to midnight.
    val firstMinute = days.flatMap { d -> d.entries.map { ClockTime.minutesOf(it.startTime, d.range.start) } + d.sessions.map { ClockTime.minutesOf(it.clockIn, d.range.start) } }.minOrNull() ?: (7 * 60)
    val hourPx = with(androidx.compose.ui.platform.LocalDensity.current) { HourHeight.toPx() }
    LaunchedEffect(days.firstOrNull()?.range?.start) {
        scroll.scrollTo(((firstMinute - 30).coerceAtLeast(0) / 60f * hourPx).toInt())
    }
    // Keep a block chosen with the keyboard in view.
    LaunchedEffect(selection) {
        val entry = (selection as? TimesheetSelection.Entry)?.let { sel -> allEntries.firstOrNull { it.id == sel.id } } ?: return@LaunchedEffect
        val day = days.firstOrNull { it.range.contains(entry.startTime) } ?: return@LaunchedEffect
        val top = ClockTime.minutesOf(entry.startTime, day.range.start) / 60f * hourPx
        val bottom = ClockTime.minutesOf(entry.endTime ?: now, day.range.start) / 60f * hourPx
        val margin = hourPx / 2
        if (top < scroll.value + margin) scroll.animateScrollTo((top - margin).toInt().coerceAtLeast(0))
        else if (bottom > scroll.value + scroll.viewportSize - margin) scroll.animateScrollTo((bottom - scroll.viewportSize + margin).toInt().coerceAtMost(scroll.maxValue))
    }

    // The app clears focus when a list scrolls (to hide a phone keyboard); scrolling the week with the
    // mouse wheel must not cost the calendar its keyboard focus.
    var refocusAfterScroll by remember { mutableStateOf(false) }
    LaunchedEffect(scroll.isScrollInProgress) {
        if (!scroll.isScrollInProgress && refocusAfterScroll) {
            refocusAfterScroll = false
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = modifier) {
        DayHeaders(days, now)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            Row(modifier = Modifier.height(HourHeight * 24).fillMaxWidth()) {
                HourGutter()
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("timesheet_calendar")
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (!it.isFocused && scroll.isScrollInProgress) refocusAfterScroll = true }
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            handleCalendarKey(event.key, event.isAltPressed, event.isShiftPressed, event.isCtrlPressed || event.isMetaPressed, currentLatestDays(), currentSelection, currentNow, onSelect, actions, onEditSelected)
                        }
                        .focusable()
                        .drawBehind {
                            val line = colors.outlineVariant.copy(alpha = 0.5f)
                            val half = colors.outlineVariant.copy(alpha = 0.18f)
                            val hour = size.height / 24f
                            for (h in 1 until 24) {
                                drawLine(line, Offset(0f, h * hour), Offset(size.width, h * hour), strokeWidth = 1f)
                                drawLine(half, Offset(0f, (h - 0.5f) * hour), Offset(size.width, (h - 0.5f) * hour), strokeWidth = 1f)
                            }
                            val dayCount = days.size.coerceAtLeast(1)
                            val col = size.width / dayCount
                            for (d in 0..dayCount) drawLine(line, Offset(d * col, 0f), Offset(d * col, size.height), strokeWidth = 1f)
                        }
                        .pointerInput(Unit) {
                            var lastClick = 0L
                            var lastClickAt = Offset.Zero
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val dayList = currentLatestDays()
                                if (dayList.isEmpty()) return@awaitEachGesture
                                val colW = size.width / dayList.size.toFloat()
                                val pxPerMin = size.height / (24f * 60f)
                                val stripPx = AttendanceStrip.toPx()
                                fun dayAt(x: Float) = (x / colW).toInt().coerceIn(0, dayList.lastIndex)
                                fun minuteAt(y: Float) = (y / pxPerMin).toInt().coerceIn(0, ClockTime.DAY_MINUTES)

                                val day = dayAt(down.position.x)
                                val hit = hitTest(down.position, dayList[day], day, colW, pxPerMin, stripPx, currentNow, edge = 6.dp.toPx())
                                // On a touch screen a drag over empty space scrolls; only mice draw new activities.
                                if (hit == null && down.type == PointerType.Touch) return@awaitEachGesture
                                // Handled here: the app-wide "tap clears focus" must not take the keyboard away again.
                                down.consume()
                                runCatching { focusRequester.requestFocus() }

                                val entryHit = hit as? Hit.EntryHit
                                val mode = when {
                                    entryHit == null -> if (hit is Hit.SessionHit) null else DragMode.CREATE
                                    entryHit.entry.isRunning && entryHit.edge != DragMode.RESIZE_START -> null
                                    else -> entryHit.edge
                                }
                                val dayStart = dayList[day].range.start
                                val origStart = entryHit?.let { ClockTime.minutesOf(it.entry.startTime, dayStart) } ?: 0
                                val origEnd = entryHit?.let { ClockTime.minutesOf(it.entry.endTime ?: currentNow, dayStart) } ?: 0
                                val downMinute = minuteAt(down.position.y)
                                val step = TimesheetEdits.DRAG_STEP_MINUTES
                                var dragging = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) { change.consume(); break }
                                    if (mode == null) continue
                                    if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragging = true
                                    if (!dragging) continue
                                    change.consume()
                                    val minute = minuteAt(change.position.y)
                                    preview = when (mode) {
                                        DragMode.MOVE -> {
                                            val length = origEnd - origStart
                                            val start = ClockTime.snap(origStart + (minute - downMinute), step).coerceIn(0, ClockTime.DAY_MINUTES - length)
                                            DragPreview(entryHit!!.entry.id, dayAt(change.position.x), start, start + length)
                                        }
                                        DragMode.RESIZE_START -> DragPreview(entryHit!!.entry.id, day, ClockTime.snap(minute, step).coerceAtMost(origEnd - step), origEnd)
                                        DragMode.RESIZE_END -> DragPreview(entryHit!!.entry.id, day, origStart, ClockTime.snap(minute, step).coerceAtLeast(origStart + step))
                                        DragMode.CREATE -> {
                                            val a = ClockTime.snap(downMinute, step)
                                            val b = ClockTime.snap(minute, step)
                                            DragPreview(null, day, minOf(a, b), maxOf(maxOf(a, b), minOf(a, b) + step))
                                        }
                                    }
                                }
                                val result = preview
                                preview = null
                                if (dragging && result != null && mode != null) {
                                    commitDrag(mode, result, entryHit?.entry, dayList, dayList.flatMap { it.entries }, actions)
                                } else if (!dragging) {
                                    when (hit) {
                                        is Hit.EntryHit -> onSelect(TimesheetSelection.Entry(hit.entry.id))
                                        is Hit.SessionHit -> onSelect(TimesheetSelection.Session(hit.session.id))
                                        null -> {
                                            val doubleClick = down.uptimeMillis - lastClick < viewConfiguration.doubleTapTimeoutMillis &&
                                                (down.position - lastClickAt).getDistance() < viewConfiguration.touchSlop
                                            if (doubleClick) {
                                                val start = (downMinute / 15 * 15).coerceAtMost(ClockTime.DAY_MINUTES - 60)
                                                actions.create(null, null, ClockTime.at(dayStart, start), ClockTime.at(dayStart, start + 60))
                                            } else onSelect(null)
                                        }
                                    }
                                    lastClick = down.uptimeMillis
                                    lastClickAt = down.position
                                }
                            }
                        }
                ) {
                    val colWidth = maxWidth / days.size.coerceAtLeast(1)
                    val today = days.indexOfFirst { it.range.contains(now) }
                    if (today >= 0) {
                        Box(Modifier.offset(x = colWidth * today).width(colWidth).fillMaxHeight().background(colors.primary.copy(alpha = 0.04f)))
                    }
                    days.forEachIndexed { index, day ->
                        val x = colWidth * index
                        day.sessions.forEach { session ->
                            val start = ClockTime.minutesOf(session.clockIn, day.range.start)
                            val end = ClockTime.minutesOf(session.clockOut ?: now, day.range.start)
                            val selected = selection == TimesheetSelection.Session(session.id)
                            // Clocked-in time tints the whole column, so unlogged stretches stand out.
                            Box(Modifier.offset(x = x, y = minutesToDp(start)).size(colWidth, minutesToDp(end - start)).background(colors.primary.copy(alpha = 0.06f)))
                            Box(
                                Modifier
                                    .offset(x = x + 2.dp, y = minutesToDp(start))
                                    .size(AttendanceStrip - 3.dp, minutesToDp(end - start).coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (selected) colors.primary else colors.primary.copy(alpha = 0.45f))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("timesheet_session_${session.id}")
                            )
                        }
                        val lanes = day.lanes(now)
                        val blockWidth = colWidth - AttendanceStrip - 2.dp
                        day.entries.forEach { entry ->
                            val dragged = preview?.takeIf { it.entryId == entry.id }
                            if (dragged != null) return@forEach
                            val lane = lanes[entry.id] ?: Lane(0, 1)
                            val start = ClockTime.minutesOf(entry.startTime, day.range.start)
                            val end = ClockTime.minutesOf(entry.endTime ?: now, day.range.start)
                            EntryBlock(
                                entry = entry, startMin = start, endMin = end,
                                selected = selection == TimesheetSelection.Entry(entry.id),
                                modifier = Modifier
                                    .offset(x = x + AttendanceStrip + blockWidth / lane.count * lane.index, y = minutesToDp(start))
                                    .size(blockWidth / lane.count - 1.dp, minutesToDp(end - start))
                            )
                        }
                    }
                    preview?.let { p ->
                        val entry = p.entryId?.let { id -> allEntries.firstOrNull { it.id == id } }
                        val x = colWidth * p.day + AttendanceStrip
                        val modifierAt = Modifier.offset(x = x, y = minutesToDp(p.startMin)).size(colWidth - AttendanceStrip - 2.dp, minutesToDp(p.endMin - p.startMin))
                        if (entry != null) EntryBlock(entry, p.startMin, p.endMin, selected = true, modifier = modifierAt, dragging = true)
                        else Box(modifierAt.clip(BlockShape).background(colors.primary.copy(alpha = 0.35f)).border(2.dp, colors.primary, BlockShape)) {
                            Text("${ClockTime.format(p.startMin)}–${ClockTime.format(p.endMin)}", fontSize = 11.sp, color = colors.onSurface, modifier = Modifier.padding(4.dp))
                        }
                    }
                    if (today >= 0) {
                        val minute = ClockTime.minutesOf(now, days[today].range.start)
                        Box(Modifier.offset(x = colWidth * today, y = minutesToDp(minute) - 1.dp).width(colWidth).height(2.dp).background(colors.error))
                    }
                }
            }
        }
    }
}

private fun minutesToDp(minutes: Int): Dp = HourHeight * (minutes / 60f)

@Composable
private fun DayHeaders(days: List<TimesheetDay>, now: Long) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Box(Modifier.width(GutterWidth))
        days.forEach { day ->
            val isToday = day.range.contains(now)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(
                    DateFormats.weekdayDay(day.range.start),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isToday) colors.primary else colors.onSurface
                )
                Text(
                    day.workedSeconds(now).let { if (it > 0) TimeFormat.hm(it) else "–" },
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HourGutter() {
    val colors = MaterialTheme.colorScheme
    Box(modifier = Modifier.width(GutterWidth).fillMaxHeight()) {
        for (h in 1 until 24) {
            Text(
                ClockTime.format(h * 60),
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.offset(x = 8.dp, y = HourHeight * h - 8.dp)
            )
        }
    }
}

@Composable
private fun EntryBlock(entry: TimeEntryWithDetails, startMin: Int, endMin: Int, selected: Boolean, modifier: Modifier, dragging: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val base = if (entry.projectId == null) colors.outline else projectColor(entry.projectColor ?: "")
    val fill = if (entry.isProductive) base.copy(alpha = 0.85f) else base.copy(alpha = 0.45f)
    val text = if (fill.luminanceIsDark()) Color.White else Color.Black
    val height = minutesToDp(endMin - startMin)
    Box(
        modifier = modifier
            .clip(BlockShape)
            .background(fill.copy(alpha = if (dragging) 0.7f else fill.alpha))
            .border(if (selected) 2.dp else 0.dp, if (selected) colors.onSurface else Color.Transparent, BlockShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("timesheet_entry_${entry.id}")
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        if (height >= 14.dp) {
            Column {
                Text(
                    entry.projectCode ?: "—",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = text, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (height >= 30.dp) {
                    Text(
                        "${ClockTime.format(startMin)}–${if (entry.isRunning && !dragging) "…" else ClockTime.format(endMin)}" +
                            (entry.taskTitle?.let { " · $it" } ?: ""),
                        fontSize = 10.sp, color = text.copy(alpha = 0.85f), maxLines = if (height >= 44.dp) 2 else 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.6f

private sealed interface Hit {
    data class EntryHit(val entry: TimeEntryWithDetails, val edge: DragMode) : Hit
    data class SessionHit(val session: AttendanceSession) : Hit
}

private fun hitTest(at: Offset, day: TimesheetDay, index: Int, colW: Float, pxPerMin: Float, stripPx: Float, now: Long, edge: Float): Hit? {
    val left = index * colW
    val x = at.x - left
    if (x < stripPx) {
        day.sessions.firstOrNull { s ->
            val top = ClockTime.minutesOf(s.clockIn, day.range.start) * pxPerMin
            val bottom = ClockTime.minutesOf(s.clockOut ?: now, day.range.start) * pxPerMin
            at.y in top..maxOf(bottom, top + 4f)
        }?.let { return Hit.SessionHit(it) }
    }
    val lanes = day.lanes(now)
    val width = colW - stripPx
    // Later (shorter-lived on top) blocks win when they touch.
    for (entry in day.entries.asReversed()) {
        val lane = lanes[entry.id] ?: Lane(0, 1)
        val bx = stripPx + width / lane.count * lane.index
        if (x < bx || x > bx + width / lane.count) continue
        val top = ClockTime.minutesOf(entry.startTime, day.range.start) * pxPerMin
        val bottom = ClockTime.minutesOf(entry.endTime ?: now, day.range.start) * pxPerMin
        if (at.y < top || at.y > bottom) continue
        val zone = minOf(edge, (bottom - top) / 4f)
        val mode = when {
            at.y - top <= zone -> DragMode.RESIZE_START
            bottom - at.y <= zone -> DragMode.RESIZE_END
            else -> DragMode.MOVE
        }
        return Hit.EntryHit(entry, mode)
    }
    return null
}

private fun commitDrag(mode: DragMode, p: DragPreview, entry: TimeEntryWithDetails?, days: List<TimesheetDay>, all: List<TimeEntryWithDetails>, actions: TimesheetActions) {
    val dayStart = days[p.day].range.start
    when (mode) {
        DragMode.CREATE -> actions.create(null, null, ClockTime.at(dayStart, p.startMin), ClockTime.at(dayStart, p.endMin))
        DragMode.MOVE -> TimesheetEdits.moved(entry!!, ClockTime.at(dayStart, p.startMin), all)?.let(actions.save)
        // The edge not dragged keeps its exact time (it may not sit on the 5-minute grid, or lie past midnight).
        DragMode.RESIZE_START -> TimesheetEdits.retimed(entry!!, ClockTime.at(dayStart, p.startMin), entry.endTime, all)?.let(actions.save)
        DragMode.RESIZE_END -> TimesheetEdits.retimed(entry!!, entry.startTime, ClockTime.at(dayStart, p.endMin), all)?.let(actions.save)
    }
}

/** Keyboard on the focused calendar. Returns true when the key was used. */
private fun handleCalendarKey(
    key: Key, alt: Boolean, shift: Boolean, command: Boolean,
    days: List<TimesheetDay>, selection: TimesheetSelection?, now: Long,
    onSelect: (TimesheetSelection?) -> Unit, actions: TimesheetActions, onEditSelected: () -> Unit,
): Boolean {
    if (command) return false
    val all = days.flatMap { it.entries }
    val entry = (selection as? TimesheetSelection.Entry)?.let { sel -> all.firstOrNull { it.id == sel.id } }
    val dayIndex = entry?.let { e -> days.indexOfFirst { it.range.contains(e.startTime) } } ?: -1
    val nudge = TimesheetEdits.NUDGE_MINUTES * 60_000L
    val vertical = key == Key.DirectionUp || key == Key.DirectionDown
    val horizontal = key == Key.DirectionLeft || key == Key.DirectionRight
    val sign = if (key == Key.DirectionUp || key == Key.DirectionLeft) -1 else 1

    if (key == Key.Escape) { onSelect(null); return selection != null }
    if (key == Key.N) {
        val dayStart = (if (dayIndex >= 0) days[dayIndex] else days.firstOrNull { it.range.contains(now) } ?: days.first()).range.start
        val start = entry?.let { it.endTime ?: now } ?: ClockTime.at(dayStart, 8 * 60)
        actions.create(entry?.projectId, entry?.taskId, start, start + 3600_000L)
        return true
    }
    if (entry == null) {
        if (!vertical && !horizontal) return false
        // Nothing selected yet: start at today's (or the week's) first activity.
        val first = days.firstOrNull { it.range.contains(now) }?.entries?.firstOrNull() ?: all.firstOrNull()
        first?.let { onSelect(TimesheetSelection.Entry(it.id)) }
        return first != null
    }
    val dayEntries = days[dayIndex].entries
    return when {
        key == Key.Delete || key == Key.Backspace -> { actions.delete(entry); true }
        key == Key.Enter || key == Key.NumPadEnter -> { onEditSelected(); true }
        alt && vertical -> {
            TimesheetEdits.moved(entry, entry.startTime + sign * nudge, all)?.let(actions.save)
            true
        }
        shift && vertical -> {
            entry.endTime?.let { end -> TimesheetEdits.retimed(entry, entry.startTime, end + sign * nudge, all)?.let(actions.save) }
            true
        }
        alt && horizontal -> {
            val target = dayIndex + sign
            if (target in days.indices) TimesheetEdits.moved(entry, ClockTime.shiftDays(entry.startTime, sign), all)?.let(actions.save)
            true
        }
        vertical -> {
            val i = dayEntries.indexOfFirst { it.id == entry.id } + sign
            dayEntries.getOrNull(i)?.let { onSelect(TimesheetSelection.Entry(it.id)) }
            true
        }
        horizontal -> {
            // The activity at about the same time on the nearest day that has any.
            val minute = ClockTime.minutesOf(entry.startTime, days[dayIndex].range.start)
            var d = dayIndex + sign
            while (d in days.indices && days[d].entries.isEmpty()) d += sign
            days.getOrNull(d)?.entries?.minByOrNull { abs(ClockTime.minutesOf(it.startTime, days[d].range.start) - minute) }
                ?.let { onSelect(TimesheetSelection.Entry(it.id)) }
            true
        }
        else -> false
    }
}

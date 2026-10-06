package com.suw1labs.worktracker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.backup.DemoData
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.platform.NoOpFileExporter
import com.suw1labs.worktracker.platform.NoOpReminderScheduler
import com.suw1labs.worktracker.ui.timesheet.ClockTime
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.currentTimeMillis
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Timesheet tab in a desktop-sized window, driven with mouse and keyboard against a real database. */
@OptIn(ExperimentalTestApi::class)
class TimesheetScreenTest {
    private val monday = DateRanges.weekRange(currentTimeMillis()).start
    private fun at(minutes: Int) = ClockTime.at(monday, minutes)

    private fun container() = AppContainer(
        databaseBuilder = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()),
        reminderScheduler = NoOpReminderScheduler,
        fileExporter = NoOpFileExporter,
        platformName = "desktop"
    )

    /** Monday: clocked in 08:00–12:00, P-1001 08:00–10:00 then P-1002 10:00–12:00. (The container seeds the defaults itself.) */
    private fun AppContainer.seedMonday(): Pair<Long, Long> = runBlocking {
        val p1 = database.projectDao().insertProject(Project(code = "P-1001.1", name = "Retrofit", colorHex = "#2F6BFF"))
        val p2 = database.projectDao().insertProject(Project(code = "P-1002.1", name = "Commissioning", colorHex = "#10B981"))
        database.attendanceDao().insertSession(AttendanceSession(clockIn = at(8 * 60), clockOut = at(12 * 60)))
        val a = database.timeEntryDao().insertEntry(TimeEntry(projectId = p1, startTime = at(8 * 60), endTime = at(10 * 60)))
        val b = database.timeEntryDao().insertEntry(TimeEntry(projectId = p2, startTime = at(10 * 60), endTime = at(12 * 60)))
        a to b
    }

    private fun AppContainer.entry(id: Long): TimeEntry = runBlocking { database.backupDao().allTimeEntries().first { it.id == id } }
    private fun AppContainer.liveEntries(): List<TimeEntry> = runBlocking { database.backupDao().allTimeEntries().filter { it.deletedAt == null } }

    @Test
    fun table_typingAnEndTime_movesTheNextActivityAlong() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val app = container()
        val (a, b) = app.seedMonday()
        setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }

        onNodeWithTag("timesheet_mode_table").performClick()
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("timesheet_end_$a").assertExistsSafely() }.getOrDefault(false) }
        onNodeWithTag("timesheet_end_$a").performClick()
        onNodeWithTag("timesheet_end_$a").performTextReplacement("1030")
        onNodeWithTag("timesheet_end_$a").performKeyInput { pressKey(Key.Enter) }

        waitUntil(timeoutMillis = 5_000) { app.entry(a).endTime == at(10 * 60 + 30) }
        // P-1002 ran right after it, so it now starts at 10:30 instead of overlapping.
        waitUntil(timeoutMillis = 5_000) { app.entry(b).startTime == at(10 * 60 + 30) }
        assertEquals(at(12 * 60), app.entry(b).endTime)
    }

    @Test
    fun calendar_keyboardMovesTheSelectedActivity() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val app = container()
        val (_, b) = app.seedMonday()
        setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }

        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("timesheet_entry_$b").assertExistsSafely() }.getOrDefault(false) }
        onNodeWithTag("timesheet_entry_$b").performClick()
        // Scrolling with the wheel keeps the keyboard on the calendar.
        onNodeWithTag("timesheet_calendar").performMouseInput { scroll(3f) }
        waitForIdle()
        onNodeWithTag("timesheet_calendar").performKeyInput {
            keyDown(Key.AltLeft); pressKey(Key.DirectionDown); keyUp(Key.AltLeft)   // 15 min later
            keyDown(Key.ShiftLeft); pressKey(Key.DirectionDown); keyUp(Key.ShiftLeft) // ends 15 min later still
        }
        waitUntil(timeoutMillis = 5_000) { app.entry(b).startTime == at(10 * 60 + 15) && app.entry(b).endTime == at(12 * 60 + 30) }
    }

    @Test
    fun calendar_draggingOnAnEmptySpotLogsANewActivity() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val app = container()
        app.seedMonday()
        setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }
        waitUntil(timeoutMillis = 5_000) { app.liveEntries().size == 2 }

        // Tuesday column, dragged downwards over an hour. The calendar opens scrolled to the morning,
        // so the drag lands at whatever time is in view; what counts is the day and the length.
        val calendar = onNodeWithTag("timesheet_calendar")
        val bounds = calendar.fetchSemanticsNode().boundsInRoot
        // Mon–Fri are the working days by default and the weekend is empty, so five columns.
        val x = bounds.width / 5f * 1.5f
        val hourPx = with(density) { 52.dp.toPx() }
        calendar.performMouseInput {
            moveTo(Offset(x, hourPx * 2)); press()
            moveTo(Offset(x, hourPx * 2.5f)); moveTo(Offset(x, hourPx * 3))
            release()
        }
        waitUntil(timeoutMillis = 5_000) { app.liveEntries().size == 3 }
        val created = app.liveEntries().maxBy { it.id }
        val tuesday = DateRanges.dayRange(ClockTime.shiftDays(monday, 1))
        assertTrue(tuesday.contains(created.startTime), "logged on Tuesday")
        assertEquals(3600_000L, created.endTime!! - created.startTime)
        assertEquals(0L, (created.startTime - tuesday.start) % (5 * 60_000L), "on the 5-minute grid")
    }

    @Test
    fun calendar_draggingAnEdgeNearTheNextActivity_snapsOntoIt() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val app = container()
        // 08:00–10:00, then a gap, then 10:20–12:00.
        val (a, b) = runBlocking {
            val p = app.database.projectDao().insertProject(Project(code = "P-1001.1", name = "Retrofit"))
            app.database.timeEntryDao().insertEntry(TimeEntry(projectId = p, startTime = at(8 * 60), endTime = at(10 * 60))) to
                app.database.timeEntryDao().insertEntry(TimeEntry(projectId = p, startTime = at(10 * 60 + 20), endTime = at(12 * 60)))
        }
        setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("timesheet_entry_$b").assertExistsSafely() }.getOrDefault(false) }
        waitForIdle()

        // The calendar opens at 07:30 (half an hour before the first activity), 52 dp per hour.
        val calendar = onNodeWithTag("timesheet_calendar")
        val width = calendar.fetchSemanticsNode().boundsInRoot.width
        val minute = with(density) { 52.dp.toPx() } / 60f
        fun y(m: Int) = (m - 450) * minute
        val x = width / 5f * 0.5f
        calendar.performMouseInput {
            moveTo(Offset(x, y(600) - 2f)); press()          // the bottom edge of 08:00–10:00
            moveTo(Offset(x, y(610))); moveTo(Offset(x, y(617)))  // let go at about 10:17
            release()
        }
        // 10:17 is 3 minutes from where the next activity starts: the edge lands on 10:20, not on 10:15.
        waitUntil(timeoutMillis = 5_000) { app.entry(a).endTime == at(10 * 60 + 20) }
        assertEquals(at(10 * 60 + 20), app.entry(b).startTime)
    }

    @Test
    fun narrowWindow_hasNoTimesheetTab() = runDesktopComposeUiTest(width = 400, height = 800) {
        val app = container()
        setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }
        onNodeWithTag("nav_today").assertExistsSafely()
        assertTrue(runCatching { onNodeWithTag("nav_timesheet").assertExistsSafely() }.getOrDefault(false).not())
    }

    /** Screenshots of both modes with the fictional demo data, written only when TIMESHEET_SHOTS names a folder. */
    @Test
    fun screenshots() {
        val dir = System.getenv("TIMESHEET_SHOTS")?.let(::File) ?: return
        runDesktopComposeUiTest(width = 1440, height = 900) {
            val app = container()
            runBlocking { DemoData.seed(app.backupManager, "en") }
            setContent { App(app, AppLaunchOptions(initialTab = TrackerDestination.TIMESHEET)) }
            waitForIdle()
            onNodeWithTag("timesheet_calendar").performMouseInput { click(Offset(10f, 10f)) }
            onNodeWithTag("timesheet_calendar").performKeyInput { pressKey(Key.DirectionDown) }
            waitForIdle()
            ImageIO.write(onAllNodesRoot().captureToImage().toAwtImage(), "png", File(dir, "timesheet-calendar.png"))
            onNodeWithTag("timesheet_mode_table").performClick()
            waitForIdle()
            ImageIO.write(onAllNodesRoot().captureToImage().toAwtImage(), "png", File(dir, "timesheet-table.png"))
        }
    }
}

private fun SemanticsNodeInteraction.assertExistsSafely(): Boolean { fetchSemanticsNode(); return true }

@OptIn(ExperimentalTestApi::class)
private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesRoot(): SemanticsNodeInteraction = onAllNodes(isRoot())[0]

package com.suw1labs.worktracker

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Clocked in late: the clock-in on Today's timeline is tapped and corrected, with a time picker only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TodayClockInEditTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun tappingTheClockIn_opensATimeOnlyEditor_andSaves() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).buildAppDatabase(DatabaseCreationTracker(), Dispatchers.IO)
    val repo = TimeTrackerRepository(db.projectDao(), db.workTaskDao(), db.timeEntryDao(), db.attendanceDao(), db.dayRecordDao(), db.settingsDao())
    // Clocked in at the start of the current hour (always today, whatever time the test runs).
    val clockIn = currentTimeMillis().let { it - it % 3600_000L }.coerceAtLeast(DateRanges.startOfToday())
    val id = runBlocking { db.attendanceDao().insertSession(AttendanceSession(clockIn = clockIn)) }
    val viewModel = TrackerViewModel(repo)

    composeTestRule.setContent { MyApplicationTheme { MainAppContent(viewModel = viewModel, askNotificationPermission = false) } }

    val row = "session-$id-in"
    composeTestRule.waitUntil(5_000) {
      runCatching { composeTestRule.onNodeWithTag("today_list").performScrollToNode(hasTestTag(row)); true }.getOrDefault(false)
    }
    composeTestRule.onNodeWithTag(row).performClick()
    composeTestRule.onNodeWithTag("session_form_dialog").assertExists()

    // The field shows the time only, and picking it skips the date step.
    composeTestRule.onNode(hasText(DateFormats.hourMinute(clockIn)) and hasAnyAncestor(hasTestTag("session_clock_in_field"))).performClick()
    assertEquals(0, composeTestRule.onAllNodesWithText("Next").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.onNodeWithTag("save_session_button").performClick()

    composeTestRule.waitUntil(5_000) { runCatching { composeTestRule.onNodeWithTag("session_form_dialog").assertDoesNotExist(); true }.getOrDefault(false) }
    assertEquals(clockIn, runBlocking { db.backupDao().allSessions().single { it.id == id }.clockIn })
  }
}

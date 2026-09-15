package com.suw1labs.worktracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.suw1labs.worktracker.data.androidDatabaseBuilder
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun app_screenshot() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = androidDatabaseBuilder(context).buildAppDatabase(DatabaseCreationTracker())
    val repo = TimeTrackerRepository(
      projectDao = db.projectDao(),
      categoryDao = db.workCategoryDao(),
      taskDao = db.workTaskDao(),
      timeEntryDao = db.timeEntryDao(),
      attendanceDao = db.attendanceDao(),
      dayRecordDao = db.dayRecordDao(),
      settingsDao = db.settingsDao()
    )
    val viewModel = TrackerViewModel(repo)

    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContent(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}


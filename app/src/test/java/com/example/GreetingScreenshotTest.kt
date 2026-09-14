package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.repository.TimeTrackerRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TrackerViewModel
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
    val db = AppDatabase.getInstance(context)
    val repo = TimeTrackerRepository(db.projectDao(), db.workTaskDao(), db.timeEntryDao())
    val viewModel = TrackerViewModel(repo)

    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppContent(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}


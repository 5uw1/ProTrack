package com.suw1labs.worktracker

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.ui.screens.SapSettingsDialog
import com.suw1labs.worktracker.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SapSettingsDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun costObjectAlone_canBeSaved() {
    var saved: Triple<String, String, String>? = null
    composeTestRule.setContent {
      MyApplicationTheme {
        SapSettingsDialog(settings = AppSettings(), onDismiss = {}, onSave = { p, u, n -> saved = Triple(p, u, n) })
      }
    }

    composeTestRule.onNodeWithTag("sap_number_input").performTextInput("1000")
    composeTestRule.onNodeWithTag("save_sap_button").assertIsEnabled().performClick()

    assertEquals(Triple("", "", "1000"), saved)
  }
}

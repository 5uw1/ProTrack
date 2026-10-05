package com.suw1labs.worktracker

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.backup.DemoData
import com.suw1labs.worktracker.platform.NoOpFileExporter
import com.suw1labs.worktracker.platform.NoOpReminderScheduler
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Every tab at common Mac window sizes with the fictional demo data, for looking at the wide
 * layouts. Runs only when WIDE_SHOTS names an output folder:
 * `WIDE_SHOTS=/tmp/shots ./gradlew :composeApp:desktopTest --tests '*WideWindowScreenshots*'`
 */
@OptIn(ExperimentalTestApi::class)
class WideWindowScreenshots {
    private val sizes = listOf(1280 to 800, 1920 to 1080, 2560 to 1440)

    @Test
    fun everyTab() {
        val dir = System.getenv("WIDE_SHOTS")?.let(::File)?.also { it.mkdirs() } ?: return
        for ((w, h) in sizes) runDesktopComposeUiTest(width = w, height = h) {
            val app = AppContainer(
                databaseBuilder = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()),
                reminderScheduler = NoOpReminderScheduler,
                fileExporter = NoOpFileExporter,
                platformName = "desktop"
            )
            runBlocking { DemoData.seed(app.backupManager, "en") }
            setContent { App(app, AppLaunchOptions(demo = true)) }
            fun shot(name: String) {
                waitForIdle()
                ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(dir, "${w}x$h-$name.png"))
            }
            shot("1-today")
            tab("nav_timesheet"); shot("2-timesheet-calendar")
            onNodeWithTag("timesheet_mode_table").performClick(); shot("3-timesheet-table")
            tab("nav_projects"); shot("4-projects")
            tab("nav_reports"); shot("5-reports")
            for (period in listOf("Week", "Month")) {
                runCatching { onNodeWithText(period).performClick(); shot("5-reports-${period.lowercase()}") }
            }
            tab("nav_settings"); shot("6-settings")
        }
    }

    private fun ComposeUiTest.tab(tag: String) = onNodeWithTag(tag).performClick()
}

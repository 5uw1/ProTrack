package com.suw1labs.worktracker

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.suw1labs.worktracker.data.desktopDatabaseBuilder
import com.suw1labs.worktracker.platform.DesktopFileExporter
import com.suw1labs.worktracker.platform.DesktopReminderScheduler

/** Process-wide dependency graph for the desktop (Windows / macOS / Linux) app. */
object DesktopAppGraph {
    val container: AppContainer by lazy {
        AppContainer(
            databaseBuilder = desktopDatabaseBuilder(),
            reminderScheduler = DesktopReminderScheduler(),
            fileExporter = DesktopFileExporter(),
            platformName = "desktop"
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "WorkTracker",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1100.dp, height = 760.dp)
    ) {
        App(DesktopAppGraph.container)
    }
}

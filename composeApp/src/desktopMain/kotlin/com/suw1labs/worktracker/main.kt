package com.suw1labs.worktracker

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.suw1labs.worktracker.data.desktopDatabaseBuilder
import com.suw1labs.worktracker.platform.DesktopAppUpdater
import com.suw1labs.worktracker.platform.DesktopBackupFolderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlin.system.exitProcess
import com.suw1labs.worktracker.platform.DesktopFileExporter
import com.suw1labs.worktracker.platform.DesktopReminderScheduler

/** Process-wide dependency graph for the desktop (Windows / macOS / Linux) app. */
object DesktopAppGraph {
    val container: AppContainer by lazy {
        AppContainer(
            databaseBuilder = desktopDatabaseBuilder(),
            reminderScheduler = DesktopReminderScheduler(),
            fileExporter = DesktopFileExporter(),
            platformName = "desktop",
            backupFolderStore = DesktopBackupFolderStore(),
            appUpdater = updater
        )
    }

    /** Updates from GitHub Releases; quitting closes the database cleanly before the installer runs. */
    private val updater: DesktopAppUpdater by lazy {
        DesktopAppUpdater(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            quit = {
                runCatching { container.database.close() }
                exitProcess(0)
            }
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
        // Look for a newer release once per launch (packaged builds only).
        LaunchedEffect(Unit) { DesktopAppGraph.container.appUpdater.check() }
    }
}

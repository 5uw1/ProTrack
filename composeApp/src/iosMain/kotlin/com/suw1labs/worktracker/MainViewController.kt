package com.suw1labs.worktracker

import androidx.compose.ui.window.ComposeUIViewController
import com.suw1labs.worktracker.data.iosDatabaseBuilder
import com.suw1labs.worktracker.platform.IosFileExporter
import com.suw1labs.worktracker.platform.IosReminderScheduler
import com.suw1labs.worktracker.platform.IosWidgetBridge
import platform.UIKit.UIViewController

/** Process-wide dependency graph for the iOS app. */
object IosAppGraph {
    val container: AppContainer by lazy {
        AppContainer(
            databaseBuilder = iosDatabaseBuilder(),
            reminderScheduler = IosReminderScheduler(),
            fileExporter = IosFileExporter(),
            widgetBridge = IosWidgetBridge(),
            platformName = "ios"
        )
    }
}

/** Entry point called from Swift (`ContentView.swift`). */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(IosAppGraph.container)
}

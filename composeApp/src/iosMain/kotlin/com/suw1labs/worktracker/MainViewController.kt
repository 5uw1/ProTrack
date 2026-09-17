package com.suw1labs.worktracker

import androidx.compose.ui.window.ComposeUIViewController
import com.suw1labs.worktracker.data.backup.DemoData
import kotlinx.coroutines.launch
import platform.Foundation.NSProcessInfo
import com.suw1labs.worktracker.data.iosDatabaseBuilder
import com.suw1labs.worktracker.platform.IosBackupFolderStore
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
            platformName = "ios",
            backupFolderStore = IosBackupFolderStore()
        )
    }
}

/** Entry point called from Swift (`ContentView.swift`). */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController {
    // Screenshot / demo flags, simulator only: xcrun simctl launch booted <bundle> -demo -tab reports -lang de
    val process = NSProcessInfo.processInfo
    val isSimulator = process.environment["SIMULATOR_DEVICE_NAME"] != null
    val launchOptions = if (isSimulator) AppLaunchOptions.fromArguments(process.arguments.filterIsInstance<String>()) else AppLaunchOptions.NONE
    launchOptions.applyClock()
    val container = IosAppGraph.container
    if (launchOptions.demo) container.appScope.launch { DemoData.seed(container.backupManager, launchOptions.language) }
    return ComposeUIViewController {
        App(container, launchOptions)
    }
}

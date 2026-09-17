package com.suw1labs.worktracker

import com.suw1labs.worktracker.platform.WidgetActions
import kotlinx.coroutines.launch
import platform.Foundation.NSLog

/**
 * Called from Swift whenever the app becomes active: replays the clock in / clock out / switch
 * actions the home-screen widget queued while the app was not running.
 */
@Suppress("unused")
fun applyPendingWidgetActions() {
    val container = IosAppGraph.container
    container.appScope.launch {
        runCatching { WidgetActions.applyPending(container.repository) }
            .onFailure { NSLog("WidgetActions: failed to apply pending actions: ${it.message}") }
    }
}

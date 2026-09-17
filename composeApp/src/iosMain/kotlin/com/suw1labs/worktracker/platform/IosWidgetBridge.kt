package com.suw1labs.worktracker.platform

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

/** App Group shared by the app and the widget extension (must match both entitlements files). */
const val WIDGET_APP_GROUP = "group.com.suw1labs.worktracker"

/**
 * Swift installs a handler here that calls `WidgetCenter.shared.reloadAllTimelines()`. WidgetKit has
 * no Objective-C API, so the reload cannot be triggered from Kotlin directly.
 */
object WidgetReload {
    var handler: (() -> Unit)? = null
}

/**
 * Writes the widget state into the App Group's UserDefaults, where the WidgetKit extension
 * (`iosApp/WorkTrackerWidget`) reads it, then asks WidgetKit to refresh.
 */
class IosWidgetBridge(private val appGroup: String = WIDGET_APP_GROUP) : WidgetBridge {

    override fun publish(snapshot: WidgetSnapshot) {
        val defaults = NSUserDefaults(suiteName = appGroup)
        defaults.setBool(snapshot.clockedIn, KEY_CLOCKED_IN)
        defaults.setDoubleOrRemove(snapshot.clockedInSince?.toEpochSeconds(), KEY_CLOCKED_IN_SINCE)
        defaults.setInteger(snapshot.attendanceSecondsBefore, KEY_ATTENDANCE_BEFORE)
        defaults.setInteger(snapshot.targetSeconds, KEY_TARGET_SECONDS)
        defaults.setStringOrRemove(snapshot.runningProject, KEY_RUNNING_PROJECT)
        defaults.setStringOrRemove(snapshot.runningTask, KEY_RUNNING_TASK)
        defaults.setDoubleOrRemove(snapshot.runningSince?.toEpochSeconds(), KEY_RUNNING_SINCE)
        defaults.setInteger(snapshot.runningTaskId ?: -1L, KEY_RUNNING_TASK_ID)
        // Kotlin lists / maps / strings / numbers bridge to NSArray / NSDictionary / NSString / NSNumber.
        defaults.setObject(
            snapshot.quickTasks.map {
                mapOf(
                    "taskId" to it.taskId,
                    "projectId" to it.projectId,
                    "title" to it.title,
                    "projectLabel" to it.projectLabel,
                    "productive" to it.productive
                )
            },
            KEY_QUICK_TASKS
        )
        defaults.setDouble(snapshot.updatedAt.toEpochSeconds(), KEY_UPDATED_AT)
        defaults.synchronize()
        WidgetReload.handler?.invoke()
    }

    private fun Long.toEpochSeconds(): Double = this / 1000.0

    private fun NSUserDefaults.setDoubleOrRemove(value: Double?, key: String) {
        if (value == null) removeObjectForKey(key) else setDouble(value, key)
    }

    private fun NSUserDefaults.setStringOrRemove(value: String?, key: String) {
        if (value == null) removeObjectForKey(key) else setObject(value, key)
    }

    companion object {
        // Keys are read by WorkTrackerWidget.swift; keep both sides in sync.
        const val KEY_CLOCKED_IN = "widget.clockedIn"
        const val KEY_CLOCKED_IN_SINCE = "widget.clockedInSince"
        const val KEY_ATTENDANCE_BEFORE = "widget.attendanceSecondsBefore"
        const val KEY_TARGET_SECONDS = "widget.targetSeconds"
        const val KEY_RUNNING_PROJECT = "widget.runningProject"
        const val KEY_RUNNING_TASK = "widget.runningTask"
        const val KEY_RUNNING_SINCE = "widget.runningSince"
        const val KEY_RUNNING_TASK_ID = "widget.runningTaskId"
        const val KEY_QUICK_TASKS = "widget.quickTasks"
        const val KEY_UPDATED_AT = "widget.updatedAt"
        /** Actions the widget's buttons queued while the app was not running (see WidgetActions). */
        const val KEY_PENDING_ACTIONS = "widget.pendingActions"
    }
}

/**
 * The widget extension cannot open the database, so its buttons append `{type, at, taskId,
 * projectId}` records to [IosWidgetBridge.KEY_PENDING_ACTIONS] and update the shown state
 * optimistically. The app replays them, in order and with their original timestamps, as soon as it
 * becomes active; the regular snapshot publish then corrects the widget.
 */
object WidgetActions {
    const val CLOCK_IN = "clockIn"
    const val CLOCK_OUT = "clockOut"
    const val SWITCH_TASK = "switchTask"

    suspend fun applyPending(
        repository: com.suw1labs.worktracker.data.repository.TimeTrackerRepository,
        appGroup: String = WIDGET_APP_GROUP
    ): Int {
        val defaults = NSUserDefaults(suiteName = appGroup)
        val raw = defaults.arrayForKey(IosWidgetBridge.KEY_PENDING_ACTIONS) ?: return 0
        // Clear first so a crash while applying cannot replay the same actions forever.
        defaults.removeObjectForKey(IosWidgetBridge.KEY_PENDING_ACTIONS)
        defaults.synchronize()

        val actions = raw.mapNotNull { it as? Map<*, *> }
            .mapNotNull { m ->
                val at = m["at"].asDouble() ?: return@mapNotNull null
                PendingAction(
                    type = m["type"] as? String ?: return@mapNotNull null,
                    atMillis = (at * 1000.0).toLong(),
                    taskId = m["taskId"].asLong(),
                    projectId = m["projectId"].asLong()
                )
            }
            .sortedBy { it.atMillis }

        for (action in actions) {
            when (action.type) {
                CLOCK_IN -> repository.clockIn(action.atMillis)
                CLOCK_OUT -> repository.clockOut(action.atMillis)
                SWITCH_TASK -> repository.startActivity(action.projectId, action.taskId, "", action.atMillis)
            }
        }
        return actions.size
    }

    private data class PendingAction(val type: String, val atMillis: Long, val taskId: Long?, val projectId: Long?)

    private fun Any?.asDouble(): Double? = when (this) {
        is Number -> toDouble()
        is NSNumber -> doubleValue
        else -> null
    }

    private fun Any?.asLong(): Long? = when (this) {
        is Number -> toLong()
        is NSNumber -> longLongValue
        else -> null
    }?.takeIf { it >= 0 }
}

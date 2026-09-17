package com.suw1labs.worktracker

import android.app.Application
import android.content.Context
import com.suw1labs.worktracker.data.androidDatabaseBuilder
import com.suw1labs.worktracker.platform.AndroidFileExporter
import com.suw1labs.worktracker.platform.AndroidReminderScheduler
import com.suw1labs.worktracker.platform.AndroidWidgetBridge

class WorkTrackerApplication : Application() {

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        /** Android exporter instance so the activity can attach the "save file" picker. */
        @Volatile
        var fileExporter: AndroidFileExporter? = null
            private set

        /** Process-wide dependency graph, created lazily on first use. */
        fun container(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val exporter = AndroidFileExporter(context.applicationContext)
                    fileExporter = exporter
                    AppContainer(
                        databaseBuilder = androidDatabaseBuilder(context),
                        reminderScheduler = AndroidReminderScheduler(context.applicationContext),
                        fileExporter = exporter,
                        widgetBridge = AndroidWidgetBridge(context.applicationContext)
                    ).also { instance = it }
                }
            }
        }
    }
}

package com.suw1labs.worktracker

import androidx.room.RoomDatabase
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import com.suw1labs.worktracker.platform.FileExporter
import com.suw1labs.worktracker.platform.NoOpWidgetBridge
import com.suw1labs.worktracker.platform.ReminderScheduler
import com.suw1labs.worktracker.platform.WidgetBridge
import com.suw1labs.worktracker.platform.WidgetSnapshot
import com.suw1labs.worktracker.platform.WidgetSnapshots
import com.suw1labs.worktracker.ui.viewmodel.TrackerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Process-wide dependency graph. Each platform creates exactly one instance and hands it
 * the pieces that need platform APIs (database location/driver, notifications, file sharing).
 */
class AppContainer(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    val reminderScheduler: ReminderScheduler,
    val fileExporter: FileExporter,
    val widgetBridge: WidgetBridge = NoOpWidgetBridge
) {
    private val creationTracker = DatabaseCreationTracker()
    /** Process-wide background scope (database seeding, widget sync, actions queued by widgets). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase = databaseBuilder.buildAppDatabase(creationTracker)

    val repository: TimeTrackerRepository = TimeTrackerRepository(
        projectDao = database.projectDao(),
        taskDao = database.workTaskDao(),
        timeEntryDao = database.timeEntryDao(),
        attendanceDao = database.attendanceDao(),
        dayRecordDao = database.dayRecordDao(),
        settingsDao = database.settingsDao()
    )

    init {
        // Make sure the default work categories exist (fresh install or after a destructive migration).
        appScope.launch { database.seedDefaults() }
        // Keep the home-screen widget in sync with the database for as long as the process lives.
        appScope.launch { WidgetSnapshots.flow(repository).collect { widgetBridge.publish(it) } }
    }

    /** Current widget state, computed from the database (used when a widget renders itself). */
    suspend fun widgetSnapshot(): WidgetSnapshot = WidgetSnapshots.flow(repository).first()

    fun createViewModel(): TrackerViewModel = TrackerViewModel(repository, reminderScheduler, fileExporter)
}

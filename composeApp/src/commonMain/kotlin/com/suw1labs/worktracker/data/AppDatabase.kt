package com.suw1labs.worktracker.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.execSQL
import androidx.sqlite.SQLiteConnection
import com.suw1labs.worktracker.data.dao.AttendanceDao
import com.suw1labs.worktracker.data.dao.DayRecordDao
import com.suw1labs.worktracker.data.dao.SettingsDao
import com.suw1labs.worktracker.data.dao.ProjectDao
import com.suw1labs.worktracker.data.dao.TimeEntryDao
import com.suw1labs.worktracker.data.dao.WorkTaskDao
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.concurrent.Volatile

const val DATABASE_NAME = "work_tracker.db"

@Database(
    entities = [Project::class, WorkTask::class, TimeEntry::class, AttendanceSession::class, DayRecord::class, AppSettings::class],
    version = 7,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun workTaskDao(): WorkTaskDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun settingsDao(): SettingsDao

    /**
     * Creates the default settings row and the built-in "Unproductive" project with its tasks
     * (meeting, coffee break, …) when they do not exist yet. Real projects are entered from the
     * list provided by project management.
     */
    suspend fun seedDefaults() {
        if (settingsDao().getSettings() == null) settingsDao().upsert(AppSettings())
        val projects = projectDao()
        if (projects.findByCode(Project.UNPRODUCTIVE_CODE) == null) {
            val id = projects.insertProject(
                Project(
                    code = Project.UNPRODUCTIVE_CODE,
                    name = "Unproductive",
                    client = "",
                    colorHex = "#F59E0B",
                    isProductive = false
                )
            )
            DEFAULT_UNPRODUCTIVE_TASKS.forEach { title ->
                workTaskDao().insertTask(WorkTask(projectId = id, title = title, priority = "LOW", reminderEnabled = false))
            }
        }
    }

    companion object {
        val DEFAULT_UNPRODUCTIVE_TASKS: List<String> = listOf(
            "Meeting", "Coffee / Smoke break", "Informal meeting", "Uncategorized"
        )
    }
}

// The Room compiler generates the `actual` implementations for every target.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Records whether the database file was created from scratch, so the caller can
 * seed demo data exactly once (mirrors the old Android `RoomDatabase.Callback.onCreate`).
 */
class DatabaseCreationTracker : RoomDatabase.Callback() {
    @Volatile
    var wasCreated: Boolean = false
        private set

    override fun onCreate(connection: SQLiteConnection) {
        super.onCreate(connection)
        wasCreated = true
    }
}

/**
 * Finishes configuring a platform-provided builder (which already has its SQLite driver set)
 * with the options shared by all platforms.
 */
fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(
    creationTracker: DatabaseCreationTracker,
    queryDispatcher: CoroutineDispatcher = Dispatchers.IO
): AppDatabase = addCallback(creationTracker)
    .addMigrations(MIGRATION_6_7)
    // Schema v2 replaced the billing-oriented model; the old demo data is not worth migrating.
    // Newer schema changes must add a migration above so real data survives an update.
    .fallbackToDestructiveMigration(dropAllTables = true)
    .setQueryCoroutineContext(queryDispatcher)
    .build()

/** v7: paid short breaks per day (company rule) on the settings row. */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN paidBreakMinutes INTEGER NOT NULL DEFAULT 0")
    }
}

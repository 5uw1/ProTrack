package com.suw1labs.worktracker.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import com.suw1labs.worktracker.data.dao.AttendanceDao
import com.suw1labs.worktracker.data.dao.DayRecordDao
import com.suw1labs.worktracker.data.dao.SettingsDao
import com.suw1labs.worktracker.data.dao.ProjectDao
import com.suw1labs.worktracker.data.dao.TimeEntryDao
import com.suw1labs.worktracker.data.dao.WorkCategoryDao
import com.suw1labs.worktracker.data.dao.WorkTaskDao
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkCategory
import com.suw1labs.worktracker.data.model.WorkTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.concurrent.Volatile

const val DATABASE_NAME = "work_tracker.db"

@Database(
    entities = [Project::class, WorkCategory::class, WorkTask::class, TimeEntry::class, AttendanceSession::class, DayRecord::class, AppSettings::class],
    version = 5,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun workCategoryDao(): WorkCategoryDao
    abstract fun workTaskDao(): WorkTaskDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun settingsDao(): SettingsDao

    /**
     * Inserts the default work categories when none exist yet. Projects are not seeded:
     * they are entered from the list provided by project management.
     */
    suspend fun seedDefaults() {
        if (settingsDao().getSettings() == null) settingsDao().upsert(AppSettings())
        val dao = workCategoryDao()
        if (dao.getCategoryCount() > 0) return
        DEFAULT_CATEGORIES.forEachIndexed { index, (name, productive, color) ->
            dao.insertCategory(WorkCategory(name = name, isProductive = productive, colorHex = color, sortOrder = index))
        }
    }

    companion object {
        val DEFAULT_CATEGORIES: List<Triple<String, Boolean, String>> = listOf(
            Triple("PLC", true, "#3B82F6"),
            Triple("High Level Language", true, "#8B5CF6"),
            Triple("Meeting", true, "#0F766E"),
            Triple("Coffee / Smoke break", false, "#F59E0B"),
            Triple("Informal meeting", false, "#EC4899"),
            Triple("Uncategorized", false, "#64748B")
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
    // Schema v2 replaced the billing-oriented model; the old demo data is not worth migrating.
    .fallbackToDestructiveMigration(dropAllTables = true)
    .setQueryCoroutineContext(queryDispatcher)
    .build()

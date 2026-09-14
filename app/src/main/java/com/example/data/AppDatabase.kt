package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ProjectDao
import com.example.data.dao.TimeEntryDao
import com.example.data.dao.WorkTaskDao
import com.example.data.model.Project
import com.example.data.model.TimeEntry
import com.example.data.model.WorkTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Project::class, WorkTask::class, TimeEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun workTaskDao(): WorkTaskDao
    abstract fun timeEntryDao(): TimeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "work_tracker.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed realistic data on initial database creation
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.seedInitialData()
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val pDao = projectDao()
        val tDao = workTaskDao()
        val eDao = timeEntryDao()

        if (pDao.getProjectCount() > 0) return

        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L
        val oneDay = 24 * oneHour

        // 1. Mobile App Redesign Project
        val p1Id = pDao.insertProject(
            Project(
                name = "Fintech Mobile Redesign",
                client = "Apex Capital",
                colorHex = "#3B82F6", // Blue
                hourlyRate = 85.0,
                budgetHours = 40.0,
                status = "ACTIVE"
            )
        )

        // 2. Cloud Migration Project
        val p2Id = pDao.insertProject(
            Project(
                name = "Cloud Database Migration",
                client = "NovaTech Labs",
                colorHex = "#10B981", // Emerald Green
                hourlyRate = 110.0,
                budgetHours = 25.0,
                status = "ACTIVE"
            )
        )

        // 3. Marketing Landing Page
        val p3Id = pDao.insertProject(
            Project(
                name = "Q4 Product Showcase",
                client = "Solaris Media",
                colorHex = "#8B5CF6", // Violet
                hourlyRate = 70.0,
                budgetHours = 15.0,
                status = "ACTIVE"
            )
        )

        // Tasks with realistic deadlines
        val t1Id = tDao.insertTask(
            WorkTask(
                projectId = p1Id,
                title = "Design User Authentication & KYC flow",
                description = "Figma high-fidelity prototypes for biometrics & OTP verification",
                priority = "HIGH",
                status = "IN_PROGRESS",
                estimatedHours = 12.0,
                deadlineTimestamp = now + (oneHour * 4), // 4 hours from now - URGENT alert!
                reminderLeadHours = 24,
                reminderEnabled = true
            )
        )

        val t2Id = tDao.insertTask(
            WorkTask(
                projectId = p1Id,
                title = "Wireframe Portfolio Dashboard Widgets",
                description = "Account balance cards, asset allocation chart, recent activity list",
                priority = "MEDIUM",
                status = "TODO",
                estimatedHours = 8.0,
                deadlineTimestamp = now + (oneDay * 2), // 2 days from now
                reminderLeadHours = 24,
                reminderEnabled = true
            )
        )

        val t3Id = tDao.insertTask(
            WorkTask(
                projectId = p2Id,
                title = "PostgreSQL Schema Partitioning & Indexing",
                description = "Optimize transaction history queries and time-series telemetry partition",
                priority = "URGENT",
                status = "IN_PROGRESS",
                estimatedHours = 14.0,
                deadlineTimestamp = now + (oneDay * 1), // tomorrow
                reminderLeadHours = 24,
                reminderEnabled = true
            )
        )

        val t4Id = tDao.insertTask(
            WorkTask(
                projectId = p3Id,
                title = "SEO Audit & Mobile Responsive Layouts",
                description = "Core Web Vitals tuning and metadata OpenGraph tags",
                priority = "LOW",
                status = "DONE",
                estimatedHours = 5.0,
                deadlineTimestamp = now - oneDay, // finished yesterday
                reminderLeadHours = 24,
                reminderEnabled = false
            )
        )

        // Historical time entries to make productivity charts rich immediately
        eDao.insertEntry(
            TimeEntry(
                projectId = p1Id,
                taskId = t1Id,
                description = "Initial wireframes and visual design system exploration",
                startTime = now - (oneDay * 2) - (3 * oneHour),
                endTime = now - (oneDay * 2),
                durationSeconds = 3 * 3600L,
                isBillable = true,
                hourlyRate = 85.0,
                tags = "Design, UI/UX"
            )
        )

        eDao.insertEntry(
            TimeEntry(
                projectId = p2Id,
                taskId = t3Id,
                description = "Benchmarking query execution plans on partition indexes",
                startTime = now - oneDay - (4 * oneHour),
                endTime = now - oneDay,
                durationSeconds = 4 * 3600L,
                isBillable = true,
                hourlyRate = 110.0,
                tags = "Database, Performance"
            )
        )

        eDao.insertEntry(
            TimeEntry(
                projectId = p3Id,
                taskId = t4Id,
                description = "Lighthouse performance testing and image optimization",
                startTime = now - (oneDay * 3) - (2 * oneHour),
                endTime = now - (oneDay * 3),
                durationSeconds = 2 * 3600L,
                isBillable = true,
                hourlyRate = 70.0,
                tags = "SEO, Dev"
            )
        )

        eDao.insertEntry(
            TimeEntry(
                projectId = p1Id,
                taskId = t1Id,
                description = "Client design review call & biometric animation feedback",
                startTime = now - (oneHour * 5),
                endTime = now - (oneHour * 3),
                durationSeconds = 2 * 3600L,
                isBillable = true,
                hourlyRate = 85.0,
                tags = "Meeting, Client"
            )
        )
    }
}

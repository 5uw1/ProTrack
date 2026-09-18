package com.suw1labs.worktracker

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.suw1labs.worktracker.data.ALL_MIGRATIONS
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DATABASE_VERSION
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Replays the migration chain against the exported schema JSON files in `composeApp/schemas`.
 * Fails when a version bump has no migration or when a migration produces a schema that differs
 * from what the entities declare – i.e. exactly the cases that would crash or wipe a user's data.
 */
class MigrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun helper(): MigrationTestHelper = MigrationTestHelper(
        schemaDirectoryPath = Path("schemas"),
        databasePath = temporaryFolder.newFolder().toPath().resolve("migration_test.db"),
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class
    )

    /** First version whose data is migrated instead of recreated (see `buildAppDatabase`). */
    private val firstMigratedVersion = 6

    @Test
    fun migrationChain_isContinuousUpToTheCurrentVersion() {
        val steps = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }.sortedBy { it.first }
        val expected = (firstMigratedVersion until DATABASE_VERSION).map { it to it + 1 }
        assertEquals(expected, steps, "every version bump needs a Migration in ALL_MIGRATIONS")
    }

    @Test
    fun migrateFromOldestSupportedVersion_keepsDataAndMatchesEntities() {
        val helper = helper()
        helper.createDatabase(firstMigratedVersion).use { connection ->
            connection.execSQL("INSERT INTO app_settings (id, fullTimeWeeklyHours, weekdayHours, maxWeeklyHours, language) VALUES (1, 42.0, '8,8,8,8,8,0,0', 45.0, 'de')")
            connection.execSQL("INSERT INTO projects (id, code, name, client, colorHex, budgetHours, status, isProductive, createdAt) VALUES (1, 'P-1', 'One', 'ACME', '#3B82F6', 10.0, 'ACTIVE', 1, 5)")
            connection.execSQL("INSERT INTO tasks (id, projectId, title, description, priority, status, estimatedHours, deadlineTimestamp, reminderLeadHours, reminderEnabled, createdAt) VALUES (2, 1, 'PLC', '', 'HIGH', 'TODO', 4.0, NULL, 24, 1, 6)")
            connection.execSQL("INSERT INTO time_entries (id, projectId, taskId, description, startTime, endTime, createdAt) VALUES (3, 1, 2, 'wiring', 100, NULL, 7)")
            connection.execSQL("INSERT INTO attendance_sessions (id, clockIn, clockOut, clockOutReason, note, createdAt) VALUES (4, 100, NULL, NULL, '', 8)")
        }

        helper.runMigrationsAndValidate(DATABASE_VERSION, ALL_MIGRATIONS.toList()).use { connection ->
            // v7 column arrives with its default, everything else is untouched.
            assertEquals(listOf("de", "0"), connection.row("SELECT language, paidBreakMinutes FROM app_settings WHERE id = 1"))
            // v9 columns arrive with their defaults.
            assertEquals(listOf("5:30,9:60", "1"), connection.row("SELECT breakRules, deductMissingBreak FROM app_settings WHERE id = 1"))
            // v10: projects start unfocused.
            assertEquals(listOf("One", "0"), connection.row("SELECT name, isFocused FROM projects WHERE id = 1"))
            // v11: SAP defaults.
            assertEquals(listOf("SERTCN", "UNPROD", "700411"), connection.row("SELECT sapProductiveType, sapUnproductiveType, sapUnproductiveNumber FROM app_settings WHERE id = 1"))
            assertEquals(listOf("wiring", "2"), connection.row("SELECT description, taskId FROM time_entries WHERE id = 3"))
            assertEquals(listOf("100"), connection.row("SELECT clockIn FROM attendance_sessions WHERE clockOut IS NULL"))
            // v8 indexes exist under the names Room expects.
            val indexes = connection.column("SELECT name FROM sqlite_master WHERE type = 'index' ORDER BY name")
            assertTrue("index_time_entries_endTime" in indexes, indexes.toString())
            assertTrue("index_attendance_sessions_clockOut" in indexes, indexes.toString())
        }
    }

    @Test
    fun everyIntermediateVersion_migratesToTheCurrentSchema() {
        for (start in firstMigratedVersion until DATABASE_VERSION) {
            val helper = helper()
            helper.createDatabase(start).close()
            helper.runMigrationsAndValidate(DATABASE_VERSION, ALL_MIGRATIONS.toList()).close()
        }
    }

    private inline fun <R> SQLiteConnection.use(block: (SQLiteConnection) -> R): R = try { block(this) } finally { close() }
    private inline fun <R> SQLiteStatement.use(block: (SQLiteStatement) -> R): R = try { block(this) } finally { close() }

    private fun SQLiteConnection.row(sql: String): List<String?> = prepare(sql).use { statement ->
        assertTrue(statement.step(), "no row for: $sql")
        (0 until statement.getColumnCount()).map { if (statement.isNull(it)) null else statement.getText(it) }
    }

    private fun SQLiteConnection.column(sql: String): List<String> = prepare(sql).use { statement ->
        buildList { while (statement.step()) add(statement.getText(0)) }
    }
}

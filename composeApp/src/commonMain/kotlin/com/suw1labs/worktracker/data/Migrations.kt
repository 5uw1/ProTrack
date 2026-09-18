package com.suw1labs.worktracker.data

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Schema history. Every version bump gets one `Migration(from, to)` here so the data on a user's
 * device survives an update; `MigrationTest` replays the chain against the exported schema JSON.
 *
 * Guidelines for writing one:
 * - Only use `ALTER TABLE … ADD COLUMN` (with a DEFAULT for NOT NULL columns), `CREATE INDEX` or
 *   the "create new table, copy, drop, rename" recipe – SQLite cannot drop or retype columns in place.
 * - Table and index names must match what Room generates (`index_<table>_<column>`), otherwise the
 *   schema validation on open fails.
 */
val ALL_MIGRATIONS: Array<Migration> get() = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)

/** v7: paid short breaks per day (company rule) on the settings row. */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN paidBreakMinutes INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v8: indexes for the "what is running right now" look-ups (`endTime IS NULL`, `clockOut IS NULL`)
 * that the timer screen and the home-screen widgets evaluate on every change.
 */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_time_entries_endTime` ON `time_entries` (`endTime`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_sessions_clockOut` ON `attendance_sessions` (`clockOut`)")
    }
}

/** v9: adjustable break rules and the automatic deduction of a missing break, on the settings row. */
val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN breakRules TEXT NOT NULL DEFAULT '5:30,9:60'")
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN deductMissingBreak INTEGER NOT NULL DEFAULT 1")
    }
}

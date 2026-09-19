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
val ALL_MIGRATIONS: Array<Migration> get() = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)

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

/** v10: projects can be pinned as "focus" so the Today tab shows only those by default. */
val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE projects ADD COLUMN isFocused INTEGER NOT NULL DEFAULT 0")
    }
}

/** v11: SAP activity types and the unproductive cost object for the weekly paste export. */
val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN sapProductiveType TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN sapUnproductiveType TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN sapUnproductiveNumber TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v12: what a record needs to survive being merged with the same record from another device.
 *
 * `uid` is its identity everywhere (row ids are per database, so two devices hand out the same
 * ones), `updatedAt` decides which version of it wins, `deletedAt` tells other devices it is gone
 * instead of it simply being missing, and `deviceId` breaks ties. Existing rows get a random uid
 * and keep their creation time as the first `updatedAt`.
 */
val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    private val tables = listOf("projects", "tasks", "time_entries", "attendance_sessions", "day_records")

    override fun migrate(connection: SQLiteConnection) {
        tables.forEach { table ->
            connection.execSQL("ALTER TABLE $table ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
            connection.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE $table ADD COLUMN deletedAt INTEGER")
            connection.execSQL("ALTER TABLE $table ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
            // 26 characters of hex in the ULID alphabet: not sortable by time like a generated one,
            // but unique, which is all a row that already exists needs.
            connection.execSQL("UPDATE $table SET uid = upper(hex(randomblob(13))) WHERE uid = ''")
            connection.execSQL("UPDATE $table SET updatedAt = createdAt WHERE updatedAt = 0")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uid ON $table(uid)")
        }
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE app_settings ADD COLUMN syncClock INTEGER NOT NULL DEFAULT 0")
    }
}

package com.suw1labs.worktracker.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * Android database builder. Uses the framework SQLite driver so the same database file
 * (`work_tracker.db`) created by the previous Android-only version keeps working, and so
 * Robolectric host tests can open it without native bundled SQLite.
 */
fun androidDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath
    ).setDriver(AndroidSQLiteDriver())
}

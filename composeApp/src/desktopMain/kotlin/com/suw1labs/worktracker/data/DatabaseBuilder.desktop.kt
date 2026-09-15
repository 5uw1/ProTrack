package com.suw1labs.worktracker.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

/**
 * Stores the database in a per-user application data folder:
 * `%APPDATA%\WorkTracker` on Windows, `~/Library/Application Support/WorkTracker` on macOS,
 * `~/.local/share/WorkTracker` elsewhere.
 */
fun desktopDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(appDataDirectory(), DATABASE_NAME)
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
}

private fun appDataDirectory(): File {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    val base = when {
        os.contains("win") -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
        os.contains("mac") -> File(home, "Library/Application Support")
        else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(home, ".local/share")
    }
    return File(base, "WorkTracker")
}

package com.suw1labs.worktracker

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Number and name together identify a project; either one alone may repeat. */
class ProjectIdentityTest {

    @Test
    fun sameNumberAndName_isTheSameProject_whateverTheCaseOrSpaces() {
        assertEquals(Project.identityKey("P-100", "Retrofit"), Project.identityKey("  p-100 ", "RETROFIT  "))
    }

    @Test
    fun sameNumberOtherName_orSameNameOtherNumber_areDifferentProjects() {
        assertNotEquals(Project.identityKey("P-100", "Retrofit"), Project.identityKey("P-100", "Commissioning"))
        assertNotEquals(Project.identityKey("P-100", "Retrofit"), Project.identityKey("P-200", "Retrofit"))
    }

    @Test
    fun theKeyCannotBeFooledByMovingTextBetweenTheFields() {
        // "P-1" + "00 Retrofit" must not collide with "P-100" + " Retrofit".
        assertNotEquals(Project.identityKey("P-1", "00 Retrofit"), Project.identityKey("P-100", "Retrofit"))
    }

    @Test
    fun importSkipsOnlyExactPairs() = runTest {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .buildAppDatabase(DatabaseCreationTracker(), Dispatchers.IO)
        val repository = TimeTrackerRepository(
            db.projectDao(), db.workTaskDao(), db.timeEntryDao(), db.attendanceDao(), db.dayRecordDao(), db.settingsDao()
        )
        repository.insertProject(Project(code = "P-100", name = "Retrofit"))

        val added = repository.importProjects(
            listOf(
                Project(code = "p-100", name = "retrofit"),        // same pair → skipped
                Project(code = "P-100", name = "Commissioning"),   // same number, other name → new
                Project(code = "P-200", name = "Retrofit"),        // same name, other number → new
                Project(code = "P-200", name = "Retrofit"),        // repeated inside the paste → skipped
            ),
            repository.allProjects.first(),
        )

        assertEquals(2, added)
        val pairs = repository.allProjects.first().map { it.code to it.name }.toSet()
        assertEquals(setOf("P-100" to "Retrofit", "P-100" to "Commissioning", "P-200" to "Retrofit"), pairs)
        db.close()
    }
}

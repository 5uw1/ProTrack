package com.suw1labs.worktracker

import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.demoClockOffsetMillis

/**
 * Flags a platform entry point can pass on launch. Used for store screenshots and demos:
 * `demo` fills the database with sample data, `initialTab` opens a tab directly and `language`
 * sets the UI language. Platforms only honour them in debug / simulator builds.
 */
data class AppLaunchOptions(
    val demo: Boolean = false,
    val initialTab: TrackerDestination? = null,
    val language: String? = null,
    /** "HH:MM": the app's clock is shifted so that it is this time today (keeps screenshots and status bars consistent). */
    val time: String? = null
) {
    /** Applies [time] to the shared clock; call once before the UI or demo data is created. */
    fun applyClock() {
        val (h, m) = time?.split(':')?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 2 } ?: return
        val real = currentTimeMillis() - demoClockOffsetMillis
        demoClockOffsetMillis = DateRanges.dayRange(real).start + h * 3600_000L + m * 60_000L - real
    }

    companion object {
        val NONE = AppLaunchOptions()

        fun of(demo: Boolean, tab: String?, language: String?, time: String? = null) = AppLaunchOptions(
            demo = demo,
            initialTab = tab?.let { TrackerDestination.fromLaunchName(it) },
            language = language?.takeIf { it.isNotBlank() },
            time = time?.takeIf { it.isNotBlank() }
        )

        /** iOS-style arguments: `-demo -tab reports -lang de -time 15:20`. */
        fun fromArguments(args: List<String>): AppLaunchOptions {
            fun value(flag: String): String? = args.indexOf(flag).takeIf { it >= 0 && it + 1 < args.size }?.let { args[it + 1] }
            return of(demo = "-demo" in args, tab = value("-tab"), language = value("-lang"), time = value("-time"))
        }
    }
}

package com.suw1labs.worktracker.util

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * Multiplatform replacement for `String.format(Locale.US, "%.Nf", value)`.
 * Uses HALF_UP rounding and always renders with a '.' decimal separator.
 */
fun Double.formatFixed(decimals: Int): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "Infinity" else "-Infinity"

    var factor = 1L
    repeat(decimals) { factor *= 10 }

    val scaled = floor(abs(this) * factor + 0.5).toLong()
    val intPart = scaled / factor
    val fracPart = scaled % factor
    val negative = this < 0 && scaled != 0L

    val sb = StringBuilder()
    if (negative) sb.append('-')
    sb.append(intPart)
    if (decimals > 0) {
        sb.append('.')
        sb.append(fracPart.toString().padStart(decimals, '0'))
    }
    return sb.toString()
}

/** Multiplatform replacement for `String.format("%02d", value)`. */
fun Long.padTwo(): String = toString().padStart(2, '0')

/** Duration helpers shared by the UI and the SAP export. */
object TimeFormat {
    /** `HH:MM:SS` */
    fun hms(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "${hours.padTwo()}:${minutes.padTwo()}:${seconds.padTwo()}"
    }

    /** `H:MM` e.g. "6:05" */
    fun hm(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "$hours:${minutes.padTwo()}"
    }

    /** `6h 05m` */
    fun hoursMinutes(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes.padTwo()}m"
    }

    /** Exact decimal hours, e.g. 5400s -> 1.5 */
    fun decimalHours(totalSeconds: Long): Double = totalSeconds / 3600.0

    /** Decimal hours rounded to the nearest quarter hour (SAP style: 1h30 -> 1.5). */
    fun quarterHours(totalSeconds: Long): Double = round(totalSeconds / 900.0) * 0.25

    /** Formats decimal hours the way they are typed into SAP, e.g. "1.5" or "0.25". */
    fun sapHours(hours: Double): String {
        val text = hours.formatFixed(2)
        return text.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }
}

package com.suw1labs.worktracker.data.sync

import com.suw1labs.worktracker.util.currentTimeMillis
import kotlin.random.Random

/**
 * Identifier a record keeps for its whole life, on every device.
 *
 * Row ids are per database: two devices working offline both hand out id 42, and a merge would
 * have no way to tell the two records apart. A ULID is generated where the record is created and
 * never changes – 48 bits of millisecond timestamp so ids sort in creation order, 80 bits of
 * randomness so two devices cannot produce the same one.
 *
 * Crockford's base32 (no I, L, O, U), 26 characters, e.g. `01K5R8Q2T4ZB9V3XKD7FN1MC2E`.
 */
object Ulid {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun generate(now: Long = currentTimeMillis(), random: Random = Random.Default): String {
        val time = StringBuilder()
        var remaining = now
        repeat(10) {
            time.append(ALPHABET[(remaining and 0x1F).toInt()])
            remaining = remaining shr 5
        }
        val randomness = StringBuilder()
        repeat(16) { randomness.append(ALPHABET[random.nextInt(32)]) }
        return time.reverse().toString() + randomness
    }
}

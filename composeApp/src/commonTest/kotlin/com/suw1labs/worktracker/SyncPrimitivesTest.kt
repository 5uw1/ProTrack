package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.sync.SyncClock
import com.suw1labs.worktracker.data.sync.Ulid
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a merge will rely on: ids that never collide and timestamps that never go backwards. */
class SyncPrimitivesTest {

    @Test
    fun ulid_isSortableByTimeAndUniqueWithinTheSameMillisecond() {
        val early = Ulid.generate(now = 1_700_000_000_000)
        val late = Ulid.generate(now = 1_700_000_001_000)
        assertTrue(early < late, "$early should sort before $late")

        val sameMillis = List(1000) { Ulid.generate(now = 1_700_000_000_000) }
        assertEquals(1000, sameMillis.toSet().size, "ids generated in the same millisecond collided")
        assertTrue(sameMillis.all { it.length == 26 }, "a ULID is 26 characters")
    }

    @Test
    fun ulid_usesCrockfordAlphabetOnly() {
        val allowed = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toSet()
        repeat(200) {
            val id = Ulid.generate(random = Random(it))
            assertTrue(id.all { c -> c in allowed }, "unexpected character in $id")
        }
    }

    @Test
    fun clock_neverRepeatsAndNeverGoesBackwards() {
        val clock = SyncClock()
        val stamps = List(5000) { clock.now() }
        assertEquals(stamps, stamps.sorted(), "timestamps came out of order")
        assertEquals(stamps.size, stamps.toSet().size, "a timestamp was handed out twice")
    }

    @Test
    fun clock_movesPastWhatOtherDevicesReported() {
        val clock = SyncClock()
        val fromTheFuture = clock.now() + 60 * 60 * 1000
        clock.observe(fromTheFuture)
        assertTrue(clock.now() > fromTheFuture, "an edit after the merge must sort after it")
    }

    @Test
    fun clock_ignoresOlderTimestampsFromOtherDevices() {
        val clock = SyncClock()
        val local = clock.now()
        clock.observe(local - 60 * 60 * 1000)
        assertTrue(clock.now() > local, "a slow device must not pull the clock back")
    }

    @Test
    fun clock_resumesWhereItLeftOff() {
        val restored = SyncClock(initial = Long.MAX_VALUE / 2)
        assertTrue(restored.now() > Long.MAX_VALUE / 2, "the stored value has to be the floor")
    }
}

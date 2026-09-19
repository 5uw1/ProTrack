package com.suw1labs.worktracker.data.sync

import com.suw1labs.worktracker.util.currentTimeMillis
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Timestamps a merge can order records by, even when the devices disagree about the time.
 *
 * Merging takes the newest version of a record, so a device whose clock runs three hours fast
 * would win every conflict and a device that just crossed a daylight-saving boundary could
 * overwrite newer work with older. This is the logical half of a hybrid logical clock: it hands
 * out the wall clock when that moves forward, and otherwise the last value plus one, so the
 * sequence never goes backwards and never repeats within a device.
 *
 * [observe] pulls the clock forward past timestamps seen from other devices, which is what keeps
 * edits made right after a sync ordered after the edits they answer.
 */
@OptIn(ExperimentalAtomicApi::class)
class SyncClock(initial: Long = 0) {
    private val last = AtomicLong(initial)

    /** The next timestamp: wall clock if it moved on, otherwise one tick past the last one. */
    fun now(): Long {
        while (true) {
            val previous = last.load()
            val next = maxOf(currentTimeMillis(), previous + 1)
            if (last.compareAndSet(previous, next)) return next
        }
    }

    /** Takes note of a timestamp from another device so later local edits sort after it. */
    fun observe(remote: Long) {
        while (true) {
            val previous = last.load()
            if (remote <= previous) return
            if (last.compareAndSet(previous, remote)) return
        }
    }

    val lastIssued: Long get() = last.load()
}

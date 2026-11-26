package com.suw1labs.worktracker

import com.suw1labs.worktracker.util.SemVer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {
    private fun v(text: String) = SemVer.parse(text) ?: error("not a version: $text")

    @Test
    fun releaseCandidates_pastNine_sortNumerically() {
        assertTrue(v("1.1.0-rc.10") > v("1.1.0-rc.9"))
        assertTrue(v("1.1.0-rc.100") > v("1.1.0-rc.99"))
        assertTrue(v("1.1.0-rc.2") < v("1.1.0-rc.10"))
    }

    @Test
    fun legacyTagsWithoutTheDot_sortWithTheNewOnes() {
        // v1.1.0-rc1 … rc4 were tagged before the switch to rc.N.
        assertTrue(v("1.1.0-rc4") < v("1.1.0-rc.5"))
        assertTrue(v("1.1.0-rc10") > v("1.1.0-rc9"))
        assertEquals(v("1.1.0-rc.4"), v("1.1.0-rc4"))
    }

    @Test
    fun aRelease_isAboveItsPreReleases_andBelowTheNextPatch() {
        assertTrue(v("1.1.0") > v("1.1.0-rc.10"))
        assertTrue(v("1.1.1-rc.1") > v("1.1.0"))
        assertTrue(v("1.0.10") > v("1.0.9"))
        assertTrue(v("2.0.0") > v("1.99.99"))
    }

    @Test
    fun semverSpecExample_isInOrder() {
        // The precedence example of semver.org §11.
        val ordered = listOf(
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2",
            "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0"
        )
        assertEquals(ordered, ordered.shuffled().map(::v).sorted().map { it.toString() })
    }

    @Test
    fun theWholeTagHistory_sortsInTheOrderItWasTagged() {
        val tags = listOf("v1.0.9", "v1.0.10", "v1.0.19", "v1.1.0-rc1", "v1.1.0-rc2", "v1.1.0-rc4", "v1.1.0-rc.5", "v1.1.0-rc.9", "v1.1.0-rc.10", "v1.1.0", "v1.1.1")
        assertEquals(tags, tags.reversed().sortedBy(::v))
    }

    @Test
    fun parsing_acceptsTagSpellings_andRejectsJunk() {
        assertEquals("1.2.0", v("v1.2").toString())
        assertEquals("1.2.3-rc.4", v("v1.2.3-rc.4+build.7").toString())
        assertTrue(v("1.2.3-rc.1").isPreRelease)
        assertNull(SemVer.parse("latest"))
        assertNull(SemVer.parse("1.2.3-"))
        assertNull(SemVer.parse(""))
    }
}

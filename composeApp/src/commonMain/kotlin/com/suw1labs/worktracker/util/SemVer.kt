package com.suw1labs.worktracker.util

/**
 * A version as the release tags spell it ("1.2.3", "v1.2.3-rc.10"), ordered by Semantic Versioning
 * 2.0 precedence: numbers compare numerically, a pre-release sorts before its release
 * (1.2.3-rc.1 < 1.2.3), pre-release identifiers compare one by one – numeric ones as numbers,
 * so rc.9 < rc.10 – and build metadata ("+abc") is ignored.
 *
 * Tags before 1.1.0-rc.5 wrote the number straight after the letters ("rc4"). Strict SemVer
 * compares that as text, so rc10 would sort before rc9, and the old rc4 after a new rc.5. An
 * identifier of letters followed by digits is therefore split ("rc4" → "rc", 4), which makes "rc4"
 * and "rc.4" the same version and keeps the old tags in line with the new ones.
 */
class SemVer private constructor(
    val major: Int,
    val minor: Int,
    val patch: Int,
    /** Pre-release identifiers, each an Int or a String; empty for a release. */
    private val preRelease: List<Comparable<*>>,
) : Comparable<SemVer> {

    val isPreRelease: Boolean get() = preRelease.isNotEmpty()

    override fun compareTo(other: SemVer): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        // A release is higher than any of its pre-releases.
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1
        for (i in 0 until minOf(preRelease.size, other.preRelease.size)) {
            val a = preRelease[i]
            val b = other.preRelease[i]
            val c = when {
                a is Int && b is Int -> a.compareTo(b)
                a is Int -> -1              // numeric identifiers sort before alphanumeric ones
                b is Int -> 1
                else -> (a as String).compareTo(b as String)
            }
            if (c != 0) return c
        }
        // All shared identifiers equal: the longer list is higher (rc < rc.1).
        return preRelease.size.compareTo(other.preRelease.size)
    }

    override fun equals(other: Any?): Boolean = other is SemVer && compareTo(other) == 0
    override fun hashCode(): Int = listOf(major, minor, patch, preRelease).hashCode()
    override fun toString(): String =
        "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else "-" + preRelease.joinToString(".")

    companion object {
        private val CORE = Regex("""^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")
        private val LETTERS_THEN_DIGITS = Regex("""^([A-Za-z-]+)(\d+)$""")

        /** Parses "1.2.3", "v1.2", "1.2.3-rc.4", "1.2.3-rc4", "1.2.3+build"; null when it is none of these. */
        fun parse(text: String): SemVer? {
            val m = CORE.matchEntire(text.trim()) ?: return null
            val (major, minor, patch, pre) = m.destructured
            val identifiers = mutableListOf<Comparable<*>>()
            for (id in pre.split('.').filter { it.isNotEmpty() }) {
                val split = LETTERS_THEN_DIGITS.matchEntire(id)
                when {
                    id.all { it.isDigit() } -> identifiers += id.toIntOrNull() ?: return null
                    split != null -> {
                        identifiers += split.groupValues[1]
                        identifiers += split.groupValues[2].toIntOrNull() ?: return null
                    }
                    else -> identifiers += id
                }
            }
            return SemVer(
                major = major.toIntOrNull() ?: return null,
                minor = minor.ifEmpty { "0" }.toIntOrNull() ?: return null,
                patch = patch.ifEmpty { "0" }.toIntOrNull() ?: return null,
                preRelease = identifiers,
            )
        }
    }
}

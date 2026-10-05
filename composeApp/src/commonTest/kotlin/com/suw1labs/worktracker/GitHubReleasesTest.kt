package com.suw1labs.worktracker

import com.suw1labs.worktracker.platform.DesktopOs
import com.suw1labs.worktracker.platform.GitHubReleases
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubReleasesTest {
    private fun release(tag: String, prerelease: Boolean = false, digest: String? = "sha256:ABCDEF") = """
        {
          "tag_name": "$tag", "html_url": "https://github.com/5uw1/ProTrack/releases/tag/$tag",
          "draft": false, "prerelease": $prerelease, "body": "notes",
          "assets": [
            {"name": "WorkTracker-${tag.removePrefix("v")}-android.apk", "browser_download_url": "https://x/apk", "size": 1},
            {"name": "WorkTracker-${tag.removePrefix("v")}-macos.dmg", "browser_download_url": "https://x/dmg", "size": 2${digest?.let { ", \"digest\": \"$it\"" } ?: ""}},
            {"name": "WorkTracker-${tag.removePrefix("v")}-windows.exe", "browser_download_url": "https://x/exe", "size": 3},
            {"name": "WorkTracker-${tag.removePrefix("v")}-windows.msi", "browser_download_url": "https://x/msi", "size": 4},
            {"name": "WorkTracker-${tag.removePrefix("v")}-linux.deb", "browser_download_url": "https://x/deb", "size": 5}
          ]
        }
    """.trimIndent()

    @Test
    fun versions_compareNumerically() {
        assertTrue(GitHubReleases.isNewer("1.0.10", "1.0.9"))
        assertTrue(GitHubReleases.isNewer("v1.1", "1.0.15"))
        assertTrue(GitHubReleases.isNewer("2.0.0", "1.99.99"))
        assertFalse(GitHubReleases.isNewer("1.0.15", "1.0.15"))
        assertFalse(GitHubReleases.isNewer("1.0.14", "1.0.15"))
        assertFalse(GitHubReleases.isNewer("1.0.15-rc1", "1.0.15"))
    }

    @Test
    fun newerRelease_offersThisComputersInstaller() {
        val mac = GitHubReleases.updateFrom(release("v1.0.16"), DesktopOs.MAC, "1.0.15")!!
        assertEquals("1.0.16", mac.version)
        assertEquals("WorkTracker-1.0.16-macos.dmg", mac.assetName)
        assertEquals("abcdef", mac.sha256)
        assertEquals("https://x/msi", GitHubReleases.updateFrom(release("v1.0.16"), DesktopOs.WINDOWS, "1.0.15")!!.assetUrl)
        assertEquals("https://x/deb", GitHubReleases.updateFrom(release("v1.0.16"), DesktopOs.LINUX, "1.0.15")!!.assetUrl)
        assertNull(GitHubReleases.updateFrom(release("v1.0.16", digest = null), DesktopOs.MAC, "1.0.15")!!.sha256)
    }

    @Test
    fun noUpdate_forTheSameOrOlderVersion_orAPreRelease() {
        assertNull(GitHubReleases.updateFrom(release("v1.0.15"), DesktopOs.MAC, "1.0.15"))
        assertNull(GitHubReleases.updateFrom(release("v1.0.14"), DesktopOs.MAC, "1.0.15"))
        assertNull(GitHubReleases.updateFrom(release("v1.0.16-rc1", prerelease = true), DesktopOs.MAC, "1.0.15"))
    }

    @Test
    fun osNames_mapToInstallers() {
        assertEquals(DesktopOs.MAC, DesktopOs.fromOsName("Mac OS X"))
        assertEquals(DesktopOs.WINDOWS, DesktopOs.fromOsName("Windows 11"))
        assertEquals(DesktopOs.LINUX, DesktopOs.fromOsName("Linux"))
        assertNull(DesktopOs.fromOsName("SunOS"))
    }
}

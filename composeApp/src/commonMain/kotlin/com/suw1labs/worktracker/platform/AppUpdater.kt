package com.suw1labs.worktracker.platform

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Updates of the desktop app from the project's GitHub Releases: every `vX.Y.Z` tag publishes the
 * installers there (see .github/workflows/build.yml), so the newest release is the update.
 * Phones get theirs from the stores and use [NoOpAppUpdater].
 */
interface AppUpdater {
    /** False where the app cannot update itself (phones, a desktop build run from the IDE). */
    val supported: Boolean
    /** The running version, e.g. "1.0.15"; null when not known. */
    val currentVersion: String?
    val state: StateFlow<UpdateState>

    /** Asks GitHub for the latest release (in the background). */
    fun check()

    /** Downloads the update, checks it against the release's SHA-256 and installs it; the app restarts. */
    fun install()
}

object NoOpAppUpdater : AppUpdater {
    override val supported = false
    override val currentVersion: String? = null
    override val state: StateFlow<UpdateState> = MutableStateFlow(UpdateState.Idle)
    override fun check() = Unit
    override fun install() = Unit
}

/** The updater of this app, for the screens that show update state (Settings, the launch notice). */
val LocalAppUpdater = staticCompositionLocalOf<AppUpdater> { NoOpAppUpdater }

/** A release that is newer than the running app, with the installer for this computer. */
data class ReleaseInfo(
    val version: String,
    val pageUrl: String,
    val assetName: String,
    val assetUrl: String,
    val size: Long,
    /** Hex SHA-256 of the installer as GitHub reports it; null when the release has none. */
    val sha256: String?,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    /** [progress] from 0 to 1. */
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    /** The installer runs; the app quits and comes back as the new version. */
    data class Installing(val release: ReleaseInfo) : UpdateState
    /** The installer was opened for the user to finish (no permission to replace the app, or Linux). */
    data class Manual(val release: ReleaseInfo) : UpdateState
    data class Failed(val reason: String, val release: ReleaseInfo?) : UpdateState
}

enum class DesktopOs(val assetSuffix: String) {
    MAC("-macos.dmg"),
    WINDOWS("-windows.msi"),
    LINUX("-linux.deb");

    companion object {
        fun fromOsName(name: String): DesktopOs? {
            val n = name.lowercase()
            return when {
                "mac" in n || "darwin" in n -> MAC
                "win" in n -> WINDOWS
                "nux" in n || "nix" in n -> LINUX
                else -> null
            }
        }
    }
}

object GitHubReleases {
    /** Where the installers are published. */
    const val REPOSITORY = "5uw1/ProTrack"
    const val LATEST_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"

    @Serializable
    private data class Release(
        val tag_name: String,
        val html_url: String = "",
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private data class Asset(
        val name: String,
        val browser_download_url: String,
        val size: Long = 0,
        val digest: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The update in GitHub's "latest release" answer [body] for [os], or null when that release is
     * not newer than [current], is a draft or pre-release, or has no installer for [os].
     */
    fun updateFrom(body: String, os: DesktopOs, current: String): ReleaseInfo? {
        val release = json.decodeFromString(Release.serializer(), body)
        if (release.draft || release.prerelease) return null
        val version = release.tag_name.removePrefix("v")
        if (!isNewer(version, current)) return null
        val asset = release.assets.firstOrNull { it.name.endsWith(os.assetSuffix) } ?: return null
        return ReleaseInfo(
            version = version,
            pageUrl = release.html_url,
            assetName = asset.name,
            assetUrl = asset.browser_download_url,
            size = asset.size,
            sha256 = asset.digest?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase(),
        )
    }

    /** True when [candidate] is a higher version than [current] ("1.0.10" > "1.0.9"; a suffix like "-rc1" is ignored). */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

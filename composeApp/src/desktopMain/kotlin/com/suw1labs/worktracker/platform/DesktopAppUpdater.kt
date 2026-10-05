package com.suw1labs.worktracker.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

/**
 * Updates the installed desktop app from GitHub Releases:
 *
 * * macOS: the DMG is mounted and its WorkTracker.app replaces the running one, then it is opened
 *   again – done by a small script that waits for this process to quit first.
 * * Windows: the per-user MSI upgrades the installation in place (same upgradeUuid), then the app
 *   is started again.
 * * Linux, or a Mac where the app's folder is not writable: the installer is opened and the user
 *   finishes it ([UpdateState.Manual]).
 *
 * Every download is checked against the SHA-256 GitHub lists for the asset; without one, nothing is
 * installed. Only packaged builds update themselves: jpackage tells them their version and path.
 */
class DesktopAppUpdater(
    private val scope: CoroutineScope,
    /** Ends the app so the installer can replace it. */
    private val quit: () -> Unit,
    override val currentVersion: String? = System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() },
    /** The launcher of the installed app, e.g. /Applications/WorkTracker.app/Contents/MacOS/WorkTracker. */
    private val appPath: String? = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
        // The native launcher sets it; otherwise the running launcher is the installed app.
        ?: System.getProperty("jpackage.app-version")?.let { ProcessHandle.current().info().command().orElse(null) },
    private val os: DesktopOs? = DesktopOs.fromOsName(System.getProperty("os.name") ?: ""),
    private val latestUrl: String = GitHubReleases.LATEST_URL,
    private val downloadDir: File = File(System.getProperty("java.io.tmpdir"), "WorkTracker-update"),
) : AppUpdater {

    override val supported: Boolean = currentVersion != null && appPath != null && os != null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    // Created on first use, never while the app starts: updating must not be able to stop it.
    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build()
    }

    override fun check() {
        if (!supported) return
        val busy = _state.value
        if (busy is UpdateState.Checking || busy is UpdateState.Downloading || busy is UpdateState.Installing) return
        _state.value = UpdateState.Checking
        scope.launch(Dispatchers.IO) {
            _state.value = try {
                val request = HttpRequest.newBuilder(URI(latestUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WorkTracker/$currentVersion")
                    .timeout(Duration.ofSeconds(20))
                    .GET().build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() != 200) error("GitHub answered ${response.statusCode()}")
                GitHubReleases.updateFrom(response.body(), os!!, currentVersion!!)
                    ?.let { UpdateState.Available(it) }
                    ?: UpdateState.UpToDate(currentVersion)
            } catch (e: Throwable) {
                // Throwable, not Exception: a class missing from the runtime is an Error.
                UpdateState.Failed(e.message ?: e.toString(), null)
            }
        }
    }

    override fun install() {
        val release = when (val s = _state.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val file = download(release)
                when (os) {
                    DesktopOs.MAC -> installMac(release, file)
                    DesktopOs.WINDOWS -> installWindows(release, file)
                    DesktopOs.LINUX, null -> openForUser(release, file)
                }
            } catch (e: Throwable) {
                _state.value = UpdateState.Failed(e.message ?: e.toString(), release)
            }
        }
    }

    internal fun download(release: ReleaseInfo): File {
        val expected = release.sha256 ?: error("The release lists no checksum for ${release.assetName}")
        downloadDir.mkdirs()
        val target = File(downloadDir, release.assetName)
        // A finished earlier download is reused when it is still intact.
        if (target.isFile && sha256(target) == expected) return target
        _state.value = UpdateState.Downloading(release, 0f)
        val request = HttpRequest.newBuilder(URI(release.assetUrl))
            .header("User-Agent", "WorkTracker/$currentVersion")
            .GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) error("Download failed (${response.statusCode()})")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(release.size).coerceAtLeast(1)
        val partial = File(downloadDir, release.assetName + ".part")
        val digest = MessageDigest.getInstance("SHA-256")
        response.body().use { input ->
            partial.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                var lastShown = 0f
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    digest.update(buffer, 0, n)
                    done += n
                    val progress = (done.toFloat() / total).coerceAtMost(1f)
                    if (progress - lastShown >= 0.01f) { lastShown = progress; _state.value = UpdateState.Downloading(release, progress) }
                }
            }
        }
        val actual = digest.digest().toHex()
        if (actual != expected) {
            partial.delete()
            error("The download does not match its checksum")
        }
        target.delete()
        if (!partial.renameTo(target)) error("Could not keep the download")
        return target
    }

    private fun installMac(release: ReleaseInfo, dmg: File) {
        // .../WorkTracker.app/Contents/MacOS/WorkTracker -> .../WorkTracker.app
        val bundle = File(appPath!!).parentFile?.parentFile?.parentFile
        val replaceable = bundle != null && bundle.name.endsWith(".app") && bundle.parentFile?.canWrite() == true && bundle.canWrite()
        if (!replaceable) return openForUser(release, dmg)
        val script = writeScript("install-update.sh", UpdateScripts.MAC)
        _state.value = UpdateState.Installing(release)
        startDetached(listOf("/bin/bash", script.absolutePath, ProcessHandle.current().pid().toString(), dmg.absolutePath, bundle!!.absolutePath))
        quit()
    }

    private fun installWindows(release: ReleaseInfo, msi: File) {
        val script = writeScript("install-update.cmd", UpdateScripts.WINDOWS)
        _state.value = UpdateState.Installing(release)
        startDetached(listOf("cmd", "/c", "start", "\"WorkTracker update\"", "/min", script.absolutePath, ProcessHandle.current().pid().toString(), msi.absolutePath, appPath!!))
        quit()
    }

    private fun openForUser(release: ReleaseInfo, file: File) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop().open(file)
        else ProcessBuilder("xdg-open", file.absolutePath).start()
        _state.value = UpdateState.Manual(release)
    }

    private fun writeScript(name: String, text: String): File =
        File(downloadDir, name).apply { writeText(text); setExecutable(true) }

    private fun startDetached(command: List<String>) {
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(File(downloadDir, "install-update.log"))
            .start()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

/** The helper scripts that replace the app once it has quit (kept apart so a test can run them). */
object UpdateScripts {
    /**
     * `install-update.sh <pid> <dmg> <WorkTracker.app>`: waits for the app to quit, copies the new
     * app out of the DMG next to the old one, swaps them and opens it. OPEN_CMD overrides `open`
     * (tests).
     */
    val MAC = """
        #!/bin/bash
        set -u
        PID="${'$'}1"; DMG="${'$'}2"; TARGET="${'$'}3"
        OPEN="${'$'}{OPEN_CMD:-open}"
        for _ in ${'$'}(seq 1 120); do kill -0 "${'$'}PID" 2>/dev/null || break; sleep 0.5; done
        MNT="${'$'}(mktemp -d)"
        hdiutil attach -nobrowse -readonly -mountpoint "${'$'}MNT" "${'$'}DMG" || { echo "mount failed"; exit 1; }
        SRC="${'$'}(find "${'$'}MNT" -maxdepth 1 -name '*.app' | head -n 1)"
        STAGE="${'$'}{TARGET%.app}.update.app"
        OK=0
        if [ -n "${'$'}SRC" ] && rm -rf "${'$'}STAGE" && ditto "${'$'}SRC" "${'$'}STAGE"; then
          OLD="${'$'}{TARGET%.app}.old.app"
          rm -rf "${'$'}OLD"
          if mv "${'$'}TARGET" "${'$'}OLD" && mv "${'$'}STAGE" "${'$'}TARGET"; then rm -rf "${'$'}OLD"; OK=1
          else [ -d "${'$'}OLD" ] && [ ! -d "${'$'}TARGET" ] && mv "${'$'}OLD" "${'$'}TARGET"; fi
        fi
        hdiutil detach "${'$'}MNT" -quiet || hdiutil detach "${'$'}MNT" -force -quiet
        rmdir "${'$'}MNT" 2>/dev/null
        xattr -dr com.apple.quarantine "${'$'}TARGET" 2>/dev/null
        # The old version comes back if the swap failed, so the user is never left without the app.
        "${'$'}OPEN" "${'$'}TARGET"
        [ "${'$'}OK" = 1 ] && echo "updated" || { echo "update failed"; exit 1; }
    """.trimIndent() + "\n"

    /** `install-update.cmd <pid> <msi> <WorkTracker.exe>`: waits for the app to quit, upgrades it, starts it again. */
    val WINDOWS = """
        @echo off
        :wait
        tasklist /FI "PID eq %1" 2>NUL | find "%1" >NUL && (timeout /t 1 /nobreak >NUL & goto wait)
        msiexec /i "%~2" /passive /norestart
        start "" "%~3"
    """.trimIndent().replace("\n", "\r\n") + "\r\n"
}

package com.suw1labs.worktracker

import com.suw1labs.worktracker.platform.DesktopAppUpdater
import com.suw1labs.worktracker.platform.DesktopOs
import com.suw1labs.worktracker.platform.UpdateScripts
import com.suw1labs.worktracker.platform.UpdateState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The updater against a local stand-in for GitHub, and the macOS install script against a real DMG. */
class DesktopAppUpdaterTest {
    private val installer = "installer bytes".repeat(10_000).toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(installer).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    private var digestInRelease = sha
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/latest") { ex ->
            val base = "http://127.0.0.1:${address.port}"
            val body = """{"tag_name":"v1.0.16","html_url":"$base/page","assets":[
                {"name":"WorkTracker-1.0.16-macos.dmg","browser_download_url":"$base/asset","size":${installer.size},"digest":"sha256:$digestInRelease"}]}""".toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
        }
        createContext("/asset") { ex -> ex.sendResponseHeaders(200, installer.size.toLong()); ex.responseBody.use { it.write(installer) } }
        start()
    }
    private val dir = Files.createTempDirectory("update-test").toFile()

    @AfterTest
    fun stop() { server.stop(0); dir.deleteRecursively() }

    private fun updater(current: String = "1.0.15") = DesktopAppUpdater(
        scope = CoroutineScope(Dispatchers.IO),
        quit = {},
        currentVersion = current,
        appPath = "/Applications/WorkTracker.app/Contents/MacOS/WorkTracker",
        os = DesktopOs.MAC,
        latestUrl = "http://127.0.0.1:${server.address.port}/latest",
        downloadDir = dir,
    )

    private fun DesktopAppUpdater.settled(): UpdateState = runBlocking {
        withTimeout(10_000) { state.first { it !is UpdateState.Idle && it !is UpdateState.Checking } }
    }

    @Test
    fun check_findsTheNewerRelease_andTheDownloadMatchesItsChecksum() {
        val updater = updater()
        updater.check()
        val available = updater.settled() as UpdateState.Available
        assertEquals("1.0.16", available.release.version)

        val file = updater.download(available.release)
        assertTrue(file.readBytes().contentEquals(installer))
    }

    @Test
    fun check_onTheLatestVersion_saysUpToDate() {
        val updater = updater(current = "1.0.16")
        updater.check()
        assertEquals(UpdateState.UpToDate("1.0.16"), updater.settled())
    }

    @Test
    fun aDownloadThatDoesNotMatchItsChecksum_isRejected() {
        digestInRelease = "0".repeat(64)
        val updater = updater()
        updater.check()
        val available = updater.settled() as UpdateState.Available
        assertFailsWith<IllegalStateException> { updater.download(available.release) }
        assertTrue(dir.listFiles().orEmpty().none { it.name.endsWith(".dmg") }, "nothing kept to install")
    }

    @Test
    fun macScript_swapsTheAppForTheOneInTheDmg() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        // Installed "old" app, and a DMG with the "new" one.
        val apps = File(dir, "Applications").apply { mkdirs() }
        val target = File(apps, "WorkTracker.app")
        File(target, "Contents").mkdirs(); File(target, "Contents/version").writeText("old")
        val src = File(dir, "dmgsrc/WorkTracker.app/Contents").apply { mkdirs() }
        File(src, "version").writeText("new")
        val dmg = File(dir, "new.dmg")
        val created = ProcessBuilder("hdiutil", "create", "-quiet", "-fs", "HFS+", "-srcfolder", File(dir, "dmgsrc").absolutePath, "-volname", "WorkTracker", dmg.absolutePath)
            .inheritIO().start().waitFor()
        assertEquals(0, created)
        val script = File(dir, "install-update.sh").apply { writeText(UpdateScripts.MAC) }
        // A process that has already ended stands in for the app that quit.
        val gone = ProcessBuilder("true").start().apply { waitFor() }.pid()

        val run = ProcessBuilder("/bin/bash", script.absolutePath, gone.toString(), dmg.absolutePath, target.absolutePath)
            .redirectErrorStream(true)
            .apply { environment()["OPEN_CMD"] = "true" }
            .start()
        val output = run.inputStream.bufferedReader().readText()
        assertEquals(0, run.waitFor(), output)
        assertEquals("new", File(target, "Contents/version").readText())
        assertTrue(apps.listFiles().orEmpty().map { it.name } == listOf("WorkTracker.app"), "no leftovers: ${apps.list()?.toList()}")
    }
}

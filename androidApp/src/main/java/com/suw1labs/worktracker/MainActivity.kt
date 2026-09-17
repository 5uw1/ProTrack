package com.suw1labs.worktracker

import android.content.pm.ApplicationInfo
import android.net.Uri
import com.suw1labs.worktracker.data.backup.DemoData
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var pendingExportContent: String? = null
    private var pendingOpenResult: ((String?) -> Unit)? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** "Save file" pickers: the user chooses the folder (Downloads, Drive, …) and we write the file there. */
    private val createCsvDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), ::writePendingExport)
    private val createJsonDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json"), ::writePendingExport)

    /** "Open file" picker for restoring a backup. */
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val onResult = pendingOpenResult
        pendingOpenResult = null
        if (onResult == null) return@registerForActivityResult
        if (uri == null) {
            onResult(null)
            return@registerForActivityResult
        }
        ioScope.launch {
            val text = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull()
            withContext(Dispatchers.Main) { onResult(text) }
        }
    }

    /** Folder picker for the automatic backup (Drive, device, …); the store keeps the permission. */
    private var pendingFolderResult: ((Uri?) -> Unit)? = null
    private val openFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        val onResult = pendingFolderResult
        pendingFolderResult = null
        onResult?.invoke(uri)
    }

    private fun writePendingExport(uri: Uri?) {
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) return
        ioScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, if (ok) "Saved" else "Could not save file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = WorkTrackerApplication.container(applicationContext)
        WorkTrackerApplication.fileExporter?.saveRequestHandler = { filename, mimeType, content ->
            pendingExportContent = content
            (if (mimeType == "application/json") createJsonDocument else createCsvDocument).launch(filename)
        }
        WorkTrackerApplication.fileExporter?.openRequestHandler = { mimeTypes, onResult ->
            pendingOpenResult?.invoke(null)
            pendingOpenResult = onResult
            openDocument.launch(mimeTypes)
        }

        WorkTrackerApplication.backupFolderStore?.pickRequestHandler = { onResult ->
            pendingFolderResult?.invoke(null)
            pendingFolderResult = onResult
            openFolder.launch(null)
        }

        // Screenshot / demo flags (debug builds only): adb shell am start ... --ez demo true --es tab reports --es lang de
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val launchOptions = if (debuggable) {
            AppLaunchOptions.of(intent.getBooleanExtra("demo", false), intent.getStringExtra("tab"), intent.getStringExtra("lang"), intent.getStringExtra("time"))
        } else {
            AppLaunchOptions.NONE
        }
        launchOptions.applyClock()
        if (launchOptions.demo) container.appScope.launch { DemoData.seed(container.backupManager, launchOptions.language) }

        setContent {
            App(container, launchOptions)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            WorkTrackerApplication.fileExporter?.saveRequestHandler = null
            WorkTrackerApplication.fileExporter?.openRequestHandler = null
            WorkTrackerApplication.backupFolderStore?.pickRequestHandler = null
        }
        super.onDestroy()
    }
}
